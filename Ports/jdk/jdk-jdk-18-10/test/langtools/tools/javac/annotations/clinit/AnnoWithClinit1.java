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
 * @bug 8013485
 * @summary Annotations that gets a clinit can't be verified for correct elements in a second compilation unit
 * @compile AnnoWithClinit1.java
 */

public @interface AnnoWithClinit1 {
    Foo f = new Foo();

    @AnnoWithClinit1
    static class C {} // this is in the same CU so there wont be a
                      // <clinit> when the this anno instance is checked

    class Foo {}
}


@AnnoWithClinit1
class BarAnnoClinit1 {}

@interface AAnnoClinit1 {
    Runnable r2 = new Runnable() { public void run() { }};
    String str1();
    String str2withdefault() default "bar";
}

@AAnnoClinit1(str1="value")
class TestAnnoClinit1 { }
