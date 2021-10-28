/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class SymbolPrototype final : public Object {
    JS_OBJECT(SymbolPrototype, Object);

public:
    explicit SymbolPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SymbolPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(description_getter);

    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(symbol_to_primitive);
};

}
