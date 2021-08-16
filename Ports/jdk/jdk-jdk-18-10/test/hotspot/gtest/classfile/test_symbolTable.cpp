/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/interfaceSupport.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

TEST_VM(SymbolTable, temp_new_symbol) {
  // Assert messages assume these symbols are unique, and the refcounts start at
  // one, but code does not rely on this.
  JavaThread* THREAD = JavaThread::current();
  // the thread should be in vm to use locks
  ThreadInVMfromNative ThreadInVMfromNative(THREAD);

  Symbol* abc = SymbolTable::new_symbol("abc");
  int abccount = abc->refcount();
  TempNewSymbol ss = abc;
  ASSERT_EQ(ss->refcount(), abccount) << "only one abc";
  ASSERT_EQ(ss->refcount(), abc->refcount()) << "should match TempNewSymbol";

  Symbol* efg = SymbolTable::new_symbol("efg");
  Symbol* hij = SymbolTable::new_symbol("hij");
  int efgcount = efg->refcount();
  int hijcount = hij->refcount();

  TempNewSymbol s1 = efg;
  TempNewSymbol s2 = hij;
  ASSERT_EQ(s1->refcount(), efgcount) << "one efg";
  ASSERT_EQ(s2->refcount(), hijcount) << "one hij";

  // Assignment operator
  s1 = s2;
  ASSERT_EQ(hij->refcount(), hijcount + 1) << "should be two hij";
  ASSERT_EQ(efg->refcount(), efgcount - 1) << "should be no efg";

  s1 = ss; // s1 is abc
  ASSERT_EQ(s1->refcount(), abccount + 1) << "should be two abc (s1 and ss)";
  ASSERT_EQ(hij->refcount(), hijcount) << "should only have one hij now (s2)";

  s1 = s1; // self assignment
  ASSERT_EQ(s1->refcount(), abccount + 1) << "should still be two abc (s1 and ss)";

  TempNewSymbol s3;
  Symbol* klm = SymbolTable::new_symbol("klm");
  int klmcount = klm->refcount();
  s3 = klm; // assignment
  ASSERT_EQ(s3->refcount(), klmcount) << "only one klm now";

  Symbol* xyz = SymbolTable::new_symbol("xyz");
  int xyzcount = xyz->refcount();
  { // inner scope
    TempNewSymbol s_inner = xyz;
  }
  ASSERT_EQ(xyz->refcount(), xyzcount - 1)
          << "Should have been decremented by dtor in inner scope";

  // Test overflowing refcount making symbol permanent
  Symbol* bigsym = SymbolTable::new_symbol("bigsym");
  for (int i = 0; i < PERM_REFCOUNT + 100; i++) {
    bigsym->increment_refcount();
  }
  ASSERT_EQ(bigsym->refcount(), PERM_REFCOUNT) << "should not have overflowed";

  // Test that PERM_REFCOUNT is sticky
  for (int i = 0; i < 10; i++) {
    bigsym->decrement_refcount();
  }
  ASSERT_EQ(bigsym->refcount(), PERM_REFCOUNT) << "should be sticky";
}

// TODO: Make two threads one decrementing the refcount and the other trying to increment.
// try_increment_refcount should return false

#define SYM_NAME_LENGTH 30
static char symbol_name[SYM_NAME_LENGTH];

class SymbolThread : public JavaTestThread {
  public:
  SymbolThread(Semaphore* post) : JavaTestThread(post) {}
  virtual ~SymbolThread() {}
  void main_run() {
    for (int i = 0; i < 1000; i++) {
      TempNewSymbol sym = SymbolTable::new_symbol(symbol_name);
      // Create and destroy new symbol
      EXPECT_TRUE(sym->refcount() != 0) << "Symbol refcount unexpectedly zeroed";
    }
  }
};

#define SYM_TEST_THREAD_COUNT 5

class DriverSymbolThread : public JavaTestThread {
public:
  Semaphore _done;
  DriverSymbolThread(Semaphore* post) : JavaTestThread(post) { };
  virtual ~DriverSymbolThread(){}

  void main_run() {
    Semaphore done(0);

    // Find a symbol where there will probably be only one instance.
    for (int i = 0; i < 100; i++) {
       os::snprintf(symbol_name, SYM_NAME_LENGTH, "some_symbol%d", i);
       TempNewSymbol ts = SymbolTable::new_symbol(symbol_name);
       if (ts->refcount() == 1) {
         EXPECT_TRUE(ts->refcount() == 1) << "Symbol is just created";
         break;  // found a unique symbol
       }
    }

    SymbolThread* st[SYM_TEST_THREAD_COUNT];
    for (int i = 0; i < SYM_TEST_THREAD_COUNT; i++) {
      st[i] = new SymbolThread(&done);
      st[i]->doit();
    }

    for (int i = 0; i < SYM_TEST_THREAD_COUNT; i++) {
      done.wait();
    }
  }
};

TEST_VM(SymbolTable, test_symbol_refcount_parallel) {
  mt_test_doer<DriverSymbolThread>();
}
