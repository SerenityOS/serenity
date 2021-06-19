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

class DataView : public Object {
    JS_OBJECT(DataView, Object);

public:
    static DataView* create(GlobalObject&, ArrayBuffer*, size_t byte_length, size_t byte_offset);

    explicit DataView(ArrayBuffer*, size_t byte_length, size_t byte_offset, Object& prototype);
    virtual ~DataView() override;

    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }
    size_t byte_length() const { return m_byte_length; }
    size_t byte_offset() const { return m_byte_offset; }

private:
    virtual void visit_edges(Visitor& visitor) override;

    ArrayBuffer* m_viewed_array_buffer { nullptr };
    size_t m_byte_length { 0 };
    size_t m_byte_offset { 0 };
};

}
