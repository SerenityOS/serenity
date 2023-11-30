/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <elf.h>
#include <limits.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

struct dl_phdr_info {
    Elf_Addr dlpi_addr;
    char const* dlpi_name;
    Elf_Phdr const* dlpi_phdr;
    Elf_Half dlpi_phnum;
};

int dl_iterate_phdr(int (*callback)(struct dl_phdr_info* info, size_t size, void* data), void* data);

__END_DECLS
