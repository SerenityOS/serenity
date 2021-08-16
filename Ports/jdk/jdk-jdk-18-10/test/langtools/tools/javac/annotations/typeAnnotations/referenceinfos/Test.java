/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.lang.annotation.*;

class Test<K> { GOuter<@TC Object, String> entrySet() { return null; } }

@interface A {}
@interface B {}
@interface C {}
@interface D {}
@interface E {}
@interface F {}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TA {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TB {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TC {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TD {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TE {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TF {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TG {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TH {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TI {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TJ {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TK {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TL {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface TM {}

@Repeatable(RTAs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface RTA {}
@Repeatable(RTBs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface RTB {}
@ContainerFor(RTA.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface RTAs { RTA[] value(); }
@ContainerFor(RTB.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface RTBs { RTB[] value(); }
@Target(value={ElementType.TYPE,ElementType.FIELD,ElementType.METHOD,ElementType.PARAMETER,ElementType.CONSTRUCTOR,ElementType.LOCAL_VARIABLE})
@interface Decl {}
