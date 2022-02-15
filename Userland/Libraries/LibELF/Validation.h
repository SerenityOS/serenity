/*
 * Copyright (c) 2020, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>
#include <LibC/elf.h>
#include <limits.h>

namespace ELF {

bool validate_elf_header(ElfW(Ehdr) const& elf_header, size_t file_size, bool verbose = true);
ErrorOr<bool> validate_program_headers(ElfW(Ehdr) const& elf_header, size_t file_size, ReadonlyBytes buffer, StringBuilder* interpreter_path_builder, bool verbose = true);

} // end namespace ELF
