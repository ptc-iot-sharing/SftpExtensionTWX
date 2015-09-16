package com.thingworx.extensions.sftpExtension;

import org.joda.time.DateTime;

/**
 * An POJO representation of the FileSystemFile Thingworx datashape
 */
public class FileSystemFile {
    private String path;
    private double size;
    private DateTime dateTime;
    private String name;
    private boolean isDirectory;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
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

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public String getFileType() {
        return isDirectory() ? "D" : "F";
    }

}
