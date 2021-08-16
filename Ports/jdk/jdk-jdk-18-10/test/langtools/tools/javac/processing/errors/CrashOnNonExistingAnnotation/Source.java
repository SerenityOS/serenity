/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test /nodynamiccopyright/
 * @bug 8027310
 * @summary Ensure no exceptions on unresolvable annotations
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build Processor
 * @run main Processor Source.java
 */

import java.util.List;

@Anno("TYPE")
public class Source {
    @Anno("TYPE")
    class Inner {
        class InnerInner {
            public @Anno("CONSTRUCTOR") InnerInner(@Anno("TYPE_USE") Source. @Anno("TYPE_USE") Inner Inner.this,
                                                   @Anno("PARAMETER") java.lang. @Anno("TYPE_USE") Runnable p) {
                Runnable r = () -> {
                    @Anno("TYPE_USE") Object tested = null;
                    @Anno("TYPE_USE") boolean isAnnotated = tested instanceof @Anno("TYPE_USE") String;
                };

                @Anno("TYPE_USE") Object tested = (@Anno("TYPE_USE") String @Anno("TYPE_USE") []) null;
                @Anno("TYPE_USE") boolean isAnnotated = tested instanceof@Anno("TYPE_USE") String;

                tested = new java.lang. @Anno("TYPE_USE") Object();
                tested = new @Anno("TYPE_USE") Object();
            }
        }
    }

    {
        Runnable r = () -> {
            @Anno("TYPE_USE") Object tested = null;
            @Anno("TYPE_USE") boolean isAnnotated = tested instanceof @Anno("TYPE_USE") String;
        };

        @Anno("TYPE_USE") Object tested = (@Anno("TYPE_USE") String @Anno("TYPE_USE") []) null;
        @Anno("TYPE_USE") boolean isAnnotated = tested instanceof@Anno("TYPE_USE") String;

        tested = new java.lang. @Anno("TYPE_USE") Object();
        tested = new @Anno("TYPE_USE") Object();
    }

    @Anno("TYPE")
    @Anno("ANNOTATION_TYPE")
    @interface A { }
    abstract class Parameterized<@Anno("TYPE_PARAMETER") T extends @Anno("TYPE_USE") CharSequence &
                                                                   @Anno("TYPE_USE") Runnable>
        implements @Anno("TYPE_USE") List<@Anno("TYPE_USE") Runnable> { }
}
