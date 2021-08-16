/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIEXCEPTIONHANDLER_HPP
#define SHARE_CI_CIEXCEPTIONHANDLER_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciInstanceKlass.hpp"

// ciExceptionHandler
//
// This class represents an exception handler for a method.
class ciExceptionHandler : public ResourceObj {
private:
  friend class ciMethod;

  // The loader to be used for resolving the exception
  // klass.
  ciInstanceKlass* _loading_klass;

  // Handler data.
  int _start;
  int _limit;
  int _handler_bci;
  int _catch_klass_index;

  // The exception klass that this handler catches.
  ciInstanceKlass* _catch_klass;

public:
  ciExceptionHandler(ciInstanceKlass* loading_klass,
                     int start, int limit,
                     int handler_bci, int klass_index) {
    _loading_klass = loading_klass;
    _start  = start;
    _limit  = limit;
    _handler_bci = handler_bci;
    _catch_klass_index = klass_index;
    _catch_klass = NULL;
  }

  int       start()             { return _start; }
  int       limit()             { return _limit; }
  int       handler_bci()       { return _handler_bci; }
  int       catch_klass_index() { return _catch_klass_index; }

  // Get the exception klass that this handler catches.
  ciInstanceKlass* catch_klass();

  bool      is_catch_all() { return catch_klass_index() == 0; }
  bool      is_in_range(int bci) {
    return start() <= bci && bci < limit();
  }
  bool      is_rethrow() { return handler_bci() == -1; }

  void      print();
};

#endif // SHARE_CI_CIEXCEPTIONHANDLER_HPP
