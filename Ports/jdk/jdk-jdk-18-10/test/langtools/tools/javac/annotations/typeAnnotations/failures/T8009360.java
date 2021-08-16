/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009360
 * @summary AssertionError from type annotation on member of anonymous class
 * @compile T8009360.java
 */
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

class Test1<T> {
    Object mtest( Test1<T> t){ return null; }
    public void test() {
        mtest( new Test1<T>() {
                @A String data1 = "test";    // ok
                @A @A String data2 = "test"; // ok
                @A @B String data3 = "test"; // was AssertionError
                @B @C String data4 = "test"; // was AssertionError
           });
   }
}

@Target({TYPE_USE,FIELD}) @Repeatable( AC.class) @interface A { }
@Target({TYPE_USE,FIELD}) @interface AC { A[] value(); }
@Target({TYPE_USE}) @interface B { }
@Target({TYPE_USE, FIELD}) @interface C { }
