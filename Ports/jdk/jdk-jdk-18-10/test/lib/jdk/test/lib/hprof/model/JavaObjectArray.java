/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import jdk.test.lib.hprof.parser.ReadBuffer;

/**
 * @author      Bill Foote
 */
public class JavaObjectArray extends JavaLazyReadObject {

    private Object clazz;  // Long before resolve, the class after resolve

    public JavaObjectArray(long classID, long offset) {
        super(offset);
        this.clazz = makeId(classID);
    }

    public JavaClass getClazz() {
        return (JavaClass) clazz;
    }

    public void resolve(Snapshot snapshot) {
        if (clazz instanceof JavaClass) {
            return;
        }
        long classID = getIdValue((Number)clazz);
        if (snapshot.isNewStyleArrayClass()) {
            // Modern heap dumps do this
            JavaThing t = snapshot.findThing(classID);
            if (t instanceof JavaClass) {
                clazz = (JavaClass) t;
            }
        }
        if (!(clazz instanceof JavaClass)) {
            JavaThing t = snapshot.findThing(classID);
            if (t != null && t instanceof JavaClass) {
                JavaClass el = (JavaClass) t;
                String nm = el.getName();
                if (!nm.startsWith("[")) {
                    nm = "L" + el.getName() + ";";
                }
                clazz = snapshot.getArrayClass(nm);
            }
        }

        if (!(clazz instanceof JavaClass)) {
            clazz = snapshot.getOtherArrayType();
        }
        ((JavaClass)clazz).addInstance(this);
        super.resolve(snapshot);
    }

    public JavaThing[] getValues() {
        return getElements();
    }

    public JavaThing[] getElements() {
        return getValue();
    }

    public int compareTo(JavaThing other) {
        if (other instanceof JavaObjectArray) {
            return 0;
        }
        return super.compareTo(other);
    }

    public int getLength() {
        return (int)(getValueLength() / idSize());
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        super.visitReferencedObjects(v);
        JavaThing[] elements = getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null && elements[i] instanceof JavaHeapObject) {
                v.visit((JavaHeapObject) elements[i]);
            }
        }
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(JavaThing target, Snapshot ss) {
        JavaThing[] elements = getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == target) {
                return "Element " + i + " of " + this;
            }
        }
        return super.describeReferenceTo(target, ss);
    }

    /*
     * Java object array record (HPROF_GC_OBJ_ARRAY_DUMP)
     * looks as below:
     *
     *     object ID
     *     stack trace serial number (int)
     *     array length (int)
     *     array class ID
     *     array element IDs
     */
    @Override
    protected final long readValueLength() throws IOException {
        long offset = getOffset() + idSize() + 4;
        // length of the array in elements
        long len = buf().getInt(offset);
        // byte length of array
        return len * idSize();
        }

    private long dataStartOffset() {
        return getOffset() + idSize() + 4 + 4 + idSize();
    }

    @Override
    protected final JavaThing[] readValue() throws IOException {
        Snapshot snapshot = getClazz().getSnapshot();
        int len = getLength();
        long offset = dataStartOffset();

        JavaThing[] res = new JavaThing[len];
        for (int i = 0; i < len; i++) {
            long id = objectIdAt(offset);
            res[i] = snapshot.findThing(id);
            offset += idSize();
         }
         return res;
    }
}
