/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import jdk.jfr.internal.SecuritySupport.SafePath;

// This class keeps track of files that can't be deleted
// so they can a later staged be removed.
final class FilePurger {

    private static final Set<SafePath> paths = new LinkedHashSet<>();

    public synchronized static void add(SafePath p) {
        paths.add(p);
        if (paths.size() > 1000) {
            removeOldest();
        }
    }

    public synchronized static void purge() {
        if (paths.isEmpty()) {
            return;
        }

        for (SafePath p : new ArrayList<>(paths)) {
            if (delete(p)) {
                paths.remove(p);
            }
        }
    }

    private static void removeOldest() {
        SafePath oldest = paths.iterator().next();
        paths.remove(oldest);
    }

    private static boolean delete(SafePath p) {
        try {
            SecuritySupport.delete(p);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
