/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/timerTrace.hpp"
#include "utilities/ostream.hpp"

TraceTime::TraceTime(const char* title,
                     bool doit) {
  _active   = doit;
  _verbose  = true;
  _title    = title;
  _print    = NULL;

  if (_active) {
    _accum = NULL;
    _t.start();
  }
}

TraceTime::TraceTime(const char* title,
                     elapsedTimer* accumulator,
                     bool doit,
                     bool verbose) {
  _active   = doit;
  _verbose  = verbose;
  _title    = title;
  _print    = NULL;

  if (_active) {
    _accum = accumulator;
    _t.start();
  }
}

TraceTime::TraceTime(const char* title,
                     TraceTimerLogPrintFunc ttlpf) {
  _active   = ttlpf!= NULL;
  _verbose  = true;
  _title    = title;
  _print    = ttlpf;

  if (_active) {
    _accum = NULL;
    _t.start();
  }
}

TraceTime::~TraceTime() {
  if (!_active) {
    return;
  }
  _t.stop();
  if (_accum != NULL) {
    _accum->add(_t);
  }
  if (!_verbose) {
    return;
  }
  if (_print) {
    _print("%s, %3.7f secs", _title, _t.seconds());
  } else {
    tty->print_cr("[%s, %3.7f secs]", _title, _t.seconds());
    tty->flush();
  }
}

