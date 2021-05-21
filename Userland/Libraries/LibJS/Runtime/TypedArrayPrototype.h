/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class TypedArrayPrototype final : public Object {
    JS_OBJECT(TypedArrayPrototype, Object);

public:
    explicit TypedArrayPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~TypedArrayPrototype() override;

private:
    JS_DECLARE_NATIVE_GETTER(length_getter);
    JS_DECLARE_NATIVE_GETTER(buffer_getter);
    JS_DECLARE_NATIVE_GETTER(byte_length_getter);
    JS_DECLARE_NATIVE_GETTER(byte_offset_getter);

    JS_DECLARE_NATIVE_FUNCTION(at);
};

}
