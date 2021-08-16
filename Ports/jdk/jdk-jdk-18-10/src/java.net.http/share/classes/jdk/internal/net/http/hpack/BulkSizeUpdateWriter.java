/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static java.util.Objects.requireNonNull;

final class BulkSizeUpdateWriter implements BinaryRepresentationWriter {

    private final SizeUpdateWriter writer = new SizeUpdateWriter();
    private Iterator<Integer> maxSizes;
    private boolean writing;
    private boolean configured;

    BulkSizeUpdateWriter maxHeaderTableSizes(Iterable<Integer> sizes) {
        if (configured) {
            throw new IllegalStateException("Already configured");
        }
        requireNonNull(sizes, "sizes");
        maxSizes = sizes.iterator();
        configured = true;
        return this;
    }

    @Override
    public boolean write(HeaderTable table, ByteBuffer destination) {
        if (!configured) {
            throw new IllegalStateException("Configure first");
        }
        while (true) {
            if (writing) {
                if (!writer.write(table, destination)) {
                    return false;
                }
                writing = false;
            } else if (maxSizes.hasNext()) {
                writing = true;
                writer.reset();
                writer.maxHeaderTableSize(maxSizes.next());
            } else {
                configured = false;
                return true;
            }
        }
    }

    @Override
    public BulkSizeUpdateWriter reset() {
        maxSizes = null;
        writing = false;
        configured = false;
        return this;
    }
}
