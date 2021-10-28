/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class ArrayBufferPrototype final : public PrototypeObject<ArrayBufferPrototype, ArrayBuffer> {
    JS_PROTOTYPE_OBJECT(ArrayBufferPrototype, ArrayBuffer, ArrayBuffer);

public:
    explicit ArrayBufferPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ArrayBufferPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(slice);
    JS_DECLARE_NATIVE_FUNCTION(byte_length_getter);
};

}
