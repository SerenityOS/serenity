/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @bug 4363844
 * @summary Verify that a custom readObjectNoData method, if defined properly
 *          by a serializable superclass, gets invoked during deserialization
 *          of a subclass instance whose serialized form omits a class
 *          descriptor corresponding to the superclass.
 */

import java.io.*;

/* Non-serializable superclass which defines readObjectNoData:
 * readObjectNoData should not get called.
 */
class A {
    private static final long serialVersionUID = 0L;
    boolean aCalled = false;
    private void readObjectNoData() throws ObjectStreamException {
        aCalled = true;
    }
}

/* Serializable superclass which defines readObjectNoData with wrong signature:
 * readObjectNoData should not get called.
 */
class B extends A implements Serializable {
    private static final long serialVersionUID = 0L;
    boolean bCalled = false;
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private void readObjectNoData(int wrong) throws ObjectStreamException {
        bCalled = true;
    }
}

/* Serializable superclass which defines readObjectNoData correctly, and is not
 * listed in stream: readObjectNoData should get called.
 */
class C extends B {
    private static final long serialVersionUID = 0L;
    boolean cCalled = false;
    private void readObjectNoData() throws ObjectStreamException {
        cCalled = true;
    }
}

/* Serializable superclass which defines readObjectNoData correctly, but whose
 * corresponding class descriptor is listed in stream: readObjectNoData should
 * not get called.
 */
class D extends C {
    private static final long serialVersionUID = 0L;
    boolean dCalled = false;
    private void readObjectNoData() throws ObjectStreamException {
        dCalled = true;
    }
}

/* Serializable superclass which defines readObjectNoData with wrong access:
 * readObjectNoData should not get called.
 */
class E extends D {
    private static final long serialVersionUID = 0L;
    boolean eCalled = false;
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    void readObjectNoData() throws ObjectStreamException {
        eCalled = true;
    }
}

/* Instance class.
 */
class F extends E {
    private static final long serialVersionUID = 0L;
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            F f = (F) oin.readObject();
            if (f.aCalled || f.bCalled || f.dCalled || f.eCalled) {
                throw new Error("readObjectNoData invoked erroneously");
            }
            if (! f.cCalled) {
                throw new Error("readObjectNoData not invoked");
            }
        } finally {
            in.close();
        }
    }
}
