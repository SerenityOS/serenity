/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @summary sanity test for log sudden-death and recovery
 * @run ignore Requires special hooks in ReliableLog not yet putback.
 */

/* The ReliableLog uses three filenames and renaming to effect atomic
 * versionFile updates.  First, a newVersionFile (an intention list)
 * is written.  Next, the current versionFile is renamed to an
 * oldVersionFile (an undo list).  Finally, the newVersionFile is
 * renamed to the current versionFile, and the undo list is discarded.
 * If the current version file does not exist on restart, then
 * stability can always be restored by reading the oldVersionFile.
 *
 * This test uses little conditional bombs.  When a switch is flipped
 * in ReliableLog, the code will abort with an InternalError at a
 * particular point.  We then pretend to have come up from scratch and
 * recover from the bombed situation.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import sun.rmi.log.LogHandler;
import sun.rmi.log.ReliableLog;

//import javasoft.sqe.harness.Status;
//import javasoft.sqe.harness.Test;

public class Recovery
    extends LogHandler
    implements Serializable //, Test
{
    static private final String dir = "./recovery_tmp";

    static public void main (String[] argv)
    {
        Recovery test = new Recovery();
        //Status status = test.run (argv, System.err, System.out);
        //status.exit();
        test.run (argv, System.err, System.out);
    }

    //public Status run (String argv[], PrintStream log, PrintStream out)
    public void run (String argv[], PrintStream lg, PrintStream out)
    {
        try {
            int size;
            int deathpoint;
            for (size = 1; size < 256; size *= 2) {
                for (deathpoint = 0; deathpoint <= 8; deathpoint++) {
                    // Trash the log directory
                    try {
                        (new File(dir,"Version_Number")).delete();
                    } catch (Exception e) {
                    }
                    try {
                        (new File(dir,"New_Version_Number")).delete();
                    } catch (Exception e) {
                    }
                    try {
                        (new File(dir,"Old_Version_Number")).delete();
                    } catch (Exception e) {
                    }

                    Recovery handler = new Recovery();
                    ReliableLog log;
                    log = new ReliableLog(dir, handler);

                    // Generate a number of updates (size - 1) until failing
                    int i;
                    StringBuffer writ = new StringBuffer();
                    char[] u = new char[1];
                    for (i = 1; i < size; i++) {
                        u[0] = (char)(64 + (i % 26));
                        String update = new String(u);
                        handler.basicUpdate(update);
                        log.update(update, true);
                        writ.append(update);
                    }

                    // Fail
                    String f = ("FALL" + deathpoint);
                    boolean failed_as_desired = false;
                    try {
                        ReliableLog.fallOverPoint = deathpoint;
                        handler.basicUpdate(f);
                        log.update(f, true);
                        log.snapshot(handler);
                    } catch (InternalError e) {
                        if (!e.getMessage().equals(f))
                            throw e; // oops, not ours
                        failed_as_desired = true;
                    } finally {
                        ReliableLog.fallOverPoint = 0;
                        try {
                            log.close();
                        } catch (IOException ign) {
                        }
                    }

                    // deathpoint == 0 means that there is no deathpoint and we are testing
                    // undisastered behaviour.
                    if (!failed_as_desired && deathpoint != 0) {
                        System.err.println ("sun.rmi.log.ReliableLog is not instrumented for" +
                                            " this test; test skipped");
                        return;
                    }

                    // Now try to recover.
                    Recovery laz = new Recovery();
                    ReliableLog carbon = new ReliableLog(dir, laz);
                    Recovery thingy;
                    thingy = (Recovery)carbon.recover();
                    try {
                        carbon.close();
                    } catch (IOException ign) {
                    }

                    // Recovered thingy must be equal to writ or to writ + f, but nothing
                    // else (or in between).
                    String recovered = thingy.contents;
                    String sacr = writ.toString();
                    if (recovered.length() == sacr.length()
                        && recovered.compareTo(sacr) == 0)
                    {
                        lg.println("Passed test " + size + "/" + deathpoint
                                   + ": rolled back to v1");
                    } else if (recovered.length() == (sacr.length() + f.length())
                               && recovered.compareTo(sacr + f) == 0)
                    {
                        lg.println("Passed test " + size + "/" + deathpoint
                                   + ": committed to v2");
                    } else {
                        final String q = "\"";
                        lg.println("Wrote " + sacr.length() + ": " + q + sacr + q);
                        lg.println("Simulated failure during write "
                                   + f.length() + ": " + q + f + q);
                        lg.println("Recovered " + recovered.length() + ": " + q + recovered + q);
                        throw new InternalError("Failed test " + size + "/" + deathpoint
                                   + " (size/deathpoint):");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(lg);
            //return (Status.failed
            throw (new RuntimeException
                    ("Exception in sanity test for sun.rmi.log.ReliableLog"));
        }
        //return (Status.passed ("OKAY"));
    }

    private String contents;
    transient private StringBuffer trc = null;

    public Recovery()
    {
        super();
        if (this.contents == null)
            this.contents = "?";
    }

    // implements LogHandler.initialSnapshot()
    public Object initialSnapshot()
        throws Exception
    {
        this.contents = "";
        return (this);
    }

    // implements LogHandler.applyUpdate()
    public Object applyUpdate (Object update, Object state)
        throws Exception
    {
        // basicUpdate appends the string
        ((Recovery)state).basicUpdate ((String)update);
        return (state);
    }

    // an "update" is a short string to append to this.contents (must ignore state)
    public void basicUpdate (String extra)
    {
        this.contents = this.contents + extra;
    }
}
