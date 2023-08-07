/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/DataViewConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

DataViewConstructor::DataViewConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.DataView.as_string(), realm.intrinsics().function_prototype())
{
}

void DataViewConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);

    // 25.3.3.1 DataView.prototype, https://tc39.es/ecma262/#sec-dataview.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().data_view_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 25.3.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] ), https://tc39.es/ecma262/#sec-dataview-buffer-byteoffset-bytelength
ThrowCompletionOr<Value> DataViewConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, vm.names.DataView);
}

// 25.3.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] ), https://tc39.es/ecma262/#sec-dataview-buffer-byteoffset-bytelength
ThrowCompletionOr<NonnullGCPtr<Object>> DataViewConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto buffer = vm.argument(0);
    auto byte_offset = vm.argument(1);
    auto byte_length = vm.argument(2);

    // 2. Perform ? RequireInternalSlot(buffer, [[ArrayBufferData]]).
    if (!buffer.is_object() || !is<ArrayBuffer>(buffer.as_object()))
        return vm.throw_completion<TypeError>(ErrorType::IsNotAn, TRY_OR_THROW_OOM(vm, buffer.to_string_without_side_effects()), vm.names.ArrayBuffer);

    auto& array_buffer = static_cast<ArrayBuffer&>(buffer.as_object());

    // 3. Let offset be ? ToIndex(byteOffset).
    auto offset = TRY(byte_offset.to_index(vm));

    // 4. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (array_buffer.is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 5. Let bufferByteLength be buffer.[[ArrayBufferByteLength]].
    auto buffer_byte_length = array_buffer.byte_length();

    // 6. If offset > bufferByteLength, throw a RangeError exception.
    if (offset > buffer_byte_length)
        return vm.throw_completion<RangeError>(ErrorType::DataViewOutOfRangeByteOffset, offset, buffer_byte_length);

    size_t view_byte_length;

    // 7. If byteLength is undefined, then
    if (byte_length.is_undefined()) {
        // a. Let viewByteLength be bufferByteLength - offset.
        view_byte_length = buffer_byte_length - offset;
    }
    // 8. Else,
    else {
        // a. Let viewByteLength be ? ToIndex(byteLength).
        view_byte_length = TRY(byte_length.to_index(vm));

        // b. If offset + viewByteLength > bufferByteLength, throw a RangeError exception.
        auto const checked_add = AK::make_checked(view_byte_length) + AK::make_checked(offset);
        if (checked_add.has_overflow() || checked_add.value() > buffer_byte_length)
            return vm.throw_completion<RangeError>(ErrorType::InvalidLength, vm.names.DataView);
    }

    // 9. Let O be ? OrdinaryCreateFromConstructor(NewTarget, "%DataView.prototype%", « [[DataView]], [[ViewedArrayBuffer]], [[ByteLength]], [[ByteOffset]] »).
    // 11. Set O.[[ViewedArrayBuffer]] to buffer.
    // 12. Set O.[[ByteLength]] to viewByteLength.
    // 13. Set O.[[ByteOffset]] to offset.
    auto data_view = TRY(ordinary_create_from_constructor<DataView>(vm, new_target, &Intrinsics::data_view_prototype, &array_buffer, view_byte_length, offset));

    // 10. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (array_buffer.is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 14. Return O.
    return data_view;
}

}
