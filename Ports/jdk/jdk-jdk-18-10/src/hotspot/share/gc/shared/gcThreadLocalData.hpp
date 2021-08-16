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
 */

#ifndef SHARE_GC_SHARED_GCTHREADLOCALDATA_HPP
#define SHARE_GC_SHARED_GCTHREADLOCALDATA_HPP

#include "utilities/globalDefinitions.hpp"

// Thread local data area for GC-specific information. Each GC
// is free to decide the internal structure and contents of this
// area. It is represented as a 64-bit aligned opaque blob to
// avoid circular dependencies between Thread and all GCs. For
// the same reason, the size of the data area is hard coded to
// provide enough space for all current GCs. Adjust the size if
// needed, but avoid making it excessively large as it adds to
// the memory overhead of creating a thread.
//
// Use Thread::gc_data<T>() to access the data, where T is the
// GC-specific type describing the structure of the data. GCs
// should consider placing frequently accessed fields first in
// T, so that field offsets relative to Thread are small, which
// often allows for a more compact instruction encoding.
typedef uint64_t GCThreadLocalData[19]; // 152 bytes

#endif // SHARE_GC_SHARED_GCTHREADLOCALDATA_HPP
