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
 *
 */

#ifndef SHARE_GC_SHARED_GCCONFIG_HPP
#define SHARE_GC_SHARED_GCCONFIG_HPP

#include "gc/shared/collectedHeap.hpp"
#include "memory/allocation.hpp"

class GCArguments;

class GCConfig : public AllStatic {
private:
  static GCArguments* _arguments;
  static bool         _gc_selected_ergonomically;

  static void fail_if_non_included_gc_is_selected();
  static bool is_no_gc_selected();
  static bool is_exactly_one_gc_selected();

  static void select_gc_ergonomically();
  static GCArguments* select_gc();

public:
  static void initialize();

  static bool is_gc_supported(CollectedHeap::Name name);
  static bool is_gc_selected(CollectedHeap::Name name);
  static bool is_gc_selected_ergonomically();

  static const char* hs_err_name();
  static const char* hs_err_name(CollectedHeap::Name name);

  static GCArguments* arguments();
};

#endif // SHARE_GC_SHARED_GCCONFIG_HPP
