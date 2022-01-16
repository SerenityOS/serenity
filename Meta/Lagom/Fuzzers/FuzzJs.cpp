/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Script.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto js = StringView(static_cast<const unsigned char*>(data), size);
    auto vm = JS::VM::create();
    auto interpreter = JS::Interpreter::create<JS::GlobalObject>(*vm);
    auto parse_result = JS::Script::parse(js, interpreter->realm());
    if (!parse_result.is_error())
        (void)interpreter->run(parse_result.value());

    return 0;
}
