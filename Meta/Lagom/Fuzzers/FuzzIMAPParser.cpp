/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIMAP/Parser.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto parser = IMAP::Parser();
    parser.parse(ByteBuffer::copy(data, size).release_value(), true);
    parser.parse(ByteBuffer::copy(data, size).release_value(), false);
    return 0;
}
