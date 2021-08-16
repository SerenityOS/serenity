/*
 * Copyright 2014 Goldman Sachs.
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7142035
 * @summary Assert in java.lang.instrument agents during shutdown when classloading occurs after shutdown
 * @library /test/lib
 *
 * @modules java.instrument
 *          java.management
 * @build DummyAgent DummyClass TestDaemonThreadLauncher TestDaemonThread
 * @run shell ../MakeJAR3.sh DummyAgent
 * @run main/timeout=240 TestDaemonThreadLauncher
 *
 */
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class TestDaemonThread implements Runnable{
    File classpath;

    public TestDaemonThread(File classpath) {
        this.classpath = classpath;
    }

    @Override
    public void run() {


        try {
            URL u = this.getClass().getClassLoader().getResource("DummyClass.class");
            String path = u.getPath();
            String parent = u.getPath().substring(0, path.lastIndexOf('/')+1);
            URL parentURL = new URL(u, parent);
            System.out.println(parentURL);
            /* Load lots of class by creating multiple classloaders */
            for(;;) {
                ClassLoader cl = new URLClassLoader(new URL[] {parentURL}, null);
                cl.loadClass("DummyClass");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
           Thread t = new Thread(new TestDaemonThread(new File(args[0])));
           /* The important part of the bug is that a Daemon thread can continue to load classes after shutdown */
           t.setDaemon(true);
           t.start();
           Thread.sleep(200);
    }
}
