/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

/**
 * Qualified Link: {@link pkg.C.InnerC}.<br/>
 * Unqualified Link1: {@link C.InnerC}.<br/>
 * Unqualified Link2: {@link InnerC}.<br/>
 * Qualified Link: {@link #method(pkg.C.InnerC, pkg.C.InnerC2)}.<br/>
 * Unqualified Link: {@link #method(C.InnerC, C.InnerC2)}.<br/>
 * Unqualified Link: {@link #method(InnerC, InnerC2)}.<br/>
 * Package Link: {@link pkg}.<br/>
 *
 *
 */
public class C {

    public InnerC MEMBER = new InnerC();
    /**
     *  A red herring inner class to confuse the matching, thus to
     *  ensure the right one is linked.
     */
    public class RedHerringInnerC {}

    /**
     * Link to member in outer class: {@link #MEMBER} <br/>
     * Link to member in inner class: {@link InnerC2#MEMBER2} <br/>
     * Link to another inner class: {@link InnerC2}
     */
    public class InnerC {}

    /**
     * Link to conflicting member in inner class: {@link #MEMBER} <br/>
     */
    public class InnerC2 {
        public static final int MEMBER = 1;
        public static final int MEMBER2 = 1;
    }

    public void method(InnerC p1, InnerC2 p2){}

}
