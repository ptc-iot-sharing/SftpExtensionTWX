package com.thingworx.extensions.sftpExtension.jsch;

import com.jcraft.jsch.*;
import com.thingworx.common.utils.PathUtilities;
import com.thingworx.common.utils.StringUtilities;
import com.thingworx.extensions.sftpExtension.FileSystemFile;
import com.thingworx.extensions.sftpExtension.SftpConfiguration;
import com.thingworx.extensions.sftpExtension.SftpException;
import com.thingworx.extensions.sftpExtension.SftpRepository;
import com.thingworx.logging.LogUtilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * A implementation of the SftpRepository based on the Jsch library
 */
public class SftpFileRepositoryImpl implements SftpRepository {
    private static org.slf4j.Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpFileRepositoryImpl.class);

    private final Session session;
    private final ChannelSftp channel;

    public SftpFileRepositoryImpl(SftpConfiguration config) throws SftpException {
        JSch jSch = new JSch();
        try {
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
            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException e) {
            throw new SftpException("Failed to create session", e);
        }
    }

    /**
     * Creates a new folder on the remote filesystem on the specified path
     *
     * @param path path where to create the new folder
     */
    @Override
    public void createFolder(String path) throws SftpException {
        try {
            channel.mkdir(path);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.warn("Failed to create a new directory in path " + path, e);
            throw new SftpException("Directory creation failed", e);
        }
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
        if (!overwrite) {
            // check if the file exists
            try {
                SftpATTRS attrs = channel.stat(newFilePath);
                if (attrs != null) {
                    throw new SftpException("File " + newFilePath + " already exists!");
                }
            } catch (com.jcraft.jsch.SftpException e) {
                LOGGER.warn(String.format("Failed to see if file %s exists", newFilePath), e);
                throw new SftpException(String.format("Failed to see if file %s exists", newFilePath), e);
            }
        }
        try {
            channel.rename(filePath, newFilePath);
        } catch (com.jcraft.jsch.SftpException e) {
            LOGGER.warn(String.format("Failed to rename file %s to %s", filePath, newName), e);
            throw new SftpException(String.format("Failed to rename file %s to %s", filePath, newName), e);
        }
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
        return null;
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
        return null;
    }

    /**
     * Gets the file information for a given folder
     *
     * @param filePath The full file path
     * @return File information about that file
     */
    @Override
    public FileSystemFile getFileInfo(String filePath) throws SftpException {
        return null;
    }

    /**
     * Deletes a specific file from the remote filesystem
     *
     * @param filePath full file path
     * @return True if the deletion was successful
     */
    @Override
    public boolean deleteFile(String filePath) throws SftpException {
        return false;
    }

    /**
     * Download a file from the remote filesystem
     *
     * @param filePath file to download specified by the full path
     * @return The file as an outputStream
     */
    @Override
    public ByteArrayOutputStream downloadFile(String filePath) throws SftpException {
        return null;
    }

    /**
     * Upload a file to the remote filesystem
     *
     * @param inputStream the file to upload represented as input file stream
     * @param filePath    path where to upload the file
     */
    @Override
    public void uploadFile(ByteArrayInputStream inputStream, String filePath) throws SftpException {

    }

    /**
     * Closes the underlying channel and session
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }
}
