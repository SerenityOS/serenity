/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/vmClasses.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/semaphore.inline.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

// The test doesn't work for PRODUCT because it needs WizardMode
#ifndef PRODUCT
static bool test_pattern(stringStream* st, const char* pattern) {
  return (strstr(st->as_string(), pattern) != NULL);
}

static void assert_test_pattern(Handle object, const char* pattern) {
  stringStream st;
  object->print_on(&st);
  ASSERT_TRUE(test_pattern(&st, pattern)) << pattern << " not in " << st.as_string();
}

class LockerThread : public JavaTestThread {
  oop _obj;
  public:
  LockerThread(Semaphore* post, oop obj) : JavaTestThread(post), _obj(obj) {}
  virtual ~LockerThread() {}

  void main_run() {
    JavaThread* THREAD = JavaThread::current();
    HandleMark hm(THREAD);
    Handle h_obj(THREAD, _obj);
    ResourceMark rm(THREAD);

    // Wait gets the lock inflated.
    // The object will stay locked for the context of 'ol' so the lock will
    // still be inflated after the notify_all() call. Deflation can't happen
    // while an ObjectMonitor is "busy" and being locked is the most "busy"
    // state we have...
    ObjectLocker ol(h_obj, THREAD);
    ol.notify_all(THREAD);
    assert_test_pattern(h_obj, "monitor");
  }
};


TEST_VM(markWord, printing) {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);
  ResourceMark rm(THREAD);

  oop obj = vmClasses::Byte_klass()->allocate_instance(THREAD);

  FlagSetting fs(WizardMode, true);

  HandleMark hm(THREAD);
  Handle h_obj(THREAD, obj);

  // Thread tries to lock it.
  {
    ObjectLocker ol(h_obj, THREAD);
    assert_test_pattern(h_obj, "locked");
  }
  assert_test_pattern(h_obj, "is_neutral no_hash");

  // Hash the object then print it.
  intx hash = h_obj->identity_hash();
  assert_test_pattern(h_obj, "is_neutral hash=0x");

  // Wait gets the lock inflated.
  {
    ObjectLocker ol(h_obj, THREAD);

    Semaphore done(0);
    LockerThread* st;
    st = new LockerThread(&done, h_obj());
    st->doit();

    ol.wait(THREAD);
    assert_test_pattern(h_obj, "monitor");
    done.wait_with_safepoint_check(THREAD);  // wait till the thread is done.
  }
}
#endif // PRODUCT
