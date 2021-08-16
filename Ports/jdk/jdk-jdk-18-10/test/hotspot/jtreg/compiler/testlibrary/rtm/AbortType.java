/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.rtm;

import jdk.test.lib.Asserts;

import java.util.HashMap;
import java.util.Map;

/**
 * Type of transactional execution abort.
 * For more details on different abort types please see
 * shared/vm/runtime/rtmLocking.hpp
 */
public enum AbortType {
    XABORT(0),
    RETRIABLE(1),
    MEM_CONFLICT(2),
    BUF_OVERFLOW(3),
    DEBUG_BREAKPOINT(4),
    NESTED_ABORT(5);

    private final int type;
    private static final Map<Integer, AbortType> LOOKUP_MAP = new HashMap<>();

    static {
        for (AbortType abortType : AbortType.values()) {
            Asserts.assertFalse(LOOKUP_MAP.containsKey(abortType.type),
                    "Abort type values should be unique.");
            LOOKUP_MAP.put(abortType.type, abortType);
        }
    }

    private AbortType(int type) {
        this.type = type;
    }

    /**
     * Returns AbortProvoker for aborts represented by this abort type.
     *
     * @return an AbortProvoker instance
     */
    public AbortProvoker provoker() {
        return AbortType.createNewProvoker(this);
    }

    public static AbortType lookup(int type) {
        Asserts.assertLT(type, AbortType.values().length,
                "Unknown abort type.");
        return LOOKUP_MAP.get(type);
    }

    /**
     * Returns transaction execution abort provoker for specified abortion type.
     *
     * @param type a type of abort which will be forced by returned
     *             AbortProvoker instance.
     * @return AbortProvoker instance that will force abort of specified type
     * @throws RuntimeException if there is no provoker for specified type
     */
    private static AbortProvoker createNewProvoker(AbortType type) {
        switch (type) {
            case XABORT:
                return new XAbortProvoker();
            case MEM_CONFLICT:
                return new MemoryConflictProvoker();
            case BUF_OVERFLOW:
                return new BufferOverflowProvoker();
            case NESTED_ABORT:
                return new NestedAbortProvoker();
            default:
                throw new RuntimeException("No provoker exists for type "
                        + type.name());
        }
    }

    @Override
    public String toString() {
        return Integer.toString(type);
    }
}
