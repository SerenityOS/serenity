/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class NumberFormatConstructor final : public NativeFunction {
    JS_OBJECT(NumberFormatConstructor, NativeFunction);

public:
    explicit NumberFormatConstructor(Realm&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberFormatConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(supported_locales_of);
};

ThrowCompletionOr<NumberFormat*> initialize_number_format(GlobalObject& global_object, NumberFormat& number_format, Value locales_value, Value options_value);
ThrowCompletionOr<void> set_number_format_digit_options(GlobalObject& global_object, NumberFormatBase& intl_object, Object const& options, int default_min_fraction_digits, int default_max_fraction_digits, NumberFormat::Notation notation);
ThrowCompletionOr<void> set_number_format_unit_options(GlobalObject& global_object, NumberFormat& intl_object, Object const& options);

}
