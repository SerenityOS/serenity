/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef __serenity__
#    include <LibC/elf.h>
#else
#    include <elf.h>
#endif
#include <sys/cdefs.h>

__BEGIN_DECLS

#define ElfW(type) Elf32_##type

struct dl_phdr_info {
    Elf32_Addr dlpi_addr;
    const char* dlpi_name;
    const Elf32_Phdr* dlpi_phdr;
    Elf32_Half dlpi_phnum;
};

int dl_iterate_phdr(int (*callback)(struct dl_phdr_info* info, size_t size, void* data), void* data);

__END_DECLS
