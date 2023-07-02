/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

Optional<size_t> get_view_byte_length(VM& vm, DataView const& view, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length);

class DataView : public Object {
    JS_OBJECT(DataView, Object);

public:
    static NonnullGCPtr<DataView> create(Realm&, ArrayBuffer*, Optional<size_t> byte_length, size_t byte_offset);

    virtual ~DataView() override = default;

    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }
    Optional<size_t> byte_length() const { return m_byte_length; }
    bool is_byte_length_auto() const { return !m_byte_length.has_value(); }
    size_t byte_offset() const { return m_byte_offset; }

    // FIXME: Some functions aren't aware of byte length being OOB in the resizable ArrayBuffers proposal
    // These functions performs the steps to get the byte length that is used in other parts of the proposal
    size_t idempotent_byte_length() const
    {
        auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);
        return get_view_byte_length(vm(), *this, get_buffer_byte_length).value_or(0);
    }

private:
    DataView(ArrayBuffer*, Optional<size_t> byte_length, size_t byte_offset, Object& prototype);

    virtual void visit_edges(Visitor& visitor) override;

    GCPtr<ArrayBuffer> m_viewed_array_buffer;
    Optional<size_t> m_byte_length { 0 };
    size_t m_byte_offset { 0 };
};

}
