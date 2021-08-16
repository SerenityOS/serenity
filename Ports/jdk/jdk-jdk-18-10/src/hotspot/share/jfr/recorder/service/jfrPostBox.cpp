/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "jfr/recorder/service/jfrPostBox.hpp"
#include "jfr/utilities/jfrTryLock.hpp"
#include "runtime/atomic.hpp"
#include "runtime/thread.inline.hpp"

#define MSG_IS_SYNCHRONOUS ( (MSGBIT(MSG_ROTATE)) |          \
                             (MSGBIT(MSG_STOP))   |          \
                             (MSGBIT(MSG_START))  |          \
                             (MSGBIT(MSG_CLONE_IN_MEMORY)) | \
                             (MSGBIT(MSG_VM_ERROR))        | \
                             (MSGBIT(MSG_FLUSHPOINT))        \
                           )

static JfrPostBox* _instance = NULL;

JfrPostBox& JfrPostBox::instance() {
  return *_instance;
}

JfrPostBox* JfrPostBox::create() {
  assert(_instance == NULL, "invariant");
  _instance = new JfrPostBox();
  return _instance;
}

void JfrPostBox::destroy() {
  assert(_instance != NULL, "invariant");
  delete _instance;
  _instance = NULL;
}

JfrPostBox::JfrPostBox() :
  _msg_read_serial(0),
  _msg_handled_serial(0),
  _messages(0),
  _has_waiters(false) {}

static bool is_thread_lock_aversive() {
  Thread* const thread = Thread::current();
  return (thread->is_Java_thread() && JavaThread::cast(thread)->thread_state() != _thread_in_vm) || thread->is_VM_thread();
}

static bool is_synchronous(int messages) {
  return ((messages & MSG_IS_SYNCHRONOUS) != 0);
}

void JfrPostBox::post(JFR_Msg msg) {
  const int the_message = MSGBIT(msg);
  if (is_thread_lock_aversive()) {
    deposit(the_message);
    return;
  }
  if (!is_synchronous(the_message)) {
    asynchronous_post(the_message);
    return;
  }
  synchronous_post(the_message);
}

void JfrPostBox::deposit(int new_messages) {
  while (true) {
    const int current_msgs = Atomic::load(&_messages);
    // OR the new message
    const int exchange_value = current_msgs | new_messages;
    const int result = Atomic::cmpxchg(&_messages, current_msgs, exchange_value);
    if (result == current_msgs) {
      return;
    }
    /* Some other thread just set exactly what this thread wanted */
    if ((result & new_messages) == new_messages) {
      return;
    }
  }
}

void JfrPostBox::asynchronous_post(int msg) {
  assert(!is_synchronous(msg), "invariant");
  deposit(msg);
  JfrMonitorTryLock try_msg_lock(JfrMsg_lock);
  if (try_msg_lock.acquired()) {
    JfrMsg_lock->notify_all();
  }
}

void JfrPostBox::synchronous_post(int msg) {
  assert(is_synchronous(msg), "invariant");
  assert(!JfrMsg_lock->owned_by_self(), "should not hold JfrMsg_lock here!");
  MonitorLocker msg_lock(JfrMsg_lock);
  deposit(msg);
  // serial_id is used to check when what we send in has been processed.
  // _msg_read_serial is read under JfrMsg_lock protection.
  const uintptr_t serial_id = Atomic::load(&_msg_read_serial) + 1;
  msg_lock.notify_all();
  while (!is_message_processed(serial_id)) {
    msg_lock.wait();
  }
}

/*
 * Check if a synchronous message has been processed.
 * We avoid racing on _msg_handled_serial by ensuring
 * that we are holding the JfrMsg_lock when checking
 * completion status.
 */
bool JfrPostBox::is_message_processed(uintptr_t serial_id) const {
  assert(JfrMsg_lock->owned_by_self(), "_msg_handled_serial must be read under JfrMsg_lock protection");
  return serial_id <= Atomic::load(&_msg_handled_serial);
}

bool JfrPostBox::is_empty() const {
  assert(JfrMsg_lock->owned_by_self(), "not holding JfrMsg_lock!");
  return Atomic::load(&_messages) == 0;
}

int JfrPostBox::collect() {
  // get pending and reset to 0
  const int messages = Atomic::xchg(&_messages, 0);
  if (check_waiters(messages)) {
    _has_waiters = true;
    assert(JfrMsg_lock->owned_by_self(), "incrementing _msg_read_serial is protected by JfrMsg_lock");
    // Update made visible on release of JfrMsg_lock via fence instruction in Monitor::IUnlock.
    ++_msg_read_serial;
  }
  return messages;
}

bool JfrPostBox::check_waiters(int messages) const {
  assert(JfrMsg_lock->owned_by_self(), "not holding JfrMsg_lock!");
  assert(!_has_waiters, "invariant");
  return is_synchronous(messages);
}

void JfrPostBox::notify_waiters() {
  if (!_has_waiters) {
    return;
  }
  _has_waiters = false;
  assert(JfrMsg_lock->owned_by_self(), "incrementing _msg_handled_serial is protected by JfrMsg_lock.");
  // Update made visible on release of JfrMsg_lock via fence instruction in Monitor::IUnlock.
  ++_msg_handled_serial;
  JfrMsg_lock->notify();
}

// safeguard to ensure no threads are left waiting
void JfrPostBox::notify_collection_stop() {
  MutexLocker msg_lock(JfrMsg_lock);
  JfrMsg_lock->notify_all();
}
