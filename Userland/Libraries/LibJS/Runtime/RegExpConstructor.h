/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class RegExpConstructor final : public NativeFunction {
    JS_OBJECT(RegExpConstructor, NativeFunction);

public:
    explicit RegExpConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~RegExpConstructor() override = default;
    void set_captured_values(Array* const& captured_values) { m_captured_values = captured_values; }
    void set_input(Value const& input) { m_input = input; }
    void set_last_match(Value const& last_match) { m_last_match = last_match; }
    void set_last_paren(Value const& last_paren) { m_last_paren = last_paren; }
    void set_right_context(Value const& right_context) { m_right_context = right_context; }
    void set_left_context(Value const& left_context) { m_left_context = left_context; }

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject&) override;

private:
    virtual bool has_constructor() const override { return true; }
    Array* m_captured_values { nullptr };
    Value m_input { js_undefined() };
    Value m_last_match { js_undefined() };
    Value m_last_paren { js_undefined() };
    Value m_right_context { js_undefined() };
    Value m_left_context { js_undefined() };

    JS_DECLARE_NATIVE_FUNCTION(input_getter);
    JS_DECLARE_NATIVE_FUNCTION(input_setter);
    JS_DECLARE_NATIVE_FUNCTION(last_match_getter);
    JS_DECLARE_NATIVE_FUNCTION(last_paren_getter);
    JS_DECLARE_NATIVE_FUNCTION(left_context_getter);
    JS_DECLARE_NATIVE_FUNCTION(right_context_getter);
    JS_DECLARE_NATIVE_FUNCTION($1_getter);
    JS_DECLARE_NATIVE_FUNCTION($2_getter);
    JS_DECLARE_NATIVE_FUNCTION($3_getter);
    JS_DECLARE_NATIVE_FUNCTION($4_getter);
    JS_DECLARE_NATIVE_FUNCTION($5_getter);
    JS_DECLARE_NATIVE_FUNCTION($6_getter);
    JS_DECLARE_NATIVE_FUNCTION($7_getter);
    JS_DECLARE_NATIVE_FUNCTION($8_getter);
    JS_DECLARE_NATIVE_FUNCTION($9_getter);
    JS_DECLARE_NATIVE_FUNCTION(symbol_species_getter);
};

}
