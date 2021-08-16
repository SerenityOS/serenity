/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZVERIFY_HPP
#define SHARE_GC_Z_ZVERIFY_HPP

#include "memory/allocation.hpp"

class frame;
class ZPageAllocator;

class ZVerify : public AllStatic {
private:
  static void roots_strong(bool verify_fixed);
  static void roots_weak();

  static void objects(bool verify_weaks);

public:
  static void before_zoperation();
  static void after_mark();
  static void after_weak_processing();

  static void verify_thread_head_bad(JavaThread* thread) NOT_DEBUG_RETURN;
  static void verify_thread_frames_bad(JavaThread* thread) NOT_DEBUG_RETURN;
  static void verify_frame_bad(const frame& fr, RegisterMap& register_map) NOT_DEBUG_RETURN;
};

class ZVerifyViewsFlip {
private:
  const ZPageAllocator* const _allocator;

public:
  ZVerifyViewsFlip(const ZPageAllocator* allocator);
  ~ZVerifyViewsFlip();
};

#endif // SHARE_GC_Z_ZVERIFY_HPP
