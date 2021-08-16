/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4421190
 * @summary Tests that Image I/O statics may be referenced properly from
 *          multiple AppContexts, as would be the case for multiple Applets in a
 *          single VM. Each AppContext should get its own copy of the registry
 *          and the caching parameters in the ImageIO class.
 * @modules java.desktop/sun.awt
 */

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import sun.awt.SunToolkit;

class TestThread extends Thread {

    IIORegistry registry;
    boolean useCache;
    File cacheDirectory;
    boolean cacheSettingsOK = false;
    String threadName;

    boolean gotCrosstalk = false;

    public TestThread(ThreadGroup tg,
                      boolean useCache, File cacheDirectory,
                      String threadName) {
        super(tg, threadName);
        this.useCache = useCache;
        this.cacheDirectory = cacheDirectory;
        this.threadName = threadName;
    }

    public void run() {
//          System.out.println("Thread " + threadName + " in thread group " +
//                             getThreadGroup().getName());

        // Create a new AppContext as though we were an applet
        SunToolkit.createNewAppContext();

        // Get default registry and store reference
        this.registry = IIORegistry.getDefaultInstance();

        for (int i = 0; i < 10; i++) {
//              System.out.println(threadName +
//                                 ": setting cache parameters to " +
//                                 useCache + ", " + cacheDirectory);
            ImageIO.setUseCache(useCache);
            ImageIO.setCacheDirectory(cacheDirectory);

            try {
                sleep(1000L);
            } catch (InterruptedException e) {
            }

//              System.out.println(threadName + ": reading cache parameters");
            boolean newUseCache = ImageIO.getUseCache();
            File newCacheDirectory = ImageIO.getCacheDirectory();
            if (newUseCache != useCache ||
                newCacheDirectory != cacheDirectory) {
//                  System.out.println(threadName + ": got " +
//                                     newUseCache + ", " +
//                                     newCacheDirectory);
//                  System.out.println(threadName + ": crosstalk encountered!");
                gotCrosstalk = true;
            }
        }
    }

    public IIORegistry getRegistry() {
        return registry;
    }

    public boolean gotCrosstalk() {
        return gotCrosstalk;
    }
}

public class AppContextTest {

    public AppContextTest() {
        ThreadGroup tg0 = new ThreadGroup("ThreadGroup0");
        ThreadGroup tg1 = new ThreadGroup("ThreadGroup1");

        TestThread t0 =
            new TestThread(tg0, false, null, "TestThread 0");
        TestThread t1 =
            new TestThread(tg1, true, new File("."), "TestThread 1");

        t0.start();
        t1.start();

        try {
            t0.join();
        } catch (InterruptedException ie0) {
        }
        try {
            t1.join();
        } catch (InterruptedException ie1) {
        }

        if (t0.gotCrosstalk() || t1.gotCrosstalk()) {
            throw new RuntimeException("ImageIO methods had crosstalk!");
        }

        if (t0.getRegistry() == t1.getRegistry()) {
            throw new RuntimeException("ThreadGroups had same IIORegistry!");
        }
    }

    public static void main(String[] args) throws IOException {
        new AppContextTest();
    }
}
