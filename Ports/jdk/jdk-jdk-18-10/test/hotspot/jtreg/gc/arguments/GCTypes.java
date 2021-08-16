/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Objects;

/**
 * Helper class with enum representation of GC types.
 */
public final class GCTypes {

    private static <T extends GCType> T getCurrentGCType(Class<T> type) {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(bean -> getGCTypeByName(type, bean.getName()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static <T extends GCType> T getGCTypeByName(Class<T> type, String name) {
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.getGCName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static <T extends GCType> GarbageCollectorMXBean getGCBeanByType(Class<T> type) {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .filter(bean -> Arrays.stream(type.getEnumConstants())
                        .filter(enumName -> enumName.getGCName().equals(bean.getName()))
                        .findFirst()
                        .isPresent()
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper interface used by GCTypes static methods
     * to get gcTypeName field of *GCType classes.
     */
    private interface GCType {

        String getGCName();
    }

    public static enum YoungGCType implements GCType {
        DefNew("Copy"),
        PSNew("PS Scavenge"),
        G1("G1 Young Generation");

        @Override
        public String getGCName() {
            return gcTypeName;
        }
        private final String gcTypeName;

        private YoungGCType(String name) {
            gcTypeName = name;
        }

        public static YoungGCType getYoungGCType() {
            return GCTypes.getCurrentGCType(YoungGCType.class);
        }

        public static GarbageCollectorMXBean getYoungGCBean() {
            return GCTypes.getGCBeanByType(YoungGCType.class);
        }
    }

    public static enum OldGCType implements GCType {
        Serial("MarkSweepCompact"),
        PSOld("PS MarkSweep"),
        G1("G1 Old Generation");

        private final String gcTypeName;

        private OldGCType(String name) {
            gcTypeName = name;
        }

        public static OldGCType getOldGCType() {
            return GCTypes.getCurrentGCType(OldGCType.class);
        }

        @Override
        public String getGCName() {
            return gcTypeName;
        }
    }
}
