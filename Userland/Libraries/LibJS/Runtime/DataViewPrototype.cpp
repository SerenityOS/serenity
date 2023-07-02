/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/DataViewPrototype.h>

namespace JS {

DataViewPrototype::DataViewPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().object_prototype())
{
}

ThrowCompletionOr<void> DataViewPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(realm, vm.names.getBigInt64, get_big_int_64, 1, attr);
    define_native_function(realm, vm.names.getBigUint64, get_big_uint_64, 1, attr);
    define_native_function(realm, vm.names.getFloat32, get_float_32, 1, attr);
    define_native_function(realm, vm.names.getFloat64, get_float_64, 1, attr);
    define_native_function(realm, vm.names.getInt8, get_int_8, 1, attr);
    define_native_function(realm, vm.names.getInt16, get_int_16, 1, attr);
    define_native_function(realm, vm.names.getInt32, get_int_32, 1, attr);
    define_native_function(realm, vm.names.getUint8, get_uint_8, 1, attr);
    define_native_function(realm, vm.names.getUint16, get_uint_16, 1, attr);
    define_native_function(realm, vm.names.getUint32, get_uint_32, 1, attr);
    define_native_function(realm, vm.names.setBigInt64, set_big_int_64, 2, attr);
    define_native_function(realm, vm.names.setBigUint64, set_big_uint_64, 2, attr);
    define_native_function(realm, vm.names.setFloat32, set_float_32, 2, attr);
    define_native_function(realm, vm.names.setFloat64, set_float_64, 2, attr);
    define_native_function(realm, vm.names.setInt8, set_int_8, 2, attr);
    define_native_function(realm, vm.names.setInt16, set_int_16, 2, attr);
    define_native_function(realm, vm.names.setInt32, set_int_32, 2, attr);
    define_native_function(realm, vm.names.setUint8, set_uint_8, 2, attr);
    define_native_function(realm, vm.names.setUint16, set_uint_16, 2, attr);
    define_native_function(realm, vm.names.setUint32, set_uint_32, 2, attr);

    define_native_accessor(realm, vm.names.buffer, buffer_getter, {}, Attribute::Configurable);
    define_native_accessor(realm, vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(realm, vm.names.byteOffset, byte_offset_getter, {}, Attribute::Configurable);

    // 25.3.4.25 DataView.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-dataview.prototype-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), PrimitiveString::create(vm, vm.names.DataView.as_string()), Attribute::Configurable);

    return {};
}

bool is_view_out_of_bounds(VM& vm, DataView const& view, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length);

// 25.3.1.1 GetViewValue ( view, requestIndex, isLittleEndian, type ), https://tc39.es/ecma262/#sec-getviewvalue
// 5.1.3 GetViewValue ( view, requestIndex, isLittleEndian, type ), https://tc39.es/proposal-resizablearraybuffer/#sec-getviewvalue
template<typename T>
static ThrowCompletionOr<Value> get_view_value(VM& vm, Value request_index, Value is_little_endian)
{
    // 1. Perform ? RequireInternalSlot(view, [[DataView]]).
    // 2. Assert: view has a [[ViewedArrayBuffer]] internal slot.
    auto view = TRY(DataViewPrototype::typed_this_value(vm));

    // 3. Let getIndex be ? ToIndex(requestIndex).
    auto get_index = TRY(request_index.to_index(vm));

    // 4. Set isLittleEndian to ToBoolean(isLittleEndian).
    auto little_endian = is_little_endian.to_boolean();

    // 5. Let buffer be view.[[ViewedArrayBuffer]].
    auto buffer = view->viewed_array_buffer();

    // 6. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // FIXME: 7. NOTE: Bounds checking is not a synchronizing operation when view's backing buffer is a growable SharedArrayBuffer.

    // 8. Let viewOffset be view.[[ByteOffset]].
    auto view_offset = view->byte_offset();

    // 9. Let viewSize be GetViewByteLength(view, getBufferByteLength).
    auto view_size = get_view_byte_length(vm, *view, get_buffer_byte_length);

    // 10. If viewSize is out-of-bounds, throw a TypeError exception.
    if (!view_size.has_value())
        return vm.throw_completion<TypeError>(ErrorType::DataViewOverflowOrOutOfBounds, "view size");

    // 11. Let elementSize be the Element Size value specified in Table 68 for Element Type type.
    auto element_size = sizeof(T);

    // 12. Let bufferIndex be getIndex + viewOffset.
    Checked<size_t> buffer_index = get_index;
    buffer_index += view_offset;

    Checked<size_t> end_index = get_index;
    end_index += element_size;

    // 13. If getIndex + elementSize > viewSize, throw a RangeError exception.
    if (buffer_index.has_overflow() || end_index.has_overflow() || end_index.value() > view_size.value())
        return vm.throw_completion<RangeError>(ErrorType::DataViewOutOfRangeByteOffset, get_index, view_size.value());

    // 14. Return GetValueFromBuffer(buffer, bufferIndex, type, false, Unordered, isLittleEndian).
    return buffer->get_value<T>(buffer_index.value(), false, ArrayBuffer::Order::Unordered, little_endian);
}

// 25.3.1.2 SetViewValue ( view, requestIndex, isLittleEndian, type, value ), https://tc39.es/ecma262/#sec-setviewvalue
template<typename T>
static ThrowCompletionOr<Value> set_view_value(VM& vm, Value request_index, Value is_little_endian, Value value)
{
    // 1. Perform ? RequireInternalSlot(view, [[DataView]]).
    // 2. Assert: view has a [[ViewedArrayBuffer]] internal slot.
    auto view = TRY(DataViewPrototype::typed_this_value(vm));

    // 3. Let getIndex be ? ToIndex(requestIndex).
    auto get_index = TRY(request_index.to_index(vm));

    Value number_value;

    // 4. If IsBigIntElementType(type) is true, let numberValue be ? ToBigInt(value).
    if constexpr (IsIntegral<T> && sizeof(T) == 8)
        number_value = TRY(value.to_bigint(vm));
    // 5. Otherwise, let numberValue be ? ToNumber(value).
    else
        number_value = TRY(value.to_number(vm));

    // 6. Set isLittleEndian to ToBoolean(isLittleEndian).
    auto little_endian = is_little_endian.to_boolean();

    // 7. Let buffer be view.[[ViewedArrayBuffer]].
    auto buffer = view->viewed_array_buffer();

    // 8. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // FIXME: 9. NOTE: Bounds checking is not a synchronizing operation when view's backing buffer is a growable SharedArrayBuffer.

    // 10. Let viewOffset be view.[[ByteOffset]].
    auto view_offset = view->byte_offset();

    // 11. Let viewSize be GetViewByteLength(view, getBufferByteLength).
    auto view_size = get_view_byte_length(vm, *view, get_buffer_byte_length);

    // 12. If viewSize is out-of-bounds, throw a TypeError exception.
    if (!view_size.has_value())
        return vm.throw_completion<TypeError>(ErrorType::DataViewOverflowOrOutOfBounds, "view size");

    // 11. Let elementSize be the Element Size value specified in Table 68 for Element Type type.
    auto element_size = sizeof(T);

    // 13. Let bufferIndex be getIndex + viewOffset.
    Checked<size_t> buffer_index = get_index;
    buffer_index += view_offset;

    Checked<size_t> end_index = get_index;
    end_index += element_size;

    // 12. If getIndex + elementSize > viewSize, throw a RangeError exception.
    if (buffer_index.has_overflow() || end_index.has_overflow() || end_index.value() > view_size.value())
        return vm.throw_completion<RangeError>(ErrorType::DataViewOutOfRangeByteOffset, get_index, view_size.value());

    // 14. Perform SetValueInBuffer(buffer, bufferIndex, type, numberValue, false, Unordered, isLittleEndian).
    buffer->set_value<T>(buffer_index.value(), number_value, false, ArrayBuffer::Order::Unordered, little_endian);

    // 15. Return undefined.
    return js_undefined();
}

// 5.1.1 GetViewByteLength ( view, getBufferByteLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-getviewbytelength
Optional<size_t> get_view_byte_length(VM& vm, DataView const& view, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length)
{
    // 1. If IsViewOutOfBounds(view, getBufferByteLength) is true, return out-of-bounds.
    if (is_view_out_of_bounds(vm, view, get_buffer_byte_length))
        return {};

    // 2. If view.[[ByteLength]] is not auto, return view.[[ByteLength]].
    if (!view.is_byte_length_auto())
        return view.byte_length().value();

    // 3. Let buffer be view.[[ViewedArrayBuffer]].
    auto buffer = view.viewed_array_buffer();

    // 4. Let bufferByteLength be getBufferByteLength(buffer).
    auto buffer_byte_length = get_buffer_byte_length(vm, *buffer);

    // 5. Assert: IsResizableArrayBuffer(buffer) is true.
    VERIFY(buffer->is_resizable());

    // 6. Let byteOffset be view.[[ByteOffset]].
    auto byte_offset = view.byte_offset();

    // 7. Return bufferByteLength - byteOffset.
    return buffer_byte_length - byte_offset;
}

// 5.1.2 IsViewOutOfBounds ( view, getBufferByteLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-isviewoutofbounds
bool is_view_out_of_bounds(VM& vm, DataView const& view, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length)
{
    // 1. Let buffer be view.[[ViewedArrayBuffer]].
    auto buffer = view.viewed_array_buffer();

    // 2. If IsDetachedBuffer(buffer) is true, return true.
    if (buffer->is_detached())
        return true;

    // 3. Let bufferByteLength be getBufferByteLength(buffer).
    auto buffer_byte_length = get_buffer_byte_length(vm, *buffer);

    // 4. Let byteOffsetStart be view.[[ByteOffset]].
    auto byte_offset_start = view.byte_offset();

    // 5. If view.[[ByteLength]] is auto, then
    // a. Let byteOffsetEnd be bufferByteLength.
    // 6. Else,
    // a. Let byteOffsetEnd be byteOffsetStart + view.[[ByteLength]].
    auto byte_offset_end = view.is_byte_length_auto() ? buffer_byte_length : byte_offset_start + view.byte_length().value();

    // 7. If byteOffsetStart > bufferByteLength or byteOffsetEnd > bufferByteLength, return true.
    if (byte_offset_start > buffer_byte_length || byte_offset_end > buffer_byte_length)
        return true;

    // 8. NOTE: 0-length DataViews are not considered out-of-bounds.

    // 9. Return false.
    return false;
}

// 25.3.4.1 get DataView.prototype.buffer, https://tc39.es/ecma262/#sec-get-dataview.prototype.buffer
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::buffer_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[DataView]]).
    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    auto data_view = TRY(typed_this_value(vm));

    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    // 5. Return buffer.
    return data_view->viewed_array_buffer();
}

// 25.3.4.2 get DataView.prototype.byteLength, https://tc39.es/ecma262/#sec-get-dataview.prototype.bytelength
// 5.3.1 get DataView.prototype.byteLength, https://tc39.es/proposal-resizablearraybuffer/#sec-get-dataview.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::byte_length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[DataView]]).
    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    auto data_view = TRY(typed_this_value(vm));

    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. If IsViewOutOfBounds(O, getBufferByteLength) is true, throw a TypeError exception.
    if (is_view_out_of_bounds(vm, *data_view, get_buffer_byte_length))
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "byte length");

    // 6. Let size be O.[[ByteLength]].
    // 7. Return 𝔽(size).
    return Value(get_view_byte_length(vm, *data_view, get_buffer_byte_length).value());
}

// 25.3.4.3 get DataView.prototype.byteOffset, https://tc39.es/ecma262/#sec-get-dataview.prototype.byteoffset
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::byte_offset_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[DataView]]).
    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    auto data_view = TRY(typed_this_value(vm));

    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. If IsViewOutOfBounds(O, getBufferByteLength) is true, throw a TypeError exception.
    if (is_view_out_of_bounds(vm, *data_view, get_buffer_byte_length))
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "byte offset");

    // 7. Let offset be O.[[ByteOffset]].
    // 8. Return 𝔽(offset).
    return Value(data_view->byte_offset());
}

// 25.3.4.5 DataView.prototype.getBigInt64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getbigint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_big_int_64)
{
    // 1. Let v be the this value.
    // 2. Return ? GetViewValue(v, byteOffset, littleEndian, BigInt64).
    return get_view_value<i64>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.6 DataView.prototype.getBigUint64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getbiguint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_big_uint_64)
{
    // 1. Let v be the this value.
    // 2. Return ? GetViewValue(v, byteOffset, littleEndian, BigUint64).
    return get_view_value<u64>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.7 DataView.prototype.getFloat32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getfloat32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_float_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Float32).
    return get_view_value<float>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.8 DataView.prototype.getFloat64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getfloat64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_float_64)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Float64).
    return get_view_value<double>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.9 DataView.prototype.getInt8 ( byteOffset ), https://tc39.es/ecma262/#sec-dataview.prototype.getint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_8)
{
    // 1. Let v be the this value.
    // 2. Return ? GetViewValue(v, byteOffset, true, Int8).
    return get_view_value<i8>(vm, vm.argument(0), Value(true));
}

// 25.3.4.10 DataView.prototype.getInt16 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_16)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Int16).
    return get_view_value<i16>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.11 DataView.prototype.getInt32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Int32).
    return get_view_value<i32>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.12 DataView.prototype.getUint8 ( byteOffset ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_8)
{
    // 1. Let v be the this value.
    // 2. Return ? GetViewValue(v, byteOffset, true, Uint8).
    return get_view_value<u8>(vm, vm.argument(0), Value(true));
}

// 25.3.4.13 DataView.prototype.getUint16 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_16)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Uint16).
    return get_view_value<u16>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.14 DataView.prototype.getUint32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? GetViewValue(v, byteOffset, littleEndian, Uint32).
    return get_view_value<u32>(vm, vm.argument(0), vm.argument(1));
}

// 25.3.4.15 DataView.prototype.setBigInt64 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setbigint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_big_int_64)
{
    // 1. Let v be the this value.
    // 2. Return ? SetViewValue(v, byteOffset, littleEndian, BigInt64, value).
    return set_view_value<i64>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.16 DataView.prototype.setBigUint64 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setbiguint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_big_uint_64)
{
    // 1. Let v be the this value.
    // 2. Return ? SetViewValue(v, byteOffset, littleEndian, BigUint64, value).
    return set_view_value<u64>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.17 DataView.prototype.setFloat32 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setfloat32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_float_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Float32, value).
    return set_view_value<float>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.18 DataView.prototype.setFloat64 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setfloat64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_float_64)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Float64, value).
    return set_view_value<double>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.19 DataView.prototype.setInt8 ( byteOffset, value ), https://tc39.es/ecma262/#sec-dataview.prototype.setint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_8)
{
    // 1. Let v be the this value.
    // 2. Return ? SetViewValue(v, byteOffset, true, Int8, value).
    return set_view_value<i8>(vm, vm.argument(0), Value(true), vm.argument(1));
}

// 25.3.4.20 DataView.prototype.setInt16 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_16)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Int16, value).
    return set_view_value<i16>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.21 DataView.prototype.setInt32 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Int32, value).
    return set_view_value<i32>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.22 DataView.prototype.setUint8 ( byteOffset, value ), https://tc39.es/ecma262/#sec-dataview.prototype.setuint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_8)
{
    // 1. Let v be the this value.
    // 2. Return ? SetViewValue(v, byteOffset, true, Uint8, value).
    return set_view_value<u8>(vm, vm.argument(0), Value(true), vm.argument(1));
}

// 25.3.4.23 DataView.prototype.setUint16 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setuint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_16)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Uint16, value).
    return set_view_value<u16>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.24 DataView.prototype.setUint32 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setuint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_32)
{
    // 1. Let v be the this value.
    // 2. If littleEndian is not present, set littleEndian to false.
    // 3. Return ? SetViewValue(v, byteOffset, littleEndian, Uint32, value).
    return set_view_value<u32>(vm, vm.argument(0), vm.argument(2), vm.argument(1));
}

}
