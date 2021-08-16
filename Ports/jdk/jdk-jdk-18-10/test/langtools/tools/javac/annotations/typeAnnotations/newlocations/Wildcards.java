/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;

/*
 * @test
 * @bug 6843077 8006775
 * @summary new type annotation location: wildcard bound
 * @author Mahmood Ali
 * @compile Wildcards.java
 */
class BoundTest {
  void wcExtends(MyList<? extends @A String> l) { }
  void wcSuper(MyList<? super @A String> l) { }

  MyList<? extends @A String> returnWcExtends() { return null; }
  MyList<? super @A String> returnWcSuper() { return null; }
  MyList<? extends @A MyList<? super @B("m") String>> complex() { return null; }
}

class BoundWithValue {
  void wcExtends(MyList<? extends @B("m") String> l) { }
  void wcSuper(MyList<? super @B(value="m") String> l) { }

  MyList<? extends @B("m") String> returnWcExtends() { return null; }
  MyList<? super @B(value="m") String> returnWcSuper() { return null; }
  MyList<? extends @B("m") MyList<? super @B("m") String>> complex() { return null; }
}

class SelfTest {
  void wcExtends(MyList<@A ?> l) { }
  void wcSuper(MyList<@A ?> l) { }

  MyList<@A ?> returnWcExtends() { return null; }
  MyList<@A ?> returnWcSuper() { return null; }
  MyList<@A ? extends @A MyList<@B("m") ?>> complex() { return null; }
}

class SelfWithValue {
  void wcExtends(MyList<@B("m") ?> l) { }
  void wcSuper(MyList<@B(value="m") ?> l) { }

  MyList<@B("m") ?> returnWcExtends() { return null; }
  MyList<@B(value="m") ?> returnWcSuper() { return null; }
  MyList<@B("m") ? extends MyList<@B("m") ? super String>> complex() { return null; }
}

class MyList<K> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { String value(); }
