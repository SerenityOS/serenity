/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.util;

import com.sun.hotspot.igv.data.Properties;
import com.sun.hotspot.igv.data.Property;
import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;

/**
 *
 * @author Thomas Wuerthinger
 */
public class PropertiesSheet {

    public static void initializeSheet(final Properties properties, Sheet s) {

        Sheet.Set set1 = Sheet.createPropertiesSet();
        set1.setDisplayName("Properties");
        for (final Property p : properties) {
            Node.Property<String> prop = new Node.Property<String>(String.class) {

                @Override
                public boolean canRead() {
                    return true;
                }

                @Override
                public String getValue() throws IllegalAccessException, InvocationTargetException {
                    return p.getValue();
                }

                @Override
                public boolean canWrite() {
                    return false;
                }

                @Override
                public void setValue(String arg0) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    properties.setProperty(p.getName(), arg0);
                }
            };
            prop.setName(p.getName());
            set1.put(prop);
        }
        s.put(set1);
    }
}
