/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8201793
 * @summary Test Reference::clone to throw CloneNotSupportedException
 */

import java.lang.ref.*;

public class ReferenceClone {
    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();
    public static void main(String... args) {
        ReferenceClone refClone = new ReferenceClone();
        refClone.test();
    }

    public void test() {
        // test Reference::clone that throws CNSE
        Object o = new Object();
        assertCloneNotSupported(new SoftRef(o));
        assertCloneNotSupported(new WeakRef(o));
        assertCloneNotSupported(new PhantomRef(o));

        // Reference subclass may override the clone method
        CloneableReference ref = new CloneableReference(o);
        try {
            ref.clone();
        } catch (CloneNotSupportedException e) {}
    }

    private void assertCloneNotSupported(CloneableRef ref) {
        try {
            ref.clone();
            throw new RuntimeException("Reference::clone should throw CloneNotSupportedException");
        } catch (CloneNotSupportedException e) {}
    }

    // override clone to be public that throws CNSE
    interface CloneableRef extends Cloneable {
        public Object clone() throws CloneNotSupportedException;
    }

    class SoftRef extends SoftReference<Object> implements CloneableRef {
        public SoftRef(Object referent) {
            super(referent, QUEUE);
        }
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    class WeakRef extends WeakReference<Object> implements CloneableRef {
        public WeakRef(Object referent) {
            super(referent, QUEUE);
        }
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    class PhantomRef extends PhantomReference<Object> implements CloneableRef {
        public PhantomRef(Object referent) {
            super(referent, QUEUE);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    // override clone to return a new instance
    class CloneableReference extends WeakReference<Object> implements Cloneable {
        public CloneableReference(Object referent) {
            super(referent, QUEUE);
        }

        public Object clone() throws CloneNotSupportedException {
            return new CloneableReference(this.get());
        }
    }

}
