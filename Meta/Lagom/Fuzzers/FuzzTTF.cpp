/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/TrueTypeFont/Font.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    (void)TTF::Font::try_load_from_externally_owned_memory({ data, size });
    return 0;
}
