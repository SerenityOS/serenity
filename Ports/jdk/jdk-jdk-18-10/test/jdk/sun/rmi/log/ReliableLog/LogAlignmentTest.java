/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4094889
 * @summary rmid can have a corrupted log
 *
 * @modules java.rmi/sun.rmi.log
 * @run main/othervm LogAlignmentTest
 */

/* Fault: ReliableLog used RandomAccessFile.skipBytes() to seek past the end
 * of a file.  Unfortunately, skipBytes() doesn't do that, so the file was
 * being misaligned.
 *
 * Reproduced by writing an odd number of bytes into an update, and then
 * reloading.
 */

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import sun.rmi.log.LogHandler;
import sun.rmi.log.ReliableLog;

//import javasoft.sqe.harness.Status;
//import javasoft.sqe.harness.Test;

public class LogAlignmentTest
    extends LogHandler
    implements Serializable //, Test
{
    static public void main (String[] argv)
    {
        LogAlignmentTest test = new LogAlignmentTest();
        //Status status = test.run (argv, System.err, System.out);
        //status.exit();
        test.run (argv, System.err, System.out);
    }

    //public Status run (String argv[], PrintStream log, PrintStream out)
    public void run (String argv[], PrintStream log, PrintStream out)
    {
        try {
            regtest(130, "./logalign_tmp", log, out);
            regtest(131, "./logalign_tmp", log, out);
        } catch (Exception e) {
            e.printStackTrace (log);
            //return (Status.failed
            throw (new RuntimeException
                    ("Exception in regression test for bugid 4094889"));
        }
        //return (Status.passed ("OKAY"));
    }

    static private void regtest(int updatesz, String dir, PrintStream lg, PrintStream out)
        throws Exception
    {
        try {
            LogAlignmentTest handler = new LogAlignmentTest();
            ReliableLog log = new ReliableLog (dir, handler, false);

            // Write a preamble update
            String c = "[";
            handler.basicUpdate (c);
            log.update (c, true);

            // Generate the requested size update (single chars)
            char[] up = new char[updatesz];
            int i;
            for (i = 0; i < updatesz; i++) {
                up[i] = (char)(65 + (i % 26));
            }
            c = new String (up);
            handler.basicUpdate (c);
            log.update (c, true);

            // Write it again, so we can misalign
            handler.basicUpdate (c);
            log.update (c, true);

            // Write the suffix
            c = "]";
            handler.basicUpdate (c);
            log.update (c, true);

            // Read it back using a new context.
            LogAlignmentTest handler2 = new LogAlignmentTest();
            ReliableLog carbon = new ReliableLog (dir, handler2, false);
            Object thingy = carbon.recover();

            // The report bit
            String orig = handler.contents;
            String news = ((LogAlignmentTest)thingy).contents;
            lg.println ("Original as saved: " + orig);
            lg.println ("As restored      : " + news);
            if (orig.compareTo (news) != 0) {
                throw new RuntimeException ("Restored string was different from saved string");
            } else {
                lg.println ("Matched OK.  Test element passed.");
            }
        } finally {
            // Trash the log directory, so a new snap will be taken in the next test
            try {
                File vs = new File (dir, "Version_Number");
                vs.delete();
            } catch (Exception e) {
            }
            try {
                File vs = new File (dir, "New_Version_Number");
                vs.delete();
            } catch (Exception e) {
            }
        }
    }

    private String contents;

    public LogAlignmentTest()
    {
        super();
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
        ((LogAlignmentTest)state).basicUpdate ((String)update);
        return (state);
    }

    // an "update" is a short string to append to this.contents (must ignore state)
    public void basicUpdate (String extra)
    {
        this.contents = this.contents + extra;
    }
}
