/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package pkg7;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// Adding documented annotations anywhere in the signature of an overriding
// method should cause it to be included in the details section even with
// --override-methods=summary option.

interface AnnotatedBase {
    /**
     * This is AnnotatedBase::m1.
     * @param p1 first parameter
     * @param p2 second parameter
     * @return something
     */
    public Iterable<String> m1(Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub1 extends AnnotatedBase {
    @Override
    public Iterable<String> m1(Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub2 extends AnnotatedBase {
    @Override
    @A
    public Iterable<String> m1(Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub3 extends AnnotatedBase {
    @Override
    public @A Iterable<String> m1(Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub4 extends AnnotatedBase {
    @Override
    public Iterable<@A String> m1(Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub5 extends AnnotatedBase {
    @Override
    public Iterable<String> m1(@A Class<? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub6 extends AnnotatedBase {
    @Override
    public Iterable<String> m1(Class<@A ? extends CharSequence> p1, int[] p2);
}

interface AnnotatedSub7 extends AnnotatedBase {
    @Override
    public Iterable<String> m1(Class<? extends @A CharSequence> p1, int[] p2);
}

interface AnnotatedSub8 extends AnnotatedBase {
    @Override
    public Iterable<String> m1(Class<? extends CharSequence> p1, int @A [] p2);
}

@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface A {}
