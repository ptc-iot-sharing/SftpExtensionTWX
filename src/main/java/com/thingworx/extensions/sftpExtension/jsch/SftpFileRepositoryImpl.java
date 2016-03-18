package com.thingworx.extensions.sftpExtension.jsch;

import ch.qos.logback.classic.Logger;
import com.jcraft.jsch.*;
import com.thingworx.common.utils.StringUtilities;
import com.thingworx.extensions.sftpExtension.*;
import com.thingworx.extensions.sftpExtension.SftpException;
import com.thingworx.logging.LogUtilities;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A implementation of the SftpRepository based on the Jsch library
 */
public class SftpFileRepositoryImpl implements SftpRepository {
    private static Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpRepositoryThing.class);

    private final Session session;
    private final ChannelSftp channel;
    private boolean isDisconnected;

    public SftpFileRepositoryImpl(SftpConfiguration config) throws SftpException {
        try {
            JSch jSch = new JSch();
            // attempt to load the private key
            if (StringUtilities.isNonEmpty(config.getPrivateKey())) {
                // get the passphrase as a byte array
                byte[] passphraseBytes = config.getPassphrase() != null ? config.getPassphrase().getBytes() : null;
                // the private key as a byte array
                byte[] privateKeyBytes = config.getPrivateKey().getBytes();
                // add the identity
                jSch.addIdentity(config.getUsername(), privateKeyBytes, null, passphraseBytes);
            }
            // create a session
            session = jSch.getSession(config.getUsername(), config.getHost(), config.getPort());
            // accept all hosts, don't require a known_hosts list
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(config.getPassword());

            session.connect(config.getConnectionTimeout());
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(config.getConnectionTimeout());

        } catch (JSchException e) {
            LOGGER.error("Failed to create session " + e.getMessage(), e);
            throw new SftpException("Failed to create session" + e.getMessage(), e);
        }
    }

    /**
     * Creates a new folder on the remote filesystem on the specified path
     *
     * @param path path where to create the new folder
     */
    @Override
    public boolean createFolder(String path) throws SftpException {
        try {
            channel.mkdir(path);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error("Failed to create a new directory in path " + path, e);
            throw new SftpException("Directory creation failed " + e.getMessage(), e);
        }
        return true;
    }

    /**
     * Renames a file with a given filePath to a new name
     *
     * @param filePath  file to rename
     * @param newName   the new file name
     * @param overwrite Overwrite the existing file if it already exists
     */
    @Override
    public void renameFile(String filePath, String newName, boolean overwrite) throws SftpException {
        // build the new file path
        String newFilePath = Paths.get(filePath).getParent().resolve(newName).toString().replace("\\", "/");
        moveFile(filePath, newFilePath, overwrite);
    }

    /**
     * Moves a file with a given filePath to another location
     *
     * @param sourcePath file to move
     * @param targetPath where to move the file
     * @param overwrite  Overwrite the existing file if it already exists
     */
    @Override
    public void moveFile(String sourcePath, String targetPath, boolean overwrite) throws SftpException {
        if (!overwrite) {
            // check if the file exists
            try {
                SftpATTRS attrs = channel.stat(targetPath);
                if (attrs != null) {
                    throw new SftpException("File " + targetPath + " already exists!");
                }
            } catch (com.jcraft.jsch.SftpException e) {
                if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    LOGGER.error(String.format("Failed to see if file %s exists %s", targetPath, e.getMessage()), e);
                    throw new SftpException(String.format("Failed to see if file %s exists", targetPath), e);
                }
            }
        }
        try {
            channel.rename(sourcePath, targetPath);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to move file %s to %s", sourcePath, targetPath), e);
            throw new SftpException(String.format("Failed to move file %s to %s, exception: %s",
                    sourcePath, targetPath, e.getMessage()), e);
        }
    }

    /**
     * Lists the files and dirs in a given directory
     *
     * @param directoryPath directory path to list
     * @return A list of all the files in the directory
     */
    @Override
    public List<FileSystemFile> listFilesAndDirectories(String directoryPath) throws SftpException {
        return listFiles(directoryPath, false, false);
    }

    /**
     * Lists only the files in a given directory
     *
     * @param directoryPath directory path to list
     * @return A list of all the files in the directory
     */
    @Override
    public List<FileSystemFile> listFiles(String directoryPath) throws SftpException {
        return listFiles(directoryPath, true, false);

    }

    private List<FileSystemFile> listFiles(String directoryPath, boolean filterDirectories, boolean filterFiles)
            throws SftpException {
        try {
            Vector<ChannelSftp.LsEntry> vv = channel.ls(directoryPath);

            if (vv != null) {
                List<FileSystemFile> files = new ArrayList<>(vv.size());
                for (int ii = 0; ii < vv.size(); ii++) {
                    ChannelSftp.LsEntry currentElement = vv.elementAt(ii);
                    boolean isDirectory = currentElement.getAttrs().isDir();
                    // if it's a file and we don't have filter flag for files set, then add it
                    // if it's a folder and we don't have the filter flag for folders set, then add it
                    if ((!isDirectory && !filterFiles) || (!filterDirectories && isDirectory)) {
                        FileSystemFile file = new FileSystemFile();
                        file.setName(currentElement.getFilename());
                        file.setIsDirectory(isDirectory);
                        file.setSize(currentElement.getAttrs().getSize());
                        file.setDateTime(new DateTime(currentElement.getAttrs().getMTime() * 1000L));
                        if (!directoryPath.equals("/")) {
                            file.setPath(directoryPath + "/" + currentElement.getFilename());
                        } else {
                            file.setPath("/" + currentElement.getFilename());
                        }
                        files.add(file);
                    }
                }
                LOGGER.info("found " + files.size() + " files");
                return files;
            } else {
                return new ArrayList<>();
            }
        } catch (com.jcraft.jsch.SftpException | NullPointerException e) {
            throw new SftpException(String.format("Failed to list files in %s, exception: %s",
                    directoryPath, e.getMessage()), e);
        }
    }

    /**
     * Lists only the directories in a given directory
     *
     * @param directoryPath directory path to list
     * @return A list of all the directories in the directory
     */
    @Override
    public List<FileSystemFile> listDirectories(String directoryPath) throws SftpException {
        return listFiles(directoryPath, false, true);
    }

    /**
     * Gets the file information for a given folder
     *
     * @param filePath The full file path
     * @return File information about that file
     */
    @Override
    public FileSystemFile getFileInfo(String filePath) throws SftpException {
        try {
            SftpATTRS attrs = channel.stat(filePath);
            FileSystemFile file = new FileSystemFile();
            file.setName(Paths.get(filePath).getFileName().toString());
            file.setIsDirectory(attrs.isDir());
            file.setSize(attrs.getSize());
            file.setDateTime(new DateTime(attrs.getMTime()));
            file.setPath(filePath);
            return file;
        } catch (com.jcraft.jsch.SftpException | NullPointerException e) {
            LOGGER.warn(String.format("Failed to get file info for %s, exception: %s",
                    filePath, e.getMessage()), e);
            throw new SftpException(String.format("Failed to get file info for %s, exception: %s",
                    filePath, e.getMessage()), e);
        }
    }

    /**`
     * Deletes a specific file from the remote filesystem
     *
     * @param filePath full file path
     * @return True if the deletion was successful
     */
    @Override
    public boolean deleteFile(String filePath) throws SftpException {
        FileSystemFile file = getFileInfo(filePath);
        try {
            if (file.isDirectory()) {
                removeFolder(filePath);
            } else {
                channel.rm(filePath);
            }
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to delete file %s exception: %s",
                    filePath, e.getMessage()), e);
            throw new SftpException(String.format("Failed to delete file %s exception: %s",
                    filePath, e.getMessage()), e);
        }
        return true;
    }

    /**
     * Download a file from the remote filesystem
     *
     * @param filePath file to download specified by the full path
     * @return The file as an outputStream
     */
    @Override
    public ByteArrayOutputStream downloadFile(String filePath) throws SftpException {
        // first check if the file exists
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            FileSystemFile file = getFileInfo(filePath);
            if (file.isDirectory()) {
                throw new SftpException("Cannot download an entire folder");
            }
            channel.get(filePath, out);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to download file %s exception: %s",
                    filePath, e.getMessage()), e);
            throw new SftpException(String.format("Failed to download file %s exception: %s",
                    filePath, e.getMessage()), e);
        }
        return out;
    }

    /**
     * Upload a file to the remote filesystem
     *
     * @param inputStream the file to upload represented as input file stream
     * @param filePath    path where to upload the file
     */
    @Override
    public void uploadFile(ByteArrayInputStream inputStream, String filePath) throws SftpException {
        try {
            channel.put(inputStream, filePath);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to upload file %s exception: %s", filePath, e.getMessage()), e);
            throw new SftpException(String.format("Failed to upload file %s exception: %s", filePath, e.getMessage()), e);
        }
    }

    @Override
    public void changeDirectory(String directory) throws SftpException {
        try {
            channel.cd(directory);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to change directory to %s exception: %s", directory, e.getMessage()), e);
            throw new SftpException(String.format("Failed to change directory to %s exception: %s",
                    directory, e.getMessage()), e);
        }
    }

    /**
     * Closes the underlying channel and session
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        this.isDisconnected = true;
        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    private void removeFolder(String directoryPath) throws com.jcraft.jsch.SftpException {
        channel.cd(directoryPath);
        // List source directory structure.
        Vector<ChannelSftp.LsEntry> list = channel.ls(directoryPath);
        // Iterate objects in the list to get file/folder names.
        for (ChannelSftp.LsEntry oListItem : list) {
            // If it is a file (not a directory).
            if (!oListItem.getAttrs().isDir()) {
                // Remove file.
                channel.rm(directoryPath + "/" + oListItem.getFilename());

            } else if (!(".".equals(oListItem.getFilename()) ||
                    "..".equals(oListItem.getFilename()))) { // If it is a subdir.
                try {
                    // Try removing subdir.
                    channel.rmdir(directoryPath + "/" + oListItem.getFilename());
                } catch (Exception e) {
                    // If subdir is not empty and error occurs.
                    // Do lsFolderRemove on this subdir to enter it and clear its contents.
                    removeFolder(directoryPath + "/" + oListItem.getFilename());
                }
            }
        }
        channel.rmdir(directoryPath); // Finally remove the required dir.
    }
}
