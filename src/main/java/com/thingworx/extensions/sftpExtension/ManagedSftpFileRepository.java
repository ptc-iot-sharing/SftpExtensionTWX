package com.thingworx.extensions.sftpExtension;

import ch.qos.logback.classic.Logger;
import com.thingworx.extensions.sftpExtension.jsch.SftpFileRepositoryImpl;
import com.thingworx.logging.LogUtilities;
import org.joda.time.DateTime;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A sftp repository that connects on demand, and disconnects on timeout
 */
public class ManagedSftpFileRepository extends TimerTask{
    private static Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(SftpRepositoryThing.class);

    private SftpRepository repository;
    private SftpConfiguration config;
    private DateTime lastCommand;

    public ManagedSftpFileRepository(SftpConfiguration config) throws SftpException {
        this.config = config;
        repository = new SftpFileRepositoryImpl(config);
        new Timer("SftpKeepAliveThread", true).scheduleAtFixedRate(this, config.getKeepAliveTimeout(),
                config.getKeepAliveTimeout());
        lastCommand = new DateTime();
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        long lastCommandDelta = new DateTime().getMillis() - lastCommand.getMillis();
        if(lastCommandDelta > config.getKeepAliveTimeout()) {
            try {
                repository.close();
                LOGGER.info("Closed sftpRepository " + config.getUsername() + "@" + config.getHost());
            } catch (Exception e) {
                LOGGER.warn("Failed to close sftp repository");
            }
        }
    }

    public SftpRepository getRepository() throws SftpException {
        if(repository.isDisconnected()) {
            repository = new SftpFileRepositoryImpl(config);
        }
        lastCommand = new DateTime();
        return repository;
    }
}
