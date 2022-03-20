/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NumberObject.h>

namespace JS {

class NumberPrototype final : public NumberObject {
    JS_OBJECT(NumberPrototype, NumberObject);

public:
    explicit NumberPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberPrototype() override = default;

    JS_DECLARE_NATIVE_FUNCTION(to_exponential);
    JS_DECLARE_NATIVE_FUNCTION(to_fixed);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_string);
    JS_DECLARE_NATIVE_FUNCTION(to_precision);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
};

}
