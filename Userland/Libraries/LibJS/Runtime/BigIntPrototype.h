/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class BigIntPrototype final : public Object {
    JS_OBJECT(BigIntPrototype, Object);

public:
    explicit BigIntPrototype(Realm&);
    virtual void initialize(GlobalObject&) override;
    virtual ~BigIntPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_string);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
};

}
