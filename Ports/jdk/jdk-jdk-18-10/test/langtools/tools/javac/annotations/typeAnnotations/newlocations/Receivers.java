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
 * @summary new type annotation location: receivers
 * @author Mahmood Ali, Werner Dietl
 * @compile Receivers.java
 */
class DefaultUnmodified {
  void plain(@A DefaultUnmodified this) { }
  <T> void generic(@A DefaultUnmodified this) { }
  void withException(@A DefaultUnmodified this) throws Exception { }
  String nonVoid(@A DefaultUnmodified this) { return null; }
  <T extends Runnable> void accept(@A DefaultUnmodified this, T r) throws Exception { }
}

class PublicModified {
  public final void plain(@A PublicModified this) { }
  public final <T> void generic(@A PublicModified this) { }
  public final void withException(@A PublicModified this) throws Exception { }
  public final String nonVoid(@A PublicModified this) { return null; }
  public final <T extends Runnable> void accept(@A PublicModified this, T r) throws Exception { }
}

class WithValue {
  void plain(@B("m") WithValue this) { }
  <T> void generic(@B("m") WithValue this) { }
  void withException(@B("m") WithValue this) throws Exception { }
  String nonVoid(@B("m") WithValue this) { return null; }
  <T extends Runnable> void accept(@B("m") WithValue this, T r) throws Exception { }
}

class WithBody {
  Object f;

  void field(@A WithBody this) {
    this.f = null;
  }
  void meth(@A WithBody this) {
    this.toString();
  }
}

class Generic1<X> {
  void test1(Generic1<X> this) {}
  void test2(@A Generic1<X> this) {}
  void test3(Generic1<@A X> this) {}
  void test4(@A Generic1<@A X> this) {}
}

class Generic2<@A X> {
  void test1(Generic2<X> this) {}
  void test2(@A Generic2<X> this) {}
  void test3(Generic2<@A X> this) {}
  void test4(@A Generic2<@A X> this) {}
}

class Generic3<X extends @A Object> {
  void test1(Generic3<X> this) {}
  void test2(@A Generic3<X> this) {}
  void test3(Generic3<@A X> this) {}
  void test4(@A Generic3<@A X> this) {}
}

class Generic4<X extends @A Object> {
  <Y> void test1(Generic4<X> this) {}
  <Y> void test2(@A Generic4<X> this) {}
  <Y> void test3(Generic4<@A X> this) {}
  <Y> void test4(@A Generic4<@A X> this) {}
}

class Outer {
  class Inner {
    void none(Outer.Inner this) {}
    void outer(@A Outer.Inner this) {}
    void inner(Outer. @B("i") Inner this) {}
    void both(@A Outer.@B("i") Inner this) {}

    void innerOnlyNone(Inner this) {}
    void innerOnly(@A Inner this) {}
  }
}

class GenericOuter<S, T> {
  class GenericInner<U, V> {
    void none(GenericOuter<S, T>.GenericInner<U, V> this) {}
    void outer(@A GenericOuter<S, T>.GenericInner<U, V> this) {}
    void inner(GenericOuter<S, T>. @B("i") GenericInner<U, V> this) {}
    void both(@A GenericOuter<S, T>.@B("i") GenericInner<U, V> this) {}

    void innerOnlyNone(GenericInner<U, V> this) {}
    void innerOnly(@A GenericInner<U, V> this) {}
  }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { String value(); }
