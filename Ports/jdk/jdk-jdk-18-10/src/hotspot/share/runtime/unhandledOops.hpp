/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_UNHANDLEDOOPS_HPP
#define SHARE_RUNTIME_UNHANDLEDOOPS_HPP

#ifdef CHECK_UNHANDLED_OOPS

// Detect unhanded oops in VM code

// The design is that when an oop is declared on the stack as a local
// variable, the oop is actually a C++ struct with constructor and
// destructor.  The constructor adds the oop address on a list
// off each thread and the destructor removes the oop.  At a potential
// safepoint, the stack addresses of the local variable oops are trashed
// with a recognizable value.  If the local variable is used again, it
// will segfault, indicating an unsafe use of that oop.
// eg:
//    oop o;    //register &o on list
//    funct();  // if potential safepoint - causes clear_naked_oops()
//              // which trashes o above.
//    o->do_something();  // Crashes because o is unsafe.
//
// This code implements the details of the unhandled oop list on the thread.
//

class oop;
class Thread;

class UnhandledOopEntry : public CHeapObj<mtThread> {
 friend class UnhandledOops;
 private:
  oop* _oop_ptr;
  bool _ok_for_gc;
 public:
  oop* oop_ptr() { return _oop_ptr; }
  UnhandledOopEntry() : _oop_ptr(NULL), _ok_for_gc(false) {}
  UnhandledOopEntry(oop* op) :
                        _oop_ptr(op),   _ok_for_gc(false) {}
};


class UnhandledOops : public CHeapObj<mtThread> {
 friend class Thread;
 private:
  Thread* _thread;
  int _level;
  GrowableArray<UnhandledOopEntry> *_oop_list;
  void allow_unhandled_oop(oop* op);
  void clear_unhandled_oops();
  UnhandledOops(Thread* thread);
  ~UnhandledOops();

 public:
  static void dump_oops(UnhandledOops* list);
  void register_unhandled_oop(oop* op);
  void unregister_unhandled_oop(oop* op);
};

#ifdef _LP64
const intptr_t BAD_OOP_ADDR =  0xfffffffffffffff1;
#else
const intptr_t BAD_OOP_ADDR =  0xfffffff1;
#endif // _LP64
#endif // CHECK_UNHANDLED_OOPS

#endif // SHARE_RUNTIME_UNHANDLEDOOPS_HPP
