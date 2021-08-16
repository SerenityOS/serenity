/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _ELFMACROS_H_
#define _ELFMACROS_H_

#define ELF_NHDR        Elf_Note

#if defined(_LP64)
#define ELF_EHDR        Elf64_Ehdr
#define ELF_SHDR        Elf64_Shdr
#define ELF_PHDR        Elf64_Phdr
#define ELF_SYM         Elf64_Sym
#define ELF_DYN         Elf64_Dyn
#define ELF_ADDR        Elf64_Addr

#ifndef ELF_ST_TYPE
#define ELF_ST_TYPE     ELF64_ST_TYPE
#endif

#else

#define ELF_EHDR        Elf32_Ehdr
#define ELF_SHDR        Elf32_Shdr
#define ELF_PHDR        Elf32_Phdr
#define ELF_SYM         Elf32_Sym
#define ELF_DYN         Elf32_Dyn
#define ELF_ADDR        Elf32_Addr

#ifndef ELF_ST_TYPE
#define ELF_ST_TYPE     ELF32_ST_TYPE
#endif

#endif


#endif /* _ELFMACROS_H_ */
