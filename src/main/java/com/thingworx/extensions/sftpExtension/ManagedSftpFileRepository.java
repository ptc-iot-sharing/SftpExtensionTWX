package com.thingworx.extensions.sftpExtension;

import ch.qos.logback.classic.Logger;
import com.thingworx.extensions.sftpExtension.jsch.SftpFileRepositoryImpl;
import com.thingworx.logging.LogUtilities;
import org.joda.time.DateTime;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A sftp repository that connects on demand, and disconnects on timeout
 * This regular checks when was the last message and, if a certain time has passed,
 * closes the connection.
 */
public class ManagedSftpFileRepository extends TimerTask {
    private static Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpRepositoryThing.class);

    private SftpRepository repository;
    private SftpConfiguration config;
    private DateTime lastCommand;

    public ManagedSftpFileRepository(SftpConfiguration config) throws SftpException {
        this.config = config;
        repository = new SftpFileRepositoryImpl(config);
        new Timer("SftpKeepAliveThread", true).scheduleAtFixedRate(this, config.getKeepAliveTimeout() / 2,
                config.getKeepAliveTimeout() / 2);
        lastCommand = new DateTime();
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        long lastCommandDelta = new DateTime().getMillis() - lastCommand.getMillis();
        if (lastCommandDelta > config.getKeepAliveTimeout() && !repository.isDisconnected()) {
            try {
                repository.close();
                LOGGER.info(String.format("Closed sftpRepository %s@%s no messages in last %d ms",
                        config.getUsername(), config.getHost(), lastCommandDelta));
            } catch (Exception e) {
                LOGGER.warn("Failed to close sftp repository");
            }
        }
    }

    public SftpRepository getRepository() throws SftpException {
        if (repository.isDisconnected()) {
            repository = new SftpFileRepositoryImpl(config);
        }
        lastCommand = new DateTime();
        return repository;
    }
}
