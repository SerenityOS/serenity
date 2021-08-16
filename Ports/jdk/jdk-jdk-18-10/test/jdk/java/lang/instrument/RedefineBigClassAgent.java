/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class RedefineBigClassAgent {
    // N_REDEFINES = 2000 made my Solaris X86 box crawl on its knees
    // with the Server VM. The timeout is 600 seconds, but the test
    // ran for 675 seconds so it took the harness 75 seconds to kill
    // the test.
    private static int N_REDEFINES = 1000;
    public static Class clz;
    public static volatile boolean doneRedefining = false;

    // just read the original class and redefine in a loop via a Timer
    public static void premain(String agentArgs, final Instrumentation inst) throws Exception {
        String s = agentArgs.substring(0, agentArgs.indexOf(".class"));
        clz = Class.forName(s.replace('/', '.'));
        ClassLoader loader =
            RedefineBigClassAgent.class.getClassLoader();
        URL classURL = loader.getResource(agentArgs);
        if (classURL == null) {
            throw new Exception("Cannot find class: " + agentArgs);
        }

        int         redefineLength;
        InputStream redefineStream;

        System.out.println("Reading test class from " + classURL);
        if (classURL.getProtocol().equals("file")) {
            File f = new File(classURL.getFile());
            redefineStream = new FileInputStream(f);
            redefineLength = (int) f.length();
        } else {
            URLConnection conn = classURL.openConnection();
            redefineStream = conn.getInputStream();
            redefineLength = conn.getContentLength();
        }

        final byte[] buffer = new byte[redefineLength];
        new BufferedInputStream(redefineStream).read(buffer);

        System.gc();  // throw away anything we can before we start testing

        new Timer(true).schedule(new TimerTask() {
            public void run() {
                try {
                    int i;
                    System.out.println("Redefining");
                    ClassDefinition cld = new ClassDefinition(clz, buffer);
                    for (i = 0; i < N_REDEFINES; i++) {
                        inst.redefineClasses(new ClassDefinition[] { cld });
                        System.gc();  // throw away anything we can
                    }
                    System.out.println("Redefined " + i + " times.");
                    RedefineBigClassAgent.doneRedefining = true;
                }
                catch (Exception e) { e.printStackTrace(); }
            }
        }, 500);
    }
}
