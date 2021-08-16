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
 *
 */

#ifndef _PS_CORE_COMMON_H_
#define _PS_CORE_COMMON_H_

map_info* core_lookup(struct ps_prochandle *ph, uintptr_t addr);
map_info* add_map_info(struct ps_prochandle* ph, int fd, off_t offset,
                       uintptr_t vaddr, size_t memsz, uint32_t flags);
void core_release(struct ps_prochandle* ph);
bool read_string(struct ps_prochandle* ph, uintptr_t addr, char* buf, size_t size);
bool init_classsharing_workaround(struct ps_prochandle* ph);

#endif // _PS_CORE_COMMON_H_
