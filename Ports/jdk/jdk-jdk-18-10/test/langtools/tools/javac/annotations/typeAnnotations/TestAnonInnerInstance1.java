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
 * @bug 8026286
 * @summary This test previously forced an assertion to fail, due to
 *          TypeAnnotationPosition visiting a tree node prior to
 *          memberEnter.
 * @compile TestAnonInnerInstance1.java
 */

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
import java.util.List;

class TestAnonInnerInstance1<T> {
    Object mtest(TestAnonInnerInstance1<T> t){ return null; }
    Object mmtest(TestAnonInnerInstance1<T> t){ return null; }

    public void test() {

        mtest(new TestAnonInnerInstance1<T>() {
                  class InnerAnon<U> { // Test1$1$InnerAnon.class
                      @A @B @C @D String ia_m1(){ return null; };
                  }
    //If this is commented out, annotations are attributed correctly
                  InnerAnon<String> IA = new InnerAnon< String>();
              });
   }
}

@Retention(RUNTIME) @Target({TYPE_USE,FIELD}) @interface A { }
@Retention(RUNTIME) @Target({TYPE_USE,METHOD}) @interface B { }
@Retention(CLASS) @Target({TYPE_USE,FIELD}) @interface C { }
@Retention(CLASS) @Target({TYPE_USE,METHOD}) @interface D { }
