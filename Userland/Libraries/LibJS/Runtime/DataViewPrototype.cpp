/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/DataViewPrototype.h>

namespace JS {

DataViewPrototype::DataViewPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void DataViewPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_accessor(vm.names.buffer, buffer_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.byteOffset, byte_offset_getter, {}, Attribute::Configurable);

    // 25.3.4.25 DataView.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-dataview.prototype-@@tostringtag
    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.DataView.as_string()), Attribute::Configurable);
}

DataViewPrototype::~DataViewPrototype()
{
}

static DataView* typed_this(VM& vm, GlobalObject& global_object)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !is<DataView>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, vm.names.DataView);
        return nullptr;
    }
    return static_cast<DataView*>(&this_value.as_object());
}

// 25.3.4.1 get DataView.prototype.buffer, https://tc39.es/ecma262/#sec-get-dataview.prototype.buffer
JS_DEFINE_NATIVE_GETTER(DataViewPrototype::buffer_getter)
{
    auto* data_view = typed_this(vm, global_object);
    if (!data_view)
        return {};
    return data_view->viewed_array_buffer();
}

// 25.3.4.2 get DataView.prototype.byteLength, https://tc39.es/ecma262/#sec-get-dataview.prototype.bytelength
JS_DEFINE_NATIVE_GETTER(DataViewPrototype::byte_length_getter)
{
    auto* data_view = typed_this(vm, global_object);
    if (!data_view)
        return {};
    if (data_view->viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }
    return Value(data_view->byte_length());
}

// 25.3.4.3 get DataView.prototype.byteOffset, https://tc39.es/ecma262/#sec-get-dataview.prototype.byteoffset
JS_DEFINE_NATIVE_GETTER(DataViewPrototype::byte_offset_getter)
{
    auto* data_view = typed_this(vm, global_object);
    if (!data_view)
        return {};
    if (data_view->viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }
    return Value(data_view->byte_offset());
}

}
