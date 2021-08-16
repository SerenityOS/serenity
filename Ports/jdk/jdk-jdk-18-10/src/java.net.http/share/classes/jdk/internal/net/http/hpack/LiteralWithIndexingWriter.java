/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

final class LiteralWithIndexingWriter extends IndexNameValueWriter {

    private boolean tableUpdated;

    private CharSequence name;
    private CharSequence value;
    private int index;

    LiteralWithIndexingWriter() {
        super(0b0100_0000, 6);
    }

    @Override
    LiteralWithIndexingWriter index(int index) {
        super.index(index);
        this.index = index;
        return this;
    }

    @Override
    LiteralWithIndexingWriter name(CharSequence name, boolean useHuffman) {
        super.name(name, useHuffman);
        this.name = name;
        return this;
    }

    @Override
    LiteralWithIndexingWriter value(CharSequence value, boolean useHuffman) {
        super.value(value, useHuffman);
        this.value = value;
        return this;
    }

    @Override
    public boolean write(HeaderTable table, ByteBuffer destination) {
        if (!tableUpdated) {
            CharSequence n;
            if (indexedRepresentation) {
                n = table.get(index).name;
            } else {
                n = name;
            }
            table.put(n, value);
            tableUpdated = true;
        }
        return super.write(table, destination);
    }

    @Override
    public IndexNameValueWriter reset() {
        tableUpdated = false;
        name = null;
        value = null;
        index = -1;
        return super.reset();
    }
}
