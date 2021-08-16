/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _SALIBELF_H_
#define _SALIBELF_H_

#include <elf.h>
#include "elfmacros.h"
#include "libproc_impl.h"

// read ELF file header.
int read_elf_header(int fd, ELF_EHDR* ehdr);

// is given file descriptor corresponds to an ELF file?
bool is_elf_file(int fd);

// read program header table of an ELF file. caller has to
// free the result pointer after use. NULL on failure.
ELF_PHDR* read_program_header_table(int fd, ELF_EHDR* hdr);

// read section header table of an ELF file. caller has to
// free the result pointer after use. NULL on failure.
ELF_SHDR* read_section_header_table(int fd, ELF_EHDR* hdr);

// read a particular section's data. caller has to free the
// result pointer after use. NULL on failure.
void* read_section_data(int fd, ELF_EHDR* ehdr, ELF_SHDR* shdr);

// find the base address at which the library wants to load itself
uintptr_t find_base_address(int fd, ELF_EHDR* ehdr);
#endif /* _SALIBELF_H_ */
