/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class NumberFormatPrototype final : public PrototypeObject<NumberFormatPrototype, NumberFormat> {
    JS_PROTOTYPE_OBJECT(NumberFormatPrototype, NumberFormat, Intl.NumberFormat);

public:
    explicit NumberFormatPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberFormatPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(format);
    JS_DECLARE_NATIVE_FUNCTION(format_to_parts);
    JS_DECLARE_NATIVE_FUNCTION(resolved_options);
};

}
