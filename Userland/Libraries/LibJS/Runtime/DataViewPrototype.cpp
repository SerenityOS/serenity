/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/DataViewPrototype.h>

namespace JS {

DataViewPrototype::DataViewPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DataViewPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.getBigInt64, get_big_int_64, 1, attr);
    define_native_function(vm.names.getBigUint64, get_big_uint_64, 1, attr);
    define_native_function(vm.names.getFloat32, get_float_32, 1, attr);
    define_native_function(vm.names.getFloat64, get_float_64, 1, attr);
    define_native_function(vm.names.getInt8, get_int_8, 1, attr);
    define_native_function(vm.names.getInt16, get_int_16, 1, attr);
    define_native_function(vm.names.getInt32, get_int_32, 1, attr);
    define_native_function(vm.names.getUint8, get_uint_8, 1, attr);
    define_native_function(vm.names.getUint16, get_uint_16, 1, attr);
    define_native_function(vm.names.getUint32, get_uint_32, 1, attr);
    define_native_function(vm.names.setBigInt64, set_big_int_64, 2, attr);
    define_native_function(vm.names.setBigUint64, set_big_uint_64, 2, attr);
    define_native_function(vm.names.setFloat32, set_float_32, 2, attr);
    define_native_function(vm.names.setFloat64, set_float_64, 2, attr);
    define_native_function(vm.names.setInt8, set_int_8, 2, attr);
    define_native_function(vm.names.setInt16, set_int_16, 2, attr);
    define_native_function(vm.names.setInt32, set_int_32, 2, attr);
    define_native_function(vm.names.setUint8, set_uint_8, 2, attr);
    define_native_function(vm.names.setUint16, set_uint_16, 2, attr);
    define_native_function(vm.names.setUint32, set_uint_32, 2, attr);

    define_native_accessor(vm.names.buffer, buffer_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.byteOffset, byte_offset_getter, {}, Attribute::Configurable);

    // 25.3.4.25 DataView.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-dataview.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.DataView.as_string()), Attribute::Configurable);
}

DataViewPrototype::~DataViewPrototype()
{
}

// 25.3.1.1 GetViewValue ( view, requestIndex, isLittleEndian, type ), https://tc39.es/ecma262/#sec-getviewvalue
template<typename T>
static ThrowCompletionOr<Value> get_view_value(GlobalObject& global_object, Value request_index, Value is_little_endian)
{
    auto& vm = global_object.vm();
    auto* view = TRY(DataViewPrototype::typed_this_value(global_object));
    auto get_index = TRY(request_index.to_index(global_object));
    auto little_endian = is_little_endian.to_boolean();

    auto buffer = view->viewed_array_buffer();
    if (buffer->is_detached())
        return vm.template throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    auto view_offset = view->byte_offset();
    auto view_size = view->byte_length();

    auto element_size = sizeof(T);

    Checked<size_t> buffer_index = get_index;
    buffer_index += view_offset;

    Checked<size_t> end_index = get_index;
    end_index += element_size;

    if (buffer_index.has_overflow() || end_index.has_overflow() || end_index.value() > view_size)
        return vm.throw_completion<RangeError>(global_object, ErrorType::DataViewOutOfRangeByteOffset, get_index, view_size);

    return buffer->get_value<T>(buffer_index.value(), false, ArrayBuffer::Order::Unordered, little_endian);
}

// 25.3.1.2 SetViewValue ( view, requestIndex, isLittleEndian, type, value ), https://tc39.es/ecma262/#sec-setviewvalue
template<typename T>
static ThrowCompletionOr<Value> set_view_value(GlobalObject& global_object, Value request_index, Value is_little_endian, Value value)
{
    auto& vm = global_object.vm();
    auto* view = TRY(DataViewPrototype::typed_this_value(global_object));
    auto get_index = TRY(request_index.to_index(global_object));

    Value number_value;
    if constexpr (IsIntegral<T> && sizeof(T) == 8)
        number_value = TRY(value.to_bigint(global_object));
    else
        number_value = TRY(value.to_number(global_object));

    auto little_endian = is_little_endian.to_boolean();

    auto buffer = view->viewed_array_buffer();
    if (buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    auto view_offset = view->byte_offset();
    auto view_size = view->byte_length();

    auto element_size = sizeof(T);

    Checked<size_t> buffer_index = get_index;
    buffer_index += view_offset;

    Checked<size_t> end_index = get_index;
    end_index += element_size;

    if (buffer_index.has_overflow() || end_index.has_overflow() || end_index.value() > view_size)
        return vm.throw_completion<RangeError>(global_object, ErrorType::DataViewOutOfRangeByteOffset, get_index, view_size);

    return buffer->set_value<T>(buffer_index.value(), number_value, false, ArrayBuffer::Order::Unordered, little_endian);
}

// 25.3.4.5 DataView.prototype.getBigInt64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getbigint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_big_int_64)
{
    return get_view_value<i64>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.6 DataView.prototype.getBigUint64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getbiguint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_big_uint_64)
{
    return get_view_value<u64>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.7 DataView.prototype.getFloat32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getfloat32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_float_32)
{
    return get_view_value<float>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.8 DataView.prototype.getFloat64 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getfloat64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_float_64)
{
    return get_view_value<double>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.9 DataView.prototype.getInt8 ( byteOffset ), https://tc39.es/ecma262/#sec-dataview.prototype.getint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_8)
{
    return get_view_value<i8>(global_object, vm.argument(0), Value(true));
}

// 25.3.4.10 DataView.prototype.getInt16 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_16)
{
    return get_view_value<i16>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.11 DataView.prototype.getInt32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_int_32)
{
    return get_view_value<i32>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.12 DataView.prototype.getUint8 ( byteOffset ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint8
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_8)
{
    return get_view_value<u8>(global_object, vm.argument(0), Value(true));
}

// 25.3.4.13 DataView.prototype.getUint16 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint16
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_16)
{
    return get_view_value<u16>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.14 DataView.prototype.getUint32 ( byteOffset [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.getuint32
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::get_uint_32)
{
    return get_view_value<u32>(global_object, vm.argument(0), vm.argument(1));
}

// 25.3.4.15 DataView.prototype.setBigInt64 ( byteOffset, value [ , littleEndian ] ), https://tc39.es/ecma262/#sec-dataview.prototype.setbigint64
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_big_int_64)
{
    return set_view_value<i64>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_big_uint_64)
{
    return set_view_value<u64>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_float_32)
{
    return set_view_value<float>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_float_64)
{
    return set_view_value<double>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_8)
{
    return set_view_value<i8>(global_object, vm.argument(0), Value(true), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_16)
{
    return set_view_value<i16>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_int_32)
{
    return set_view_value<i32>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_8)
{
    return set_view_value<u8>(global_object, vm.argument(0), Value(true), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_16)
{
    return set_view_value<u16>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::set_uint_32)
{
    return set_view_value<u32>(global_object, vm.argument(0), vm.argument(2), vm.argument(1));
}

// 25.3.4.1 get DataView.prototype.buffer, https://tc39.es/ecma262/#sec-get-dataview.prototype.buffer
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::buffer_getter)
{
    auto* data_view = TRY(typed_this_value(global_object));
    return data_view->viewed_array_buffer();
}

// 25.3.4.2 get DataView.prototype.byteLength, https://tc39.es/ecma262/#sec-get-dataview.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::byte_length_getter)
{
    auto* data_view = TRY(typed_this_value(global_object));
    if (data_view->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
    return Value(data_view->byte_length());
}

// 25.3.4.3 get DataView.prototype.byteOffset, https://tc39.es/ecma262/#sec-get-dataview.prototype.byteoffset
JS_DEFINE_NATIVE_FUNCTION(DataViewPrototype::byte_offset_getter)
{
    auto* data_view = TRY(typed_this_value(global_object));
    if (data_view->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
    return Value(data_view->byte_offset());
}

}
