/*
 * Copyright (c) 2020, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibC/elf.h>
#include <limits.h>

namespace ELF {

bool validate_elf_header(const ElfW(Ehdr) & elf_header, size_t file_size, bool verbose = true);
ErrorOr<bool> validate_program_headers(const ElfW(Ehdr) & elf_header, size_t file_size, const u8* buffer, size_t buffer_size, StringBuilder* interpreter_path_builder, bool verbose = true);

} // end namespace ELF
