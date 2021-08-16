/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package pkg6;

import java.io.IOException;

public class Base<T> {
    /**
     *  This is Base::m1.
     *  @return something
     */
    public Object m1() { }

    /**
     *  This is Base::m2.
     *  @return something
     */
    public Object m2() { }

    /**
     *  This is Base::m3.
     *  @return something
     */
    public T m3() { }

    /**
     * This is Base::m4.
     */
    protected void m4() { }

    /**
     * This is Base::m5.
     * @throws IOException an error
     */
    public Object m5() throws IOException { }

    /**
     * This is Base::m6.
     */
    public Object m6() { }

    /**
     * This is Base::m7.
     */
    public abstract Object m7();

    /**
     * This is Base::m8.
     */
    public Object m8() { }

    /**
     * This is Base::m9.
     */
    public abstract Object m9();
}
