/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/DataView.h>

namespace JS {

NonnullGCPtr<DataView> DataView::create(Realm& realm, ArrayBuffer* viewed_buffer, Optional<size_t> byte_length, size_t byte_offset)
{
    return realm.heap().allocate<DataView>(realm, viewed_buffer, byte_length, byte_offset, realm.intrinsics().data_view_prototype());
}

DataView::DataView(ArrayBuffer* viewed_buffer, Optional<size_t> byte_length, size_t byte_offset, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_viewed_array_buffer(viewed_buffer)
    , m_byte_length(move(byte_length))
    , m_byte_offset(byte_offset)
{
}

void DataView::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

// 25.3.1.2 MakeDataViewWithBufferWitnessRecord ( obj, order )
DataViewRecord make_data_view_with_buffer_witness_record(VM& vm, DataView const& data_view, ArrayBuffer::Order order)
{
    // 1. Let buffer be obj.[[ViewedArrayBuffer]].
    auto* buffer = data_view.viewed_array_buffer();

    // 2. If IsDetachedBuffer(buffer) is true, then
    // a. Let byteLength be detached.
    // 3. Else,
    // a. Let byteLength be ArrayBufferByteLength(buffer, order).
    auto byte_length = buffer->is_detached() ? Optional<size_t> {} : array_buffer_byte_length(vm, *buffer, order);

    // 4. Return the DataView With Buffer Witness Record { [[Object]]: obj, [[CachedBufferByteLength]]: byteLength }.
    return { &data_view, byte_length };
}

// 25.3.1.3 GetViewByteLength ( viewRecord ), https://tc39.es/ecma262/#sec-getviewbytelength
size_t get_view_byte_length(DataViewRecord const& view_record)
{
    // 1. Assert: IsViewOutOfBounds(viewRecord) is false.
    VERIFY(!is_view_out_of_bounds(view_record));

    // 2. Let view be viewRecord.[[Object]].
    auto const* view = view_record.object;

    // 3. If view.[[ByteLength]] is not auto, return view.[[ByteLength]].
    if (!view->is_byte_length_auto())
        return view->byte_length().value();

    // 4. Assert: IsFixedLengthArrayBuffer(view.[[ViewedArrayBuffer]]) is false.
    VERIFY(!view->viewed_array_buffer()->is_fixed_length());

    // 5. Let byteOffset be view.[[ByteOffset]].
    auto byte_offset = view->byte_offset();

    // 6. Let byteLength be viewRecord.[[CachedBufferByteLength]].
    auto byte_length = view_record.cached_buffer_byte_length;

    // 7. Assert: byteLength is not detached.
    VERIFY(byte_length.has_value());

    // 8. Return byteLength - byteOffset.
    return byte_length.value() - byte_offset;
}

// 25.3.1.4 IsViewOutOfBounds ( viewRecord ), https://tc39.es/ecma262/#sec-getviewbytelength
bool is_view_out_of_bounds(DataViewRecord const& view_record)
{
    // 1. Let view be viewRecord.[[Object]].
    auto const* view = view_record.object;

    // 2. Let bufferByteLength be viewRecord.[[CachedBufferByteLength]].
    auto buffer_byte_length = view_record.cached_buffer_byte_length;

    // 3. Assert: IsDetachedBuffer(view.[[ViewedArrayBuffer]]) is true if and only if bufferByteLength is detached.
    if (!buffer_byte_length.has_value()) {
        VERIFY(view->viewed_array_buffer()->is_detached());

        // 4. If bufferByteLength is detached, return true.
        return true;
    }

    // 5. Let byteOffsetStart be view.[[ByteOffset]].
    auto byte_offset_start = view->byte_offset();

    size_t byte_offset_end = 0;

    // 6. If view.[[ByteLength]] is auto, then
    if (view->is_byte_length_auto()) {
        // a. Let byteOffsetEnd be bufferByteLength.
        byte_offset_end = buffer_byte_length.value();
    }
    // 7. Else,
    else {
        // a. Let byteOffsetEnd be byteOffsetStart + view.[[ByteLength]].
        byte_offset_end = byte_offset_start + view->byte_length().value();
    }

    // 8. If byteOffsetStart > bufferByteLength or byteOffsetEnd > bufferByteLength, return true.
    if (byte_offset_start > buffer_byte_length.value() || byte_offset_end > buffer_byte_length.value())
        return true;

    // 9. NOTE: 0-length DataViews are not considered out-of-bounds.

    // 10. Return false.
    return false;
}

}
