/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibShell/PosixParser.h>
#include <LibShell/Shell.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto source = StringView(static_cast<unsigned char const*>(data), size);
    Shell::Posix::Parser parser(source);
    (void)parser.parse();
    return 0;
}
