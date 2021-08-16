/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRSIGNAL_HPP
#define SHARE_JFR_UTILITIES_JFRSIGNAL_HPP

#include "runtime/atomic.hpp"

class JfrSignal {
 private:
  mutable volatile bool _signaled;
 public:
  JfrSignal() : _signaled(false) {}

  void signal() const {
    Atomic::release_store(&_signaled, true);
  }

  bool is_signaled() const {
    return Atomic::load_acquire(&_signaled);
  }

  bool is_signaled_with_reset() const {
    if (is_signaled()) {
      Atomic::release_store(&_signaled, false);
      return true;
    }
    return false;
  }

  address signaled_address() { return (address)&_signaled; }
};

#endif // SHARE_JFR_UTILITIES_JFRSIGNAL_HPP
