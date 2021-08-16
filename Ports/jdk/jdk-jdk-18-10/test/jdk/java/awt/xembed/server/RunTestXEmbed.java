/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 4931668 7146533
 * @summary Tests XEmbed server/client functionality
 * @author Denis Mikhalkin: area=awt.xembed
 * @requires (!(os.family=="mac") & !(os.family=="windows"))
 * @library /test/lib
 * @modules java.desktop/sun.awt
 * @build jdk.test.lib.Platform
 * @compile JavaClient.java TesterClient.java TestXEmbedServer.java
 * @run main/timeout=6000 RunTestXEmbed
 */

import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import jdk.test.lib.Platform;

public class RunTestXEmbed extends TestXEmbedServer {
    private static final Logger log = Logger.getLogger("test.xembed");
    private Method test;
    private boolean passed = false;
    public RunTestXEmbed(Method test) {
        super(false);
        this.test = test;
    }

    public Process startClient(Rectangle bounds[], long window) {
        try {
            String java_home = System.getProperty("java.home");
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < bounds.length; i++) {
                buf.append(" " + bounds[i].x);
                buf.append(" " + bounds[i].y);
                buf.append(" " + bounds[i].width);
                buf.append(" " + bounds[i].height);
            }
            Map envs = System.getenv();
            String enva[] = new String[envs.size()];
            int ind = 0;
            Iterator iter = envs.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                if (!"AWT_TOOLKIT".equals(entry.getKey())) {
                    enva[ind++] = entry.getKey() + "=" + entry.getValue();
                } else {
                    enva[ind++] = "AWT_TOOLKIT=sun.awt.X11.XToolkit";
                }
            }
            Process proc = Runtime.getRuntime().
                exec(java_home +
                     "/bin/java --add-exports java.desktop/sun.awt.X11=ALL-UNNAMED -Dawt.toolkit=sun.awt.X11.XToolkit TesterClient "
                     + test.getName() + " " + window + buf,
                     enva);
            System.err.println("Test for " + test.getName() + " has started.");
            log.fine("Test for " + test.getName() + " has started.");
            new InputReader(proc.getInputStream());
            new InputReader(proc.getErrorStream());
            try {
                passed = (proc.waitFor() == 0);
            } catch (InterruptedException ie) {
            }
            log.fine("Test for " + test.getName() + " has finished.");
            File logFile = new File("java3.txt");
            if (logFile.exists()) {
                logFile.renameTo(new File(test.getName() + ".txt"));
            }
            return proc;
        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        if (Platform.isWindows() || Platform.isOSX()) {
            return;
        }

        // Enabled XEmbed
        System.setProperty("sun.awt.xembedserver", "true");

        if (args.length == 1) {
            Class cl = Class.forName("sun.awt.X11.XEmbedServerTester");
            Method meth = cl.getMethod(args[0], new Class[0]);
            System.err.println("Performing single test " + args[0]);
            boolean res = performTest(meth);
            if (!res) {
                System.err.println("Test " + args[0] + " has failed");
            } else {
                System.err.println("Test " + args[0] + " has passed");
            }
        } else {
            Class cl = Class.forName("sun.awt.X11.XEmbedServerTester");
            Method[] meths = cl.getMethods();
            LinkedList failed = new LinkedList();
            for (int i = 0; i < meths.length; i++) {
                Method meth = meths[i];
                if (meth.getReturnType() == Void.TYPE && meth.getName().startsWith("test") && meth.getParameterTypes().length == 0) {
                    System.err.println("Performing " + meth.getName());
                    boolean res = performTest(meth);
                    if (!res) {
                        failed.add(meth);
                    }
                }
            }
            log.info("Testing finished.");
            if (failed.size() != 0) {
                System.err.println("Some tests have failed:");
                Iterator iter = failed.iterator();
                while(iter.hasNext()) {
                    Method meth = (Method)iter.next();
                    System.err.println(meth.getName());
                }
                throw new RuntimeException("TestFAILED: some of the testcases are failed");
            } else {
                System.err.println("All PASSED");
            }
        }
    }

    private static boolean performTest(Method meth) {
        RunTestXEmbed test = new RunTestXEmbed(meth);
        test.addClient();
        test.dispose();
        return test.isPassed();
    }

    public boolean isPassed() {
        return passed;
    }
}

class InputReader extends Thread {
    private InputStream stream;
    public InputReader(InputStream stream) {
        this.stream = stream;
        start();
    }
    public void run() {
        while (!interrupted()) {
            try {
                int inp = stream.read();
                if (inp != -1) {
                    System.out.write(inp);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (Exception iie) {
                    }
                }
            } catch (IOException ie) {
                break;
            }
        }
    }
}
