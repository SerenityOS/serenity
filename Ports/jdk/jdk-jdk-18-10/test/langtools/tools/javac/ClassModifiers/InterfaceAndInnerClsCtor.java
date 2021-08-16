/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4221648
 * @summary If an interface has an inner class, the constructor for that inner
 * class must be accessible to the interface, regardless of whether it is
 * explicitly declared private.
 *
 * @clean InterfaceAndInnerClsCtor
 * @compile InterfaceAndInnerClsCtor.java
 */

public interface InterfaceAndInnerClsCtor
{
    // All interface memebers are implicitly public. Hence, there is no need to
    // have other inner classes with different access levels.
    public static class Inner {
        // A constructor for each of the possible access levels.
        public Inner(boolean b) {}
        Inner(char c) {}
        protected Inner(int i) {}
        private Inner() {}
    }

    // Verify that all of the constructors are accessible at compile time.
    public final static Inner i0 = new Inner(true);
    public final static Inner i1 = new Inner('a');
    public final static Inner i2 = new Inner(7);
    public final static Inner i3 = new Inner();
}
