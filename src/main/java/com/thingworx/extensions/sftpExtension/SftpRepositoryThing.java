package com.thingworx.extensions.sftpExtension;


import com.thingworx.common.utils.PathUtilities;
import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.metadata.annotations.*;
import com.thingworx.things.Thing;
import com.thingworx.things.events.ThingworxEvent;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;


@ThingworxImplementedShapeDefinitions(
        shapes = {@ThingworxImplementedShapeDefinition(
                name = "FileSystemServices"
        )}
)
@ThingworxEventDefinitions(
        events = {@ThingworxEventDefinition(
                name = "FileTransfer",
                description = "File transfer notification",
                dataShape = "FileTransferJob"
        )}
)
public class SftpRepositoryThing extends Thing {
    @ThingworxServiceDefinition(
            name = "BrowseDirectory",
            description = "Get a list of files and/or directories on the SftpFilesystem",
            category = "Transfers"
    )
    @ThingworxServiceResult(
            name = "result",
            description = "Browse Results",
            baseType = "INFOTABLE",
            aspects = {"dataShape:FileSystemFile"}
    )
    public InfoTable BrowseDirectory(@ThingworxServiceParameter(
            name = "path",
            description = "Directory path",
            baseType = "STRING"
    ) String path) throws Exception {
        return null;
    }

    @ThingworxServiceDefinition(
            name = "DeleteFile",
            description = "Delete a file",
            category = "Files"
    )
    public synchronized Boolean DeleteFile(@ThingworxServiceParameter(
            name = "path",
            description = "File path",
            baseType = "STRING"
    ) String path) throws Exception {
       /* PathUtilities.validatePath(path);
        File file = new File(PathUtilities.concatPaths(this.getRootPath(), path));
        this.checkFileExists(file);
        if (file.isFile()) {
            boolean deleted = file.delete();
            if (!deleted) {
                _logger.warn("Could not delete Repository file [{}]", file.getAbsoluteFile());
            }

            return Boolean.valueOf(true);
        } else {
            throw new Exception("A valid file was not specified");
        }*/
        return false;
    }

    @ThingworxServiceDefinition(
            name = "GetFileInfo", description = "Get file info", category = "Files")
    @ThingworxServiceResult(
            name = "result", description = "File Results", baseType = "INFOTABLE",
            aspects = {"dataShape:FileSystemFile"})
    public InfoTable GetFileInfo(@ThingworxServiceParameter(
            name = "path",
            description = "File path",
            baseType = "STRING"
    ) String path) throws Exception {
       /* PathUtilities.validatePath(path);
        File file = new File(PathUtilities.concatPaths(this.getRootPath(), path));
        this.checkFileExists(file);
        InfoTable it = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");
        ValueCollection values = this.getFileInfoForFile(path, file, false);
        it.addRow(values);
        return it;
        */
        return null;
    }

    @ThingworxServiceDefinition(
            name = "ListDirectories",
            description = "Get list of directories",
            category = "Transfers"
    )
    @ThingworxServiceResult(
            name = "result",
            description = "Directory Results",
            baseType = "INFOTABLE",
            aspects = {"dataShape:FileSystemDirectory"}
    )
    public InfoTable ListDirectories(@ThingworxServiceParameter(
            name = "path",
            description = "Directory path",
            baseType = "STRING"
    ) String path, @ThingworxServiceParameter(
            name = "nameMask",
            description = "Name mask",
            baseType = "STRING"
    ) String nameMask) throws Exception {
       /* PathUtilities.validatePath(path);
        InfoTable it = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemDirectory");
        File startingDirectory = new File(PathUtilities.concatPaths(this.getRootPath(), path));
        validateDirectory(startingDirectory);
        this.getDirectoryListing(startingDirectory, path, it, 1);
        return it;*/
        return null;
    }

    @ThingworxServiceDefinition(
            name = "ListFiles",
            description = "Get file system listing",
            category = "Transfers"
    )
    @ThingworxServiceResult(
            name = "result",
            description = "File Results",
            baseType = "INFOTABLE",
            aspects = {"dataShape:FileSystemFile"}
    )
    public InfoTable ListFiles(@ThingworxServiceParameter(
            name = "path",
            description = "Directory path",
            baseType = "STRING"
    ) String path, @ThingworxServiceParameter(
            name = "nameMask",
            description = "Name mask",
            baseType = "STRING"
    ) String nameMask) throws Exception {
        // return this.GetFileListing(path, nameMask);
        return null;
    }

    @ThingworxServiceDefinition(
            name = "MoveFile",
            description = "Move a file",
            category = "Files"
    )
    public synchronized void MoveFile(@ThingworxServiceParameter(
            name = "sourcePath",
            description = "Path of file to move",
            baseType = "STRING"
    ) String sourcePath, @ThingworxServiceParameter(
            name = "targetPath",
            description = "Path of target file",
            baseType = "STRING"
    ) String targetPath, @ThingworxServiceParameter(
            name = "overwrite",
            description = "Overwrite existing file",
            baseType = "BOOLEAN",
            aspects = {"defaultValue:false"}
    ) Boolean overwrite) throws Exception {
        // this.moveFile(sourcePath, targetPath);
    }

    @ThingworxServiceDefinition(
            name = "RenameFile",
            description = "Rename a file",
            category = "Files"
    )
    public synchronized void RenameFile(@ThingworxServiceParameter(
            name = "path",
            description = "File path",
            baseType = "STRING"
    ) String path, @ThingworxServiceParameter(
            name = "name",
            description = "New file name",
            baseType = "STRING"
    ) String name, @ThingworxServiceParameter(
            name = "overwrite",
            description = "Overwrite existing file",
            baseType = "BOOLEAN",
            aspects = {"defaultValue:false"}
    ) Boolean overwrite) throws Exception {
        PathUtilities.validatePath(path);
        PathUtilities.validatePath(name);
    }

    @ThingworxServiceDefinition(
            name = "CreateFolder",
            description = "Create a folder",
            category = "Directories"
    )
    public Boolean CreateFolder(@ThingworxServiceParameter(
            name = "path",
            description = "Folder path",
            baseType = "STRING"
    ) String path) throws Exception {
        return false;
    }

    private void fireTransferEvent(String userName) throws Exception {
        ThingworxEvent event = new ThingworxEvent();
        event.setTraceActive(ThreadLocalContext.isTraceActive());
        event.setSecurityContext(ThreadLocalContext.getSecurityContext());
        event.setSource(getName());
        event.setEventName("FileTransfer");

        // the name parameter isn't really used
        InfoTable data = InfoTableInstanceFactory.createInfoTableFromDataShape("name", "FileTransferJob");

        ValueCollection values = new ValueCollection();
        values.put("userGreeted", new StringPrimitive(userName));

        data.addRow(values);

        event.setEventData(data);

        this.dispatchBackgroundEvent(event);
    }

    @ThingworxServiceDefinition(
            name = "DownloadFile",
            description = "Download a FTP server file to a repository"
    )
    public void downloadFile(@ThingworxServiceParameter(
            name = "FilePath",
            description = "Path to the file",
            baseType = "STRING"
    ) String filePath, @ThingworxServiceParameter(
            name = "FileRepository",
            description = "File repository",
            baseType = "THINGNAME") String fileRepository
    ) throws Exception {

    }

    @ThingworxServiceDefinition(
            name = "UploadFile",
            description = "Upload a file from a repository to the SFTP Server"
    )
    public void uploadFile(@ThingworxServiceParameter(
            name = "RepoFilePath",
            description = "Path of Repository File",
            baseType = "STRING"
    ) String repositoryPath, @ThingworxServiceParameter(
            name = "RemoteFilePath",
            description = "SFTOP file path",
            baseType = "STRING"
    ) String FTPPath, @ThingworxServiceParameter(
            name = "FileRepository",
            description = "File repository",
            baseType = "THINGNAME") String fileRepository
    ) throws Exception {
    }
}
