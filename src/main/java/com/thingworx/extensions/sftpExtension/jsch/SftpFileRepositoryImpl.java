package com.thingworx.extensions.sftpExtension.jsch;

import ch.qos.logback.classic.Logger;
import com.jcraft.jsch.*;
import com.thingworx.common.utils.PathUtilities;
import com.thingworx.common.utils.StringUtilities;
import com.thingworx.extensions.sftpExtension.*;
import com.thingworx.extensions.sftpExtension.SftpException;
import com.thingworx.logging.LogUtilities;
import org.joda.time.DateTime;

import java.io.*;
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
            throw new com.thingworx.extensions.sftpExtension.SftpException("Failed to create session", e);
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
            throw new SftpException("Directory creation failed", e);
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
        File file = new File(filePath);
        String parentPath = file.getAbsoluteFile().getParent();
        String newFilePath = PathUtilities.concatPaths(parentPath, newName);
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
                if(e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    LOGGER.error(String.format("Failed to see if file %s exists", targetPath), e);
                    throw new SftpException(String.format("Failed to see if file %s exists", targetPath), e);
                }
            }
        }
        try {
            channel.rename(sourcePath, targetPath);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error(String.format("Failed to move file %s to %s", sourcePath, targetPath), e);
            throw new SftpException(String.format("Failed to move file %s to %s", sourcePath, targetPath), e);
        }
    }

    /**
     * Lists only the files in a given directory
     *
     * @param directoryPath directory path to list
     * @param nameMask      Represents a name mask that the files must match. This uses bash style wildcards
     * @return A list of all the files in the directory
     */
    @Override
    public List<FileSystemFile> listFiles(String directoryPath, String nameMask) throws SftpException {
        return listFiles(directoryPath, false);
    }

    private List<FileSystemFile> listFiles(String directoryPath, Boolean onlyDirectories) throws SftpException {
        try {
            Vector<ChannelSftp.LsEntry> vv = channel.ls(directoryPath);

            if (vv != null) {
                List<FileSystemFile> files = new ArrayList<>(vv.size());
                for (int ii = 0; ii < vv.size(); ii++) {

                    ChannelSftp.LsEntry currentElement = vv.elementAt(ii);
                    if (!onlyDirectories || currentElement.getAttrs().isDir()) {
                        FileSystemFile file = new FileSystemFile();
                        file.setName(currentElement.getFilename());
                        file.setIsDirectory(currentElement.getAttrs().isDir());
                        file.setSize(currentElement.getAttrs().getSize());
                        file.setDateTime(new DateTime(currentElement.getAttrs().getMTime()));
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
            throw new SftpException(String.format("Failed to list files in %s", directoryPath), e);
        }
    }

    /**
     * Lists only the directories in a given directory
     *
     * @param directoryPath directory path to list
     * @param nameMask      Represents a name mask that the directories must match. This uses bash style wildcards
     * @return A list of all the directories in the directory
     */
    @Override
    public List<FileSystemFile> listDirectories(String directoryPath, String nameMask) throws SftpException {
        return listFiles(directoryPath, true);
    }

    /**
     * Gets the file information for a given folder
     *
     * @param filePath The full file path
     * @return File information about that file
     */
    @Override
    public FileSystemFile getFileInfo(String filePath) throws SftpException {
        List<FileSystemFile> files = listFiles(filePath, false);
        if (files.size() > 0) {
            return files.get(0);
        } else {
            throw new SftpException("The file " + filePath + " was not found");
        }
    }

    /**
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
                channel.rmdir(file.getPath());
            } else {
                channel.rm(file.getPath());
            }
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error("Failed to delete file " + filePath, e);
            throw new SftpException("Failed to delete file " + filePath, e);
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
        FileSystemFile file = getFileInfo(filePath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            channel.get(file.getPath(), out);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error("Failed to download file " + filePath, e);
            throw new SftpException("Failed to download file " + filePath, e);
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
            LOGGER.error("Failed to upload file " + filePath, e);
            throw new SftpException("Failed to upload file " + filePath, e);
        }
    }

    @Override
    public void changeDirectory(String directory) throws SftpException {
        try {
            channel.cd(directory);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.error("Failed to change directory to " + directory, e);
            throw new SftpException("Failed to change directory to " + directory, e);
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
}
