/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * See JVMS, section 4.8.15.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SourceDebugExtension_attribute extends Attribute {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    SourceDebugExtension_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        debug_extension = new byte[attribute_length];
        cr.readFully(debug_extension);
    }

    public SourceDebugExtension_attribute(ConstantPool constant_pool, byte[] debug_extension)
            throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.SourceDebugExtension), debug_extension);
    }

    public SourceDebugExtension_attribute(int name_index, byte[] debug_extension) {
        super(name_index, debug_extension.length);
        this.debug_extension = debug_extension;
    }

    public String getValue() {
        return new String(debug_extension, UTF8);
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitSourceDebugExtension(this, data);
    }

    public final byte[] debug_extension;
}
