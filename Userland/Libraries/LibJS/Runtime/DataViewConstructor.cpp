/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/DataViewConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

DataViewConstructor::DataViewConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.DataView.as_string(), *global_object.function_prototype())
{
}

void DataViewConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 25.3.3.1 DataView.prototype, https://tc39.es/ecma262/#sec-dataview.prototype
    define_direct_property(vm.names.prototype, global_object.data_view_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

DataViewConstructor::~DataViewConstructor()
{
}

// 25.3.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] ), https://tc39.es/ecma262/#sec-dataview-buffer-byteoffset-bytelength
Value DataViewConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.DataView);
    return {};
}

// 25.3.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] ), https://tc39.es/ecma262/#sec-dataview-buffer-byteoffset-bytelength
Value DataViewConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto buffer = vm.argument(0);
    if (!buffer.is_object() || !is<ArrayBuffer>(buffer.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::IsNotAn, buffer.to_string_without_side_effects(), vm.names.ArrayBuffer);
        return {};
    }
    auto& array_buffer = static_cast<ArrayBuffer&>(buffer.as_object());

    auto offset = vm.argument(1).to_index(global_object);
    if (vm.exception())
        return {};

    if (array_buffer.is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }

    auto buffer_byte_length = array_buffer.byte_length();
    if (offset > buffer_byte_length) {
        vm.throw_exception<RangeError>(global_object, ErrorType::DataViewOutOfRangeByteOffset, offset, buffer_byte_length);
        return {};
    }

    size_t view_byte_length;
    if (vm.argument(2).is_undefined()) {
        view_byte_length = buffer_byte_length - offset;
    } else {
        view_byte_length = vm.argument(2).to_index(global_object);
        if (vm.exception())
            return {};
        if (offset + view_byte_length > buffer_byte_length) {
            vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, vm.names.DataView);
            return {};
        }
    }

    auto* data_view = TRY_OR_DISCARD(ordinary_create_from_constructor<DataView>(global_object, new_target, &GlobalObject::data_view_prototype, &array_buffer, view_byte_length, offset));

    if (array_buffer.is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }

    return data_view;
}

}
