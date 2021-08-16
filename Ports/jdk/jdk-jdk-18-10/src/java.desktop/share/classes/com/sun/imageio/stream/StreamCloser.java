/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.stream;

import sun.awt.util.ThreadGroupUtils;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.WeakHashMap;
import javax.imageio.stream.ImageInputStream;

/**
 * This class provide means to properly close hanging
 * image input/output streams on VM shutdown.
 * This might be useful for proper cleanup such as removal
 * of temporary files.
 *
 * Addition of stream do not prevent it from being garbage collected
 * if no other references to it exists. Stream can be closed
 * explicitly without removal from StreamCloser queue.
 * Explicit removal from the queue only helps to save some memory.
 */
public class StreamCloser {

    private static WeakHashMap<CloseAction, Object> toCloseQueue;
    private static Thread streamCloser;

    @SuppressWarnings("removal")
    public static void addToQueue(CloseAction ca) {
        synchronized (StreamCloser.class) {
            if (toCloseQueue == null) {
                toCloseQueue =
                    new WeakHashMap<CloseAction, Object>();
            }

            toCloseQueue.put(ca, null);

            if (streamCloser == null) {
                final Runnable streamCloserRunnable = new Runnable() {
                    public void run() {
                        if (toCloseQueue != null) {
                            synchronized (StreamCloser.class) {
                                Set<CloseAction> set =
                                    toCloseQueue.keySet();
                                // Make a copy of the set in order to avoid
                                // concurrent modification (the is.close()
                                // will in turn call removeFromQueue())
                                CloseAction[] actions =
                                    new CloseAction[set.size()];
                                actions = set.toArray(actions);
                                for (CloseAction ca : actions) {
                                    if (ca != null) {
                                        try {
                                            ca.performAction();
                                        } catch (IOException e) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                };

                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    /* The thread must be a member of a thread group
                     * which will not get GCed before VM exit.
                     * Make its parent the top-level thread group.
                     */
                    ThreadGroup tg = ThreadGroupUtils.getRootThreadGroup();
                    streamCloser = new Thread(tg, streamCloserRunnable,
                                              "StreamCloser", 0, false);
                    /* Set context class loader to null in order to avoid
                     * keeping a strong reference to an application classloader.
                     */
                    streamCloser.setContextClassLoader(null);
                    Runtime.getRuntime().addShutdownHook(streamCloser);
                    return null;
                });
            }
        }
    }

    public static void removeFromQueue(CloseAction ca) {
        synchronized (StreamCloser.class) {
            if (toCloseQueue != null) {
                toCloseQueue.remove(ca);
            }
        }
    }

    public static CloseAction createCloseAction(ImageInputStream iis) {
        return new CloseAction(iis);
    }

    public static final class CloseAction {
        private ImageInputStream iis;

        private CloseAction(ImageInputStream iis) {
            this.iis = iis;
        }

        public void performAction() throws IOException {
            if (iis != null) {
                iis.close();
            }
        }
    }
}
