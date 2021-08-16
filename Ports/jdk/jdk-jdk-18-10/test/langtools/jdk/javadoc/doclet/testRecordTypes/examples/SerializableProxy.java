/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package examples;

import java.io.Serializable;

/**
 * A class with a record for a serialization proxy.
 */
public class SerializableProxy implements Serializable {
    private int p;
    private int q;

    /**
     * The proxy.
     *
     * @param x the first item in the serialized form
     * @param y the second item in the serialized form
     *
     * @serial include
     */
    private record Proxy(int x, int y) {
        /**
         * Returns a deserialized object.
         *
         * @returns the deserialized object
         */
        public SerializableProxy readResolve() {
            return new SerializableProxy(x, y);
        }
    }

    /**
     * Returns the proxy object.
     *
     * @return the proxy object
     */
    public Object writeReplace() {
        return new Proxy(p, q);
    }
}
