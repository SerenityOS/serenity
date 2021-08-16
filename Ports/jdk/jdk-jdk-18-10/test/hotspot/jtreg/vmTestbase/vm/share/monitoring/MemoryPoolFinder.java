/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.monitoring;

import java.lang.management.*;

public enum MemoryPoolFinder {
    CODE_CACHE,
        EDEN_SPACE,
        SURVIVOR_SPACE,
        OLD_GEN,
        PERM_GEN,
        METASPACE,
        CLASS_METASPACE;

    public static MemoryPoolMXBean findPool(MemoryPoolFinder pool) {
        for(MemoryPoolMXBean candidate : ManagementFactory.getMemoryPoolMXBeans()) {
            boolean found = false;
            switch(pool) {
            case CODE_CACHE:
                found = candidate.getName().contains("Code Cache");
                break;
            case EDEN_SPACE:
                found = candidate.getName().contains("Eden");
                break;
            case SURVIVOR_SPACE:
                found = candidate.getName().contains("Survivor");
                break;
            case OLD_GEN:
                found = candidate.getName().contains("Old") || candidate.getName().contains("Tenured");
                break;
            case PERM_GEN:
                found = candidate.getName().contains("Perm");
                break;
            case METASPACE:
                found = candidate.getName().contains("Metaspace") && !candidate.getName().contains("Class Metaspace");
                break;
            case CLASS_METASPACE:
                found = candidate.getName().contains("Class Metaspace");
                break;
            }
            if (found) return candidate;
        }
        return null;
    }
}
