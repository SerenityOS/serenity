/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class ReflectObject final : public Object {
    JS_OBJECT(ReflectObject, Object);

public:
    explicit ReflectObject(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ReflectObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(apply);
    JS_DECLARE_NATIVE_FUNCTION(construct);
    JS_DECLARE_NATIVE_FUNCTION(define_property);
    JS_DECLARE_NATIVE_FUNCTION(delete_property);
    JS_DECLARE_NATIVE_FUNCTION(get);
    JS_DECLARE_NATIVE_FUNCTION(get_own_property_descriptor);
    JS_DECLARE_NATIVE_FUNCTION(get_prototype_of);
    JS_DECLARE_NATIVE_FUNCTION(has);
    JS_DECLARE_NATIVE_FUNCTION(is_extensible);
    JS_DECLARE_NATIVE_FUNCTION(own_keys);
    JS_DECLARE_NATIVE_FUNCTION(prevent_extensions);
    JS_DECLARE_NATIVE_FUNCTION(set);
    JS_DECLARE_NATIVE_FUNCTION(set_prototype_of);
};

}
