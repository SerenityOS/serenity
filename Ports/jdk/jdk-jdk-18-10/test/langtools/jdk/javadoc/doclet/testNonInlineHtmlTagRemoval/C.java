/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

public class C {
    /**
     * case1 <ul> <li> end of sentence. <li> more </ul>
     */
    public void case1() {}

    /**
     * case2 <ul compact> <li> end of sentence. <li> more </ul>
     */
    public void case2() {}

    /**
     * case3 <ul type="square"> <li> end of sentence. <li> more </ul>
     */
    public void case3() {}

    /**
     * case4 <ul type="a<b"> <li> end of sentence. <li> more </ul>
     */
    public void case4() {}

    /**
     * case5 <ul type="a>b"> <li> end of sentence. <li> more </ul>
     */
    public void case5() {}

    /**
     * case6 <ul type='a>b'> <li> end of sentence. <li> more </ul>
     */
    public void case6() {}

    /**
     * case7 <ul type='"a>b"'> <li> end of sentence. <li> more </ul>
     */
    public void case7() {}

    /**
     * case8 <ul type="'a>b'"> <li> end of sentence. <li> more </ul>
     */
    public void case8() {}

    /**
     * case9 <ul type="'a'>b"> <li> end of sentence. <li> more </ul>
     */
    public void case9() {}

    /**
     * caseA <ul type='"a">b'> <li> end of sentence. <li> more </ul>
     */
    public void caseA() {}

    /**
     * caseB <blockquote>A block quote example:</blockquote>
     */
    public void caseB() {}
}
