/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ThreadFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The "privileged" ThreadFactory used by the AsExecutor test.
 */

public class PrivilegedThreadFactory implements ThreadFactory {
    public void PrivilegedThreadPoolFactory() {
        // check class is on boot class path
        if (PrivilegedThreadFactory.class.getClassLoader() != null)
            throw new RuntimeException("PrivilegedThreadFactory class not on boot class path");
    }

    @Override
    public Thread newThread(final Runnable r) {
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }
}
