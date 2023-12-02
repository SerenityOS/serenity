/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Span.h>
#include <AK/StringView.h>

namespace JIT::GDB {

void register_into_gdb(ReadonlyBytes data);
void unregister_from_gdb(ReadonlyBytes data);

// Build a GDB compatible image to register with the GDB JIT Interface.
// Returns Optional since the platform may not be supported.
// The `code` must be the region of memory that will be executed, since the
// image will hold direct references to addresses within the code. This way GDB
// will be able to identify the code region and insert breakpoints into it.
// Both `file_symbol_name` and `code_symbol_name` will end up in the symbol
// table of the image. They represent a file name for the image and a name for
// the region of code that is being executed.
Optional<FixedArray<u8>> build_gdb_image(ReadonlyBytes code, StringView file_symbol_name, StringView code_symbol_name);

}
