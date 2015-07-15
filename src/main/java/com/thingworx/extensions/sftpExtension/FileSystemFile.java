package com.thingworx.extensions.sftpExtension;

import org.joda.time.DateTime;

/**
 * An POJO representation of the FileSystemFile Thingworx datashape
 */
public class FileSystemFile {
    private String path;
    private Number size;
    private DateTime dateTime;
    private String name;
    private String fileType;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Number getSize() {
        return size;
    }

    public void setSize(Number size) {
        this.size = size;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
