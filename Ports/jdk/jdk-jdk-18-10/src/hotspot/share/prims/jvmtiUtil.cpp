/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
#include "prims/jvmtiUtil.hpp"
#include "runtime/handles.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "utilities/exceptions.hpp"

//
// class JvmtiUtil
//

ResourceArea* JvmtiUtil::_single_threaded_resource_area = NULL;

ResourceArea* JvmtiUtil::single_threaded_resource_area() {
  if (_single_threaded_resource_area == NULL) {
    // lazily create the single threaded resource area
    // pick a size which is not a standard since the pools don't exist yet
    _single_threaded_resource_area = new (mtInternal) ResourceArea(Chunk::non_pool_size);
  }
  return _single_threaded_resource_area;
}
