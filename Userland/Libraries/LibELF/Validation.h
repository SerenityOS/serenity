/*
 * Copyright (c) 2020, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>
#include <LibELF/ELFABI.h>

namespace ELF {

bool validate_elf_header(Elf_Ehdr const& elf_header, size_t file_size, bool verbose = true);
ErrorOr<bool> validate_program_headers(Elf_Ehdr const& elf_header, size_t file_size, ReadonlyBytes buffer, StringBuilder* interpreter_path_builder = nullptr, Optional<size_t>* requested_stack_size = nullptr, bool verbose = true);

} // end namespace ELF
