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

// 25.3.1.1 DataView With Buffer Witness Records, https://tc39.es/ecma262/#sec-dataview-with-buffer-witness-records
struct DataViewRecord {
    DataView const* object;
    Optional<size_t> cached_buffer_byte_length;
};

DataViewRecord make_data_view_with_buffer_witness_record(VM&, DataView const&, ArrayBuffer::Order);
size_t get_view_byte_length(DataViewRecord const&);
bool is_view_out_of_bounds(DataViewRecord const&);

class DataView : public Object {
    JS_OBJECT(DataView, Object);

public:
    static NonnullGCPtr<DataView> create(Realm&, ArrayBuffer*, Optional<size_t> byte_length, size_t byte_offset);

    virtual ~DataView() override = default;

    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }
    Optional<size_t> byte_length() const { return m_byte_length; }
    bool is_byte_length_auto() const { return !m_byte_length.has_value(); }
    size_t byte_offset() const { return m_byte_offset; }

    // FIXME: Some functions in other web specs aren't aware of byte length potentionally being auto
    // These functions performs the steps to get the byte length that is used in other parts of the EMCAScript spec
    size_t auto_unaware_byte_length() const
    {
        auto view_record = make_data_view_with_buffer_witness_record(vm(), *this, ArrayBuffer::Order::Unordered);
        return is_view_out_of_bounds(view_record) ? 0 : get_view_byte_length(view_record);
    }

private:
    DataView(ArrayBuffer*, Optional<size_t> byte_length, size_t byte_offset, Object& prototype);

    virtual void visit_edges(Visitor& visitor) override;

    GCPtr<ArrayBuffer> m_viewed_array_buffer;
    Optional<size_t> m_byte_length { 0 };
    size_t m_byte_offset { 0 };
};

}
