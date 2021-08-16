/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_SERVICE_JFRPOSTBOX_HPP
#define SHARE_JFR_RECORDER_SERVICE_JFRPOSTBOX_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class JavaThread;

#define MSGBIT(e) (1<<(e))

enum JFR_Msg {
  MSG_ALL_MSGS = -1,
  MSG_CLONE_IN_MEMORY = 0,
  MSG_START,
  MSG_STOP,
  MSG_ROTATE,
  MSG_FULLBUFFER,
  MSG_CHECKPOINT,
  MSG_WAKEUP,
  MSG_SHUTDOWN,
  MSG_VM_ERROR,
  MSG_FLUSHPOINT,
  MSG_NO_OF_MSGS
};

/**
 *  Jfr messaging.
 *
 *  Synchronous messages (posting thread waits for message completion):
 *
 *  MSG_CLONE_IN_MEMORY (0) ; MSGBIT(MSG_CLONE_IN_MEMORY) == (1 << 0) == 0x1
 *  MSG_START(1)            ; MSGBIT(MSG_START) == (1 << 0x1) == 0x2
 *  MSG_STOP (2)            ; MSGBIT(MSG_STOP) == (1 << 0x2) == 0x4
 *  MSG_ROTATE (3)          ; MSGBIT(MSG_ROTATE) == (1 << 0x3) == 0x8
 *  MSG_VM_ERROR (8)        ; MSGBIT(MSG_VM_ERROR) == (1 << 0x8) == 0x100
 *  MSG_FLUSHPOINT (9)     ; MSGBIT(MSG_FLUSHPOINT) == (1 << 0x9) == 0x200
 *
 *  Asynchronous messages (posting thread returns immediately upon deposit):
 *
 *  MSG_FULLBUFFER (4)      ; MSGBIT(MSG_FULLBUFFER) == (1 << 0x4) == 0x10
 *  MSG_CHECKPOINT (5)      ; MSGBIT(CHECKPOINT) == (1 << 0x5) == 0x20
 *  MSG_WAKEUP (6)          ; MSGBIT(WAKEUP) == (1 << 0x6) == 0x40
 *  MSG_SHUTDOWN (7)        ; MSGBIT(MSG_SHUTDOWN) == (1 << 0x7) == 0x80
 */

class JfrPostBox : public JfrCHeapObj {
  friend class JfrRecorder;
 public:
  void post(JFR_Msg msg);

 private:
  uintptr_t _msg_read_serial;
  uintptr_t _msg_handled_serial;
  volatile int _messages;
  bool _has_waiters;

  JfrPostBox();
  static JfrPostBox& instance();
  static JfrPostBox* create();
  static void destroy();

  void asynchronous_post(int msg);
  void synchronous_post(int msg);
  void deposit(int new_messages);
  bool is_message_processed(uintptr_t serial_id) const;

  friend void recorderthread_entry(JavaThread*, JavaThread*);
  // for the friend declaration above
  bool is_empty() const;
  int collect();
  bool check_waiters(int messages) const;
  void notify_waiters();
  void notify_collection_stop();
};

#endif // SHARE_JFR_RECORDER_SERVICE_JFRPOSTBOX_HPP
