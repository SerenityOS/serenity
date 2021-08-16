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

#include "precompiled.hpp"
#include "classfile/symbolTable.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/signature.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

TEST_VM(SignatureStream, check_refcount) {

  JavaThread* THREAD = JavaThread::current();
  // the thread should be in vm to use locks
  ThreadInVMfromNative ThreadInVMfromNative(THREAD);
  // SignatureStream::as_symbol will allocate on the resource area
  ResourceMark rm(THREAD);

  Symbol* foo = SymbolTable::new_symbol("Foo");
  int r1 = foo->refcount();

  {
    // Trivial test: non-method signature of a non-permanent symbol
    Symbol* methodSig = SymbolTable::new_symbol("LFoo;");
    SignatureStream ss(methodSig, false);
    Symbol* sym = ss.as_symbol();
    ASSERT_EQ(sym, foo) << "found symbol should be Foo: " << sym->as_C_string();
    // This should mean the SS looks up and increments refcount to Foo
    ASSERT_EQ(foo->refcount(), r1 + 1) << "refcount should be incremented";

    ASSERT_TRUE(!ss.is_done())
      << "stream parsing should not be marked as done until"
      <<" ss.next() called after the last symbol";

    ss.next();
    ASSERT_TRUE(ss.is_done()) << "stream parsing should be marked as done";
  }

  ASSERT_EQ(foo->refcount(), r1) << "refcount should have decremented";

  {
    // Ensure refcount is properly decremented when first symbol is non-permanent and second isn't

    Symbol* integer = SymbolTable::new_symbol("java/lang/Integer");
    ASSERT_TRUE(integer->is_permanent()) << "java/lang/Integer must be permanent";

    Symbol* methodSig = SymbolTable::new_symbol("(LFoo;)Ljava/lang/Integer;");
    SignatureStream ss(methodSig);
    Symbol* sym = ss.as_symbol();
    ASSERT_EQ(sym, foo) << "found symbol should be Foo: " << sym->as_C_string();
    // This should mean the SS looks up and increments refcount to Foo
    ASSERT_EQ(foo->refcount(), r1 + 1) << "refcount should be incremented";

    ss.next();
    sym = ss.as_symbol();
    ASSERT_EQ(sym, integer) << "found symbol should be java/lang/Integer";

    ASSERT_TRUE(!ss.is_done())
      << "stream parsing should not be marked as done until"
      <<" ss.next() called after the last symbol";

    ss.next();
    ASSERT_TRUE(ss.is_done()) << "stream parsing should be marked as done";
  }

  ASSERT_EQ(foo->refcount(), r1) << "refcount should have decremented";

}
