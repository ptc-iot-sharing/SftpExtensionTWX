package com.thingworx.extensions.sftpExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * An generic interface for interacting with sftp remote filesystems. This provides definitions for the
 * services in the SftpRepositoryThing.
 * Implementations can use any backend available
 */
public interface SftpRepository extends AutoCloseable {
    /**
     * Creates a new folder on the remote filesystem on the specified path
     *
     * @param path path where to create the new folder
     */
    boolean createFolder(String path) throws SftpException;

    /**
     * Renames a file with a given filePath to a new name
     *
     * @param filePath  file to rename
     * @param newName   the new file name
     * @param overwrite Overwrite the existing file if it already exists
     */
    void renameFile(String filePath, String newName, boolean overwrite) throws SftpException;

    /**
     * Moves a file with a given filePath to another location
     *
     * @param sourcePath file to move
     * @param targetPath where to move the file
     * @param overwrite  Overwrite the existing file if it already exists
     */
    void moveFile(String sourcePath, String targetPath, boolean overwrite) throws SftpException;

    /**
     * Lists only the files in a given directory
     *
     * @param directoryPath directory path to list
     * @param nameMask      Represents a name mask that the files must match. This uses bash style wildcards
     * @return A list of all the files in the directory
     */
    List<FileSystemFile> listFiles(String directoryPath, String nameMask) throws SftpException;

    /**
     * Lists only the directories in a given directory
     *
     * @param directoryPath directory path to list
     * @param nameMask      Represents a name mask that the directories must match. This uses bash style wildcards
     * @return A list of all the directories in the directory
     */
    List<FileSystemFile> listDirectories(String directoryPath, String nameMask) throws SftpException;

    /**
     * Gets the file information for a given folder
     *
     * @param filePath The full file path
     * @return File information about that file
     */
    FileSystemFile getFileInfo(String filePath) throws SftpException;

    /**
     * Deletes a specific file from the remote filesystem
     *
     * @param filePath full file path
     * @return True if the deletion was successful
     */
    boolean deleteFile(String filePath) throws SftpException;

    /**
     * Download a file from the remote filesystem
     *
     * @param filePath file to download specified by the full path
     * @return The file as an outputStream
     */
    ByteArrayOutputStream downloadFile(String filePath) throws SftpException;

    /**
     * Upload a file to the remote filesystem
     *
     * @param inputStream the file to upload represented as input file stream
     * @param filePath    path where to upload the file
     */
    void uploadFile(ByteArrayInputStream inputStream, String filePath) throws SftpException;

    /**
     * Moves to another directory
     *
     * @param directory directory to move to
     * @throws SftpException
     */
    void changeDirectory(String directory) throws SftpException;

    boolean isDisconnected();
}