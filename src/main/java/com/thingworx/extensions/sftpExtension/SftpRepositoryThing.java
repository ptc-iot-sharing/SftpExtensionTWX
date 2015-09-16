package com.thingworx.extensions.sftpExtension;


import ch.qos.logback.classic.Logger;
import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.extensions.sftpExtension.jsch.SftpFileRepositoryImpl;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.*;
import com.thingworx.things.Thing;
import com.thingworx.things.events.ThingworxEvent;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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
@ThingworxConfigurationTableDefinitions(
        tables = {@ThingworxConfigurationTableDefinition(
                name = "ConnectionInfo",
                description = "SFTP Server Connection Parameters",
                isMultiRow = false,
                ordinal = 0,
                dataShape = @ThingworxDataShapeDefinition(
                        fields = {@ThingworxFieldDefinition(
                                ordinal = 0,
                                name = "host",
                                description = "Server name",
                                baseType = "STRING",
                                aspects = {"defaultValue:localhost", "friendlyName:SFTP Server"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 1,
                                name = "port",
                                description = "Server port",
                                baseType = "INTEGER",
                                aspects = {"defaultValue:22", "friendlyName:SFTP Server Port"}
                        ),@ThingworxFieldDefinition(
                                ordinal = 2,
                                name = "username",
                                description = "Username",
                                baseType = "STRING",
                                aspects = {"defaultValue:root", "friendlyName:SFTP User"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 3,
                                name = "password",
                                description = "Password",
                                baseType = "PASSWORD",
                                aspects = {"friendlyName:SFTP Account Password"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 4,
                                name = "privateKey",
                                description = "Key based auth using a private key",
                                baseType = "PASSWORD",
                                aspects = {"friendlyName:Private key"}
                        ),   @ThingworxFieldDefinition(
                                ordinal = 3,
                                name = "passphrase",
                                description = "Passphrase",
                                baseType = "PASSWORD",
                                aspects = {"friendlyName:SFTP key passphrase"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 5,
                                name = "connectionTimeout",
                                description = "Timeout (milliseconds) to establish a connection",
                                baseType = "INTEGER",
                                aspects = {"defaultValue:20000", "friendlyName:Connection Timeout"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 6,
                                name = "keepAliveTimeout",
                                description = "Timeout (milliseconds) before closing a connection",
                                baseType = "INTEGER",
                                aspects = {"defaultValue:60000", "friendlyName:KeepAlive Timeout"}
                        )}
                )
        )}
)
public class SftpRepositoryThing extends Thing {
    private static Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpRepositoryThing.class);

    private ManagedSftpFileRepository repository;
    private SftpConfiguration config = new SftpConfiguration();

    @Override
    protected void initializeThing() throws Exception {
        // get values from the configuration table
        config.setHost((String) this.getConfigurationData().getValue("ConnectionInfo", "host"));
        config.setPort((Integer) this.getConfigurationData().getValue("ConnectionInfo", "port"));
        config.setPassphrase((String) this.getConfigurationData().getValue("ConnectionInfo", "passphrase"));
        config.setPassword((String) this.getConfigurationData().getValue("ConnectionInfo", "password"));
        config.setPrivateKey((String) this.getConfigurationData().getValue("ConnectionInfo", "privateKey"));
        config.setUsername((String) this.getConfigurationData().getValue("ConnectionInfo", "username"));
        config.setConnectionTimeout((Integer) this.getConfigurationData().getValue("ConnectionInfo", "connectionTimeout"));
        config.setKeepAliveTimeout((Integer) this.getConfigurationData().getValue("ConnectionInfo", "keepAliveTimeout"));
        repository = new ManagedSftpFileRepository(config);
        LOGGER.info("Created a sftp thing with config:" + config);
    }

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
        return convertToInfotable(repository.getRepository().listFiles(path, ""));
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
        return repository.getRepository().deleteFile(path);
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
        return convertToInfotable(repository.getRepository().getFileInfo(path));
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
        return convertToInfotable(repository.getRepository().listDirectories(path, nameMask));
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
        return convertToInfotable(repository.getRepository().listFiles(path, nameMask));
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
        repository.getRepository().moveFile(sourcePath, targetPath, overwrite);
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
        repository.getRepository().renameFile(path, name, overwrite);
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
        return repository.getRepository().createFolder(path);
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
    public void DownloadFile(@ThingworxServiceParameter(
            name = "FilePath",
            description = "Path to the file",
            baseType = "STRING"
    ) String filePath, @ThingworxServiceParameter(
            name = "FileRepository",
            description = "File repository",
            baseType = "THINGNAME") String fileRepository
    ) throws Exception {
        Thing thing = ThingUtilities.findThing(fileRepository);
        FileRepositoryThing fileRepoThing = (FileRepositoryThing) thing;
        ByteArrayOutputStream bos = repository.getRepository().downloadFile(filePath);
        fileRepoThing.CreateBinaryFile(new File(filePath).getAbsoluteFile().getName(), bos.toByteArray(), true);
    }

    @ThingworxServiceDefinition(
            name = "UploadFile",
            description = "Upload a file from a repository to the SFTP Server"
    )
    public void UploadFile(@ThingworxServiceParameter(
            name = "RepoFilePath",
            description = "Path of Repository File",
            baseType = "STRING"
    ) String repositoryPath, @ThingworxServiceParameter(
            name = "RemoteFilePath",
            description = "SFTP file path",
            baseType = "STRING"
    ) String remotePath, @ThingworxServiceParameter(
            name = "FileRepository",
            description = "File repository",
            baseType = "THINGNAME") String fileRepository
    ) throws Exception {
        Thing thing = ThingUtilities.findThing(fileRepository);
        FileRepositoryThing fileRepoThing = (FileRepositoryThing) thing;

        ByteArrayInputStream bis = new ByteArrayInputStream(fileRepoThing.LoadBinary(repositoryPath));
        repository.getRepository().uploadFile(bis, remotePath);
    }


    public static InfoTable convertToInfotable(Collection<FileSystemFile> files) throws Exception {
        InfoTable messagesInfoTable = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");

        for (FileSystemFile file : files) {
            ValueCollection vc = new ValueCollection();
            vc.put("lastModifiedDate", new DatetimePrimitive(file.getDateTime().getMillis()));
            vc.put("size", new NumberPrimitive(file.getSize()));
            vc.put("path", new StringPrimitive(file.getPath()));
            vc.put("name", new StringPrimitive(file.getName()));
            vc.put("fileType", new StringPrimitive(file.getFileType()));
            messagesInfoTable.addRow(vc);
        }
        return messagesInfoTable;
    }

    private InfoTable convertToInfotable(FileSystemFile file) throws Exception {
        List<FileSystemFile> files = new ArrayList<>();
        files.add(file);
        return convertToInfotable(files);
    }
}
