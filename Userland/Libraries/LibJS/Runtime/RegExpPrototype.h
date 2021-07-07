/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/RegExpObject.h>

namespace JS {

class RegExpPrototype final : public Object {
    JS_OBJECT(RegExpPrototype, Object);

public:
    explicit RegExpPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~RegExpPrototype() override;

private:
    JS_DECLARE_NATIVE_GETTER(flags);
    JS_DECLARE_NATIVE_GETTER(source);

    JS_DECLARE_NATIVE_FUNCTION(exec);
    JS_DECLARE_NATIVE_FUNCTION(test);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(symbol_match);
    JS_DECLARE_NATIVE_FUNCTION(symbol_replace);
    JS_DECLARE_NATIVE_FUNCTION(symbol_search);

#define __JS_ENUMERATE(_, flag_name, ...) \
    JS_DECLARE_NATIVE_GETTER(flag_name);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE
};

}
