/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package typeannos;

import java.lang.annotation.*;

/*
 * This class is replicated from test/tools/javac/annotations/typeAnnotations/newlocations.
 */
class BoundTest {
    void wcExtends(MyList<? extends @WldA String> l) { }
    void wcSuper(MyList<? super @WldA String> l) { }

    MyList<? extends @WldA String> returnWcExtends() { return null; }
    MyList<? super @WldA String> returnWcSuper() { return null; }
    MyList<? extends @WldA MyList<? super @WldB("m") String>> complex() { return null; }
}

class BoundWithValue {
    void wcExtends(MyList<? extends @WldB("m") String> l) { }
    void wcSuper(MyList<? super @WldB("m") String> l) { }

    MyList<? extends @WldB("m") String> returnWcExtends() { return null; }
    MyList<? super @WldB("m") String> returnWcSuper() { return null; }
    MyList<? extends @WldB("m") MyList<? super @WldB("m") String>> complex() { return null; }
}

class SelfTest {
    void wcExtends(MyList<@WldA ?> l) { }
    void wcSuper(MyList<@WldA ?> l) { }

    MyList<@WldA ?> returnWcExtends() { return null; }
    MyList<@WldA ?> returnWcSuper() { return null; }
    MyList<@WldA ? extends @WldA MyList<@WldB("m") ?>> complex() { return null; }
}

class SelfWithValue {
    void wcExtends(MyList<@WldB("m") ?> l) { }
    void wcSuper(MyList<@WldB("m") ?> l) { }

    MyList<@WldB("m") ?> returnWcExtends() { return null; }
    MyList<@WldB("m") ?> returnWcSuper() { return null; }
    MyList<@WldB("m") ? extends MyList<@WldB("m") ? super String>> complex() { return null; }
}

class MyList<K> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface WldA { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface WldB { String value(); }
