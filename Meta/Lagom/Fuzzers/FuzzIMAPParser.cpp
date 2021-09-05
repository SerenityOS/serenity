/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIMAP/Parser.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto parser = IMAP::Parser();
    parser.parse(ByteBuffer::copy(data, size).release_value(), true);
    parser.parse(ByteBuffer::copy(data, size).release_value(), false);
    return 0;
}
