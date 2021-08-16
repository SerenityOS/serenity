/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Attributes implements Iterable<Attribute> {

    public final Attribute[] attrs;
    public final Map<String, Attribute> map;

    Attributes(ClassReader cr) throws IOException {
        map = new HashMap<>();
        int attrs_count = cr.readUnsignedShort();
        attrs = new Attribute[attrs_count];
        for (int i = 0; i < attrs_count; i++) {
            Attribute attr = Attribute.read(cr);
            attrs[i] = attr;
            try {
                map.put(attr.getName(cr.getConstantPool()), attr);
            } catch (ConstantPoolException e) {
                // don't enter invalid names in map
            }
        }
    }

    public Attributes(ConstantPool constant_pool, Attribute[] attrs) {
        this.attrs = attrs;
        map = new HashMap<>();
        for (Attribute attr : attrs) {
            try {
                map.put(attr.getName(constant_pool), attr);
            } catch (ConstantPoolException e) {
                // don't enter invalid names in map
            }
        }
    }

    public Attributes(Map<String, Attribute> attributes) {
        this.attrs = attributes.values().toArray(new Attribute[attributes.size()]);
        map = attributes;
    }

    public Iterator<Attribute> iterator() {
        return Arrays.asList(attrs).iterator();
    }

    public Attribute get(int index) {
        return attrs[index];
    }

    public Attribute get(String name) {
        return map.get(name);
    }

    public int getIndex(ConstantPool constant_pool, String name) {
        for (int i = 0; i < attrs.length; i++) {
            Attribute attr = attrs[i];
            try {
                if (attr != null && attr.getName(constant_pool).equals(name))
                    return i;
            } catch (ConstantPoolException e) {
                // ignore invalid entries
            }
        }
        return -1;
    }

    public int size() {
        return attrs.length;
    }

    public int byteLength() {
        int length = 2;
        for (Attribute a: attrs)
            length += a.byteLength();
        return length;
    }
}
