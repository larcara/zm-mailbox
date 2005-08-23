/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 28.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RedoConfig {

    private static Log mLog = LogFactory.getLog(RedoConfig.class);

    private static RedoConfig theInstance;
    static {
        try {
            theInstance = new RedoConfig();
        } catch (ServiceException e) {
            Zimbra.halt("Unable to read redolog configuration", e);
        }
    }

    private RedoConfig() throws ServiceException {
        reloadInstance();
    }

    private void reloadInstance() throws ServiceException {
        Server config = Provisioning.getInstance().getLocalServer();

        mServiceHostname = config.getAttr(Provisioning.A_zimbraServiceHostname);
        mRedoLogEnabled = config.getBooleanAttr(Provisioning.A_zimbraRedoLogEnabled, D_REDOLOG_ENABLED);
        mRedoLogPath =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_zimbraRedoLogLogPath,
                                   D_REDOLOG_PATH)).getAbsolutePath();
        mRedoLogArchiveDir =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_zimbraRedoLogArchiveDir,
                                   D_REDOLOG_ARCHIVEDIR)).getAbsolutePath();
        mRedoLogRolloverFileSizeKB =
            config.getLongAttr(Provisioning.A_zimbraRedoLogRolloverFileSizeKB,
                               D_REDOLOG_ROLLOVER_FILESIZE_KB);
        mRedoLogFsyncIntervalMS =
            config.getLongAttr(Provisioning.A_zimbraRedoLogFsyncIntervalMS,
                               D_REDOLOG_FSYNC_INTERVAL_MS);
    }


    public static synchronized void reload() throws ServiceException {
        theInstance.reloadInstance();
    }


    private String mServiceHostname;
    public static synchronized String serviceHostname() {
        return theInstance.mServiceHostname;
    }

    private boolean mRedoLogEnabled;
    private static final boolean D_REDOLOG_ENABLED = true;
    /**
     * Indicates whether redo logging is enabled.
     * @return
     */
    public static synchronized boolean redoLogEnabled() {
        return theInstance.mRedoLogEnabled;
    }

    private String mRedoLogPath;
    private static final String D_REDOLOG_PATH = "redolog/redo.log";
    /**
     * The path to the redo.log file.  Relative path is resolved against
     * ZIMBRA_HOME.  Default value is "$ZIMBRA_HOME/redolog/redo.log".
     * @return absolute path to redo log file
     */
    public static synchronized String redoLogPath() {
        return theInstance.mRedoLogPath;
    }

    private String mRedoLogArchiveDir;
    private static final String D_REDOLOG_ARCHIVEDIR = "redolog/archive";
    /**
     * Directory in which redo logs are archived.  When the current redo.log
     * file reaches a certain threshold, it is rolled over and archived.
     * That is, the current redo.log file is renamed to a timestamped name,
     * moved into the archive directory, and a new empty redo.log file is
     * created.
     * @return absolute path to the archive directory
     * @see #redoLogRolloverFileSizeKB()
     */
    public static synchronized String redoLogArchiveDir() {
        return theInstance.mRedoLogArchiveDir;
    }

    private long mRedoLogRolloverFileSizeKB;
    private static final long D_REDOLOG_ROLLOVER_FILESIZE_KB = 10240;
    /**
     * Returns the redolog rollover threshold filesize.
     * The current redolog file is rolled over if it reaches or exceeds
     * this size.
     * @return threshold filesize in kilobytes
     */
    public static synchronized long redoLogRolloverFileSizeKB() {
        return theInstance.mRedoLogRolloverFileSizeKB;
    }

    private long mRedoLogFsyncIntervalMS;
    private static final long D_REDOLOG_FSYNC_INTERVAL_MS = 10;
    /**
     * Returns the fsync interval for flush/fsync thread.  Writes to the log
     * are written securely to disk by forcing an fsync.  But fsyncs are very
     * slow, so instead of each logging thread calling fsync individually,
     * they wait for a dedicated thread to fsync the accumulated changes
     * periodically.  This configuration value controls the interval between
     * the fsyncs.
     * 
     * With a longer interval, there will be fewer fsyncs compared to the
     * number of logging calls.  This can improve throughput under heavy
     * load but increases the latency on individual logging calls.
     * 
     * @return interval in milliseconds; default is 10ms
     */
    public static synchronized long redoLogFsyncIntervalMS() {
        return theInstance.mRedoLogFsyncIntervalMS;
    }
}
