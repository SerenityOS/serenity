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
    static ArrayBuffer* create(GlobalObject&, ByteBuffer*);

    ArrayBuffer(size_t, Object& prototype);
    ArrayBuffer(ByteBuffer* buffer, Object& prototype);
    virtual ~ArrayBuffer() override;

    size_t byte_length() const { return buffer_impl().size(); }
    ByteBuffer& buffer() { return buffer_impl(); }
    const ByteBuffer& buffer() const { return buffer_impl(); }

    Value detach_key() const { return m_detach_key; }
    void set_detach_key(Value detach_key) { m_detach_key = detach_key; }

    void detach_buffer() { m_buffer = Empty {}; }
    bool is_detached() const { return m_buffer.has<Empty>(); }

private:
    virtual void visit_edges(Visitor&) override;

    ByteBuffer& buffer_impl()
    {
        ByteBuffer* ptr { nullptr };
        m_buffer.visit([&](Empty) { VERIFY_NOT_REACHED(); }, [&](auto* pointer) { ptr = pointer; }, [&](auto& value) { ptr = &value; });
        return *ptr;
    }

    const ByteBuffer& buffer_impl() const { return const_cast<ArrayBuffer*>(this)->buffer_impl(); }

    Variant<Empty, ByteBuffer, ByteBuffer*> m_buffer;
    // The various detach related members of ArrayBuffer are not used by any ECMA262 functionality,
    // but are required to be available for the use of various harnesses like the Test262 test runner.
    Value m_detach_key;
};

}
