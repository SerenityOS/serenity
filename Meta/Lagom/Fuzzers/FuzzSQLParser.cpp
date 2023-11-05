/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Parser.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto parser = SQL::AST::Parser(SQL::AST::Lexer({ data, size }));
    [[maybe_unused]] auto statement = parser.next_statement();
    return 0;
}
