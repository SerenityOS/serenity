/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

/**
 *
 * @author      Bill Foote
 */

/**
 * Represents the value of a static field of a JavaClass
 */

public class JavaStatic {

    private JavaField field;
    private JavaThing value;

    public JavaStatic(JavaField field, JavaThing value) {
        this.field = field;
        this.value = value;
    }

    public void resolve(JavaClass clazz, Snapshot snapshot) {
        long id = -1;
        if (value instanceof JavaObjectRef) {
            id = ((JavaObjectRef)value).getId();
        }
        value = value.dereference(snapshot, field);
        if (value.isHeapAllocated() &&
            clazz.getLoader() == snapshot.getNullThing()) {
            // static fields are only roots if they are in classes
            //    loaded by the root classloader.
            JavaHeapObject ho = (JavaHeapObject) value;
            String s = "Static reference from " + clazz.getName()
                       + "." + field.getName();
            snapshot.addRoot(new Root(id, clazz.getId(),
                                      Root.JAVA_STATIC, s));
        }
    }

    public JavaField getField() {
        return field;
    }

    public JavaThing getValue() {
        return value;
    }
}
