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
 * @summary type-annotation on array level in nested class results in NPE
 * @bug 8008751
 * @compile T8008751.java
 */
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
import java.util.List;

class T8008751 {
    Object mtest( T8008751 t){ return null;  }
    public void test() {
       mtest( new T8008751() {
                class InnerAnon {
                    @A("ok") String s = (@A("ok") String)( new @A("ok") Object());
                    @A("ok") Object @A("NPE")[] [] ia_sa1 = null;
                }
                // If not instanciated, no crash.
                InnerAnon IA = new InnerAnon();
           });
   }
}
@Retention(RUNTIME) @Target(TYPE_USE)  @interface A { String value(); }
