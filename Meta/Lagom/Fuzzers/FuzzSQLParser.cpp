/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Lexer.h>
#include <LibSQL/Parser.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto parser = SQL::Parser(SQL::Lexer({ data, size }));
    [[maybe_unused]] auto statement = parser.next_statement();
    return 0;
}
