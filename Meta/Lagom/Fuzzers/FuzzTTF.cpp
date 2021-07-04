/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTTF/Font.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    ByteBuffer font_data = ByteBuffer::copy(data, size);
    (void)TTF::Font::try_load_from_memory(font_data);
    return 0;
}
