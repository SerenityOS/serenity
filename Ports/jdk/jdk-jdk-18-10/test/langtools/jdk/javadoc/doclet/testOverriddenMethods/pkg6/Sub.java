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

public class Sub<T> extends Base<T> {
    // simple override
    @Override
    public Object m1() { }
    // covariant override
    @Override
    public String m2() { }
    // not a covariant override
    @Override
    public T m3() { }
    // change visibility to public
    @Override
    public void m4() { }
    // drop checked exception
    @Override
    public Object m5() { }
    // add final modifier
    @Override
    public final Object m6() { }
    // implement abstract method
    @Override
    public Object m7() { }
    // override concrete method as abstract
    @Override
    public abstract Object m8();
    // override abstract method unchanged
    @Override
    public abstract Object m9();
}

