/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.font;

import java.io.File;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import sun.awt.AppContext;
import sun.awt.util.ThreadGroupUtils;

public class CreatedFontTracker {

    public static final int MAX_FILE_SIZE = 32 * 1024 * 1024;
    public static final int MAX_TOTAL_BYTES = 10 * MAX_FILE_SIZE;

    static CreatedFontTracker tracker;
    int numBytes;

    public static synchronized CreatedFontTracker getTracker() {
        if (tracker == null) {
            tracker = new CreatedFontTracker();
        }
        return tracker;
    }

    private CreatedFontTracker() {
        numBytes = 0;
    }

    public synchronized int getNumBytes() {
        return numBytes;
    }

    public synchronized void addBytes(int sz) {
        numBytes += sz;
    }

    public synchronized void subBytes(int sz) {
        numBytes -= sz;
    }

    /**
     * Returns an AppContext-specific counting semaphore.
     */
    private static synchronized Semaphore getCS() {
        final AppContext appContext = AppContext.getAppContext();
        Semaphore cs = (Semaphore) appContext.get(CreatedFontTracker.class);
        if (cs == null) {
            // Make a semaphore with 5 permits that obeys the first-in first-out
            // granting of permits.
            cs = new Semaphore(5, true);
            appContext.put(CreatedFontTracker.class, cs);
        }
        return cs;
    }

    public boolean acquirePermit() throws InterruptedException {
        // This does a timed-out wait.
        return getCS().tryAcquire(120, TimeUnit.SECONDS);
    }

    public void releasePermit() {
        getCS().release();
    }

    public void add(File file) {
        TempFileDeletionHook.add(file);
    }

    public void set(File file, OutputStream os) {
        TempFileDeletionHook.set(file, os);
    }

    public void remove(File file) {
        TempFileDeletionHook.remove(file);
    }

    /**
     * Helper class for cleanup of temp files created while processing fonts.
     * Note that this only applies to createFont() from an InputStream object.
     */
    private static class TempFileDeletionHook {
        private static HashMap<File, OutputStream> files = new HashMap<>();

        private static Thread t = null;
        @SuppressWarnings("removal")
        static void init() {
            if (t == null) {
                // Add a shutdown hook to remove the temp file.
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    /* The thread must be a member of a thread group
                     * which will not get GCed before VM exit.
                     * Make its parent the top-level thread group.
                     */
                    ThreadGroup rootTG = ThreadGroupUtils.getRootThreadGroup();
                    t = new Thread(rootTG, TempFileDeletionHook::runHooks,
                                   "TempFontFileDeleter", 0, false);
                    /* Set context class loader to null in order to avoid
                     * keeping a strong reference to an application classloader.
                     */
                    t.setContextClassLoader(null);
                    Runtime.getRuntime().addShutdownHook(t);
                    return null;
                });
            }
        }

        private TempFileDeletionHook() {}

        static synchronized void add(File file) {
            init();
            files.put(file, null);
        }

        static synchronized void set(File file, OutputStream os) {
            files.put(file, os);
        }

        static synchronized void remove(File file) {
            files.remove(file);
        }

        static synchronized void runHooks() {
            if (files.isEmpty()) {
                return;
            }

            for (Map.Entry<File, OutputStream> entry : files.entrySet()) {
                // Close the associated output stream, and then delete the file.
                try {
                    if (entry.getValue() != null) {
                        entry.getValue().close();
                    }
                } catch (Exception e) {}
                entry.getKey().delete();
            }
        }
    }
}
