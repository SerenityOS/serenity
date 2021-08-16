/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8195650
 * @summary Test linking of method references to VarHandle access methods.
 * @run testng VarHandleMethodReferenceTest
 */

import org.testng.annotations.Test;

import java.lang.invoke.*;

public class VarHandleMethodReferenceTest {

  interface R {
      void apply();
  }

  @Test
  public void testMethodReferences() {
      VarHandle vh = MethodHandles.arrayElementVarHandle(int[].class);

      // The compilation of these method references will result in the
      // placement of MethodHandles in the constant pool that reference
      // VarHandle signature polymorphic methods.
      // When those constant method handles are loaded the VarHandle invoker
      // mechanism will be used where the first argument to invocation will be
      // the bound VarHandle instance

      // VarHandle invokers are tested by other test classes so here it
      // is just necessary to check that functional objects can be successfully
      // obtained, it does not matter about the signature of the functional
      // interface

      R r;
      r = vh::get;
      r = vh::set;
      r = vh::getVolatile;
      r = vh::setVolatile;
      r = vh::getOpaque;
      r = vh::setOpaque;
      r = vh::getAcquire;
      r = vh::setRelease;

      r = vh::compareAndSet;
      r = vh::compareAndExchange;
      r = vh::compareAndExchangeAcquire;
      r = vh::compareAndExchangeRelease;
      r = vh::weakCompareAndSetPlain;
      r = vh::weakCompareAndSet;
      r = vh::weakCompareAndSetAcquire;
      r = vh::weakCompareAndSetRelease;

      r = vh::getAndSet;
      r = vh::getAndSetAcquire;
      r = vh::getAndSetRelease;
      r = vh::getAndAdd;
      r = vh::getAndAddAcquire;
      r = vh::getAndAddRelease;
      r = vh::getAndBitwiseOr;
      r = vh::getAndBitwiseOrAcquire;
      r = vh::getAndBitwiseOrRelease;
      r = vh::getAndBitwiseAnd;
      r = vh::getAndBitwiseAndAcquire;
      r = vh::getAndBitwiseAndRelease;
      r = vh::getAndBitwiseXor;
      r = vh::getAndBitwiseXorAcquire;
      r = vh::getAndBitwiseXorRelease;
  }
}
