/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class ArrayBuffer : public Object {
    JS_OBJECT(ArrayBuffer, Object);

public:
    static ArrayBuffer* create(GlobalObject&, size_t);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer&);

    ArrayBuffer(size_t, Object& prototype);
    ArrayBuffer(ByteBuffer& buffer, Object& prototype);
    virtual ~ArrayBuffer() override;

    size_t byte_length() const { return m_buffer.size(); }
    ByteBuffer& buffer() { return m_buffer; }
    const ByteBuffer& buffer() const { return m_buffer; }

private:
    ByteBuffer m_buffer;
};

}
