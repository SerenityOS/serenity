/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
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
    JS_DECLARE_NATIVE_FUNCTION(to_string_tag_getter);
    JS_DECLARE_NATIVE_FUNCTION(at);
    JS_DECLARE_NATIVE_FUNCTION(every);
    JS_DECLARE_NATIVE_FUNCTION(find);
    JS_DECLARE_NATIVE_FUNCTION(find_index);
    JS_DECLARE_NATIVE_FUNCTION(for_each);
    JS_DECLARE_NATIVE_FUNCTION(some);
    JS_DECLARE_NATIVE_FUNCTION(join);
};

}
