/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class ArrayBuffer : public Object {
    JS_OBJECT(ArrayBuffer, Object);

public:
    static ArrayBuffer* create(GlobalObject&, size_t);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer&);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer*);

    ArrayBuffer(size_t, Object& prototype);
    ArrayBuffer(ByteBuffer& buffer, Object& prototype);
    ArrayBuffer(ByteBuffer* buffer, Object& prototype);
    virtual ~ArrayBuffer() override;

    size_t byte_length() const { return buffer_impl().size(); }
    ByteBuffer& buffer() { return buffer_impl(); }
    const ByteBuffer& buffer() const { return buffer_impl(); }

private:
    ByteBuffer& buffer_impl()
    {
        ByteBuffer* ptr { nullptr };
        m_buffer.visit([&](auto* pointer) { ptr = pointer; }, [&](auto& value) { ptr = &value; });
        return *ptr;
    }

    const ByteBuffer& buffer_impl() const { return const_cast<ArrayBuffer*>(this)->buffer_impl(); }

    Variant<ByteBuffer, ByteBuffer*> m_buffer;
};

}
