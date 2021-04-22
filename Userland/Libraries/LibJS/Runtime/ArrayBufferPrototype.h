/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class ArrayBufferPrototype final : public Object {
    JS_OBJECT(ArrayBufferPrototype, Object);

public:
    explicit ArrayBufferPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ArrayBufferPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(slice);
    JS_DECLARE_NATIVE_GETTER(byte_length_getter);
};

}
