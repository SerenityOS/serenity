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

abstract class IndexNameValueWriter implements BinaryRepresentationWriter {

    private final int pattern;
    private final int prefix;
    private final IntegerWriter intWriter = new IntegerWriter();
    private final StringWriter nameWriter = new StringWriter();
    private final StringWriter valueWriter = new StringWriter();

    protected boolean indexedRepresentation;

    private static final int NEW               = 0;
    private static final int NAME_PART_WRITTEN = 1;
    private static final int VALUE_WRITTEN     = 2;

    private int state = NEW;

    protected IndexNameValueWriter(int pattern, int prefix) {
        this.pattern = pattern;
        this.prefix = prefix;
    }

    IndexNameValueWriter index(int index) {
        indexedRepresentation = true;
        intWriter.configure(index, prefix, pattern);
        return this;
    }

    IndexNameValueWriter name(CharSequence name, boolean useHuffman) {
        indexedRepresentation = false;
        intWriter.configure(0, prefix, pattern);
        nameWriter.configure(name, useHuffman);
        return this;
    }

    IndexNameValueWriter value(CharSequence value, boolean useHuffman) {
        valueWriter.configure(value, useHuffman);
        return this;
    }

    @Override
    public boolean write(HeaderTable table, ByteBuffer destination) {
        if (state < NAME_PART_WRITTEN) {
            if (indexedRepresentation) {
                if (!intWriter.write(destination)) {
                    return false;
                }
            } else {
                if (!intWriter.write(destination) ||
                        !nameWriter.write(destination)) {
                    return false;
                }
            }
            state = NAME_PART_WRITTEN;
        }
        if (state < VALUE_WRITTEN) {
            if (!valueWriter.write(destination)) {
                return false;
            }
            state = VALUE_WRITTEN;
        }
        return state == VALUE_WRITTEN;
    }

    @Override
    public IndexNameValueWriter reset() {
        intWriter.reset();
        if (!indexedRepresentation) {
            nameWriter.reset();
        }
        valueWriter.reset();
        state = NEW;
        return this;
    }
}
