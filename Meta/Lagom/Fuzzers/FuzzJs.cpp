/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto js = StringView(static_cast<const unsigned char*>(data), size);
    auto lexer = JS::Lexer(js);
    auto parser = JS::Parser(lexer);
    auto program = parser.parse_program();
    if (!parser.has_errors()) {
        auto vm = JS::VM::create();
        auto interpreter = JS::Interpreter::create<JS::GlobalObject>(*vm);
        interpreter->run(interpreter->global_object(), *program);
    }
    return 0;
}
