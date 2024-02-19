package com.thingworx.extensions.sftpExtension;


import ch.qos.logback.classic.Logger;
import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.*;
import com.thingworx.system.ContextType;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@ThingworxImplementedShapeDefinitions(
        shapes = {@ThingworxImplementedShapeDefinition(
                name = "FileSystemServices"
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
                                ordinal = 6,
                                name = "connectionTimeout",
                                description = "Timeout (milliseconds) to establish a connection",
                                baseType = "INTEGER",
                                aspects = {"defaultValue:20000", "friendlyName:Connection Timeout"}
                        ), @ThingworxFieldDefinition(
                                ordinal = 7,
                                name = "keepAliveTimeout",
                                description = "Timeout (milliseconds) before closing a connection",
                                baseType = "INTEGER",
                                aspects = {"defaultValue:60000", "friendlyName:KeepAlive Timeout"}
                        )}
                )
        ),
                @ThingworxConfigurationTableDefinition(
                        name = "Keybasedauth",
                        description = "SFTP key based authentication",
                        isMultiRow = false,
                        ordinal = 1,
                        dataShape = @ThingworxDataShapeDefinition(
                                fields = {@ThingworxFieldDefinition(
                                        ordinal = 0,
                                        name = "repository",
                                        description = "The repository where the private key is.",
                                        baseType = "THINGNAME",
                                        aspects = {"friendlyName:Repository"}
                                ), @ThingworxFieldDefinition(
                                        ordinal = 1,
                                        name = "privateKey",
                                        description = "File on a repository with the private key.",
                                        baseType = "TEXT",
                                        aspects = {"friendlyName:Private key"}
                                ), @ThingworxFieldDefinition(
                                        ordinal = 2,
                                        name = "passphrase",
                                        description = "Passphrase",
                                        baseType = "PASSWORD",
                                        aspects = {"friendlyName: Private key passphrase"}
                                )}
                        )
        )}
)
public class SftpRepositoryThing extends Thing {
    private static final Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpRepositoryThing.class);
    private static final String CONNECTION_SETTINGS_TABLE = "ConnectionInfo";
    private static final String KEY_BASED_SETTINGS_TABLE = "Keybasedauth";
    private ManagedSftpFileRepository repository;
    private final SftpConfiguration config = new SftpConfiguration();

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

    @Override
    protected void initializeThing(ContextType contextType) throws Exception {
        super.initializeThing(contextType);
        // get values from the configuration table
        config.setHost(this.getStringConfigurationSetting(CONNECTION_SETTINGS_TABLE, "host"));
        config.setPort((Integer) this.getConfigurationSetting(CONNECTION_SETTINGS_TABLE, "port"));
        config.setPassphrase(this.getStringConfigurationSetting(KEY_BASED_SETTINGS_TABLE, "passphrase"));
        config.setPassword(this.getStringConfigurationSetting(CONNECTION_SETTINGS_TABLE, "password"));
        String privateKeyFile = this.getStringConfigurationSetting(KEY_BASED_SETTINGS_TABLE, "privateKey");
        String fileRepo = this.getStringConfigurationSetting(KEY_BASED_SETTINGS_TABLE, "repository");
        if (fileRepo!=null && !fileRepo.isEmpty()) {
            try {
                Thing repoThing = ThingUtilities.findThing(fileRepo);
                config.setPrivateKey(((FileRepositoryThing) repoThing).LoadText(privateKeyFile));
            } catch (Exception ex) {
                LOGGER.warn("Cannot load the private key file", ex);
            }
        }
        config.setUsername(this.getStringConfigurationSetting(CONNECTION_SETTINGS_TABLE, "username"));
        config.setConnectionTimeout((Integer) this.getConfigurationSetting(CONNECTION_SETTINGS_TABLE, "connectionTimeout"));
        config.setKeepAliveTimeout((Integer) this.getConfigurationSetting(CONNECTION_SETTINGS_TABLE, "keepAliveTimeout"));
        repository = new ManagedSftpFileRepository(config);
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
        return convertToInfotable(repository.getRepository().listFilesAndDirectories(path));
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
    ) String path) throws Exception {
        return convertToInfotable(repository.getRepository().listDirectories(path));
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
            description = "Directory path. Can include wildcards",
            baseType = "STRING"
    ) String path) throws Exception {
        return convertToInfotable(repository.getRepository().listFiles(path));
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

    private InfoTable convertToInfotable(FileSystemFile file) throws Exception {
        List<FileSystemFile> files = new ArrayList<>();
        files.add(file);
        return convertToInfotable(files);
    }
}
