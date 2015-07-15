package com.thingworx.extensions.sftpExtension;

/**
 * Exception that occurs using sftp operations
 */
public class SftpException extends Exception {

    public SftpException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public SftpException(String message) {
        super(message);
    }
}
