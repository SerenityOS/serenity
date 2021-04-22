/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class ObjectPrototype final : public Object {
    JS_OBJECT(ObjectPrototype, Object);

public:
    explicit ObjectPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ObjectPrototype() override;

    // public to serve as intrinsic function %Object.prototype.toString%
    JS_DECLARE_NATIVE_FUNCTION(to_string);

private:
    JS_DECLARE_NATIVE_FUNCTION(has_own_property);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_string);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(property_is_enumerable);
    JS_DECLARE_NATIVE_FUNCTION(is_prototype_of);
};

}
