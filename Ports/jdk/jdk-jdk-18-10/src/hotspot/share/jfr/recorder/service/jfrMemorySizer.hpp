/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_SERVICE_JFRMEMORYSIZER_HPP
#define SHARE_JFR_RECORDER_SERVICE_JFRMEMORYSIZER_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

extern const julong MIN_BUFFER_COUNT;
extern const julong MIN_GLOBAL_BUFFER_SIZE;
extern const julong MIN_MEMORY_SIZE;
extern const julong MIN_THREAD_BUFFER_SIZE;
extern const julong MAX_GLOBAL_BUFFER_SIZE;
extern const julong MAX_THREAD_BUFFER_SIZE;

struct JfrMemoryOptions {
  julong memory_size;
  julong global_buffer_size;
  julong buffer_count;
  julong thread_buffer_size;
  bool memory_size_configured;
  bool global_buffer_size_configured;
  bool buffer_count_configured;
  bool thread_buffer_size_configured;
};

//
// Encapsulates sizing of memory options
// The options parameter is modified with updated values.
//
class JfrMemorySizer : AllStatic {
 public:
  static bool adjust_options(JfrMemoryOptions* options);
};

#endif // SHARE_JFR_RECORDER_SERVICE_JFRMEMORYSIZER_HPP
