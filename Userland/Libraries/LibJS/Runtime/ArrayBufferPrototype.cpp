/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, Jamie Mansfield <jmansfield@cadixdev.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBufferPrototype::ArrayBufferPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().object_prototype())
{
}

void ArrayBufferPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.slice, slice, 2, attr);
    define_native_function(realm, vm.names.transfer, transfer, 0, attr);
    define_native_function(realm, vm.names.transferToFixedLength, transfer_to_fixed_length, 0, attr);
    define_native_accessor(realm, vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(realm, vm.names.detached, detached_getter, {}, Attribute::Configurable);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), PrimitiveString::create(vm, vm.names.ArrayBuffer.as_string()), Attribute::Configurable);
}

// 25.1.5.3 ArrayBuffer.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-arraybuffer.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::slice)
{
    auto& realm = *vm.current_realm();

    auto start = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto array_buffer_object = TRY(typed_this_value(vm));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. If IsDetachedBuffer(O) is true, throw a TypeError exception.
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 5. Let len be O.[[ArrayBufferByteLength]].
    auto length = array_buffer_object->byte_length();

    // 6. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(start.to_integer_or_infinity(vm));

    double first;
    // 7. If relativeStart is -∞, let first be 0.
    if (Value(relative_start).is_negative_infinity())
        first = 0;
    // 8. Else if relativeStart < 0, let first be max(len + relativeStart, 0).
    else if (relative_start < 0)
        first = max(length + relative_start, 0.0);
    // 9. Else, let first be min(relativeStart, len).
    else
        first = min(relative_start, (double)length);

    // 10. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    auto relative_end = end.is_undefined() ? length : TRY(end.to_integer_or_infinity(vm));

    double final;
    // 11. If relativeEnd is -∞, let final be 0.
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    // 12. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
    else if (relative_end < 0)
        final = max(length + relative_end, 0.0);
    // 13. Else, let final be min(relativeEnd, len).
    else
        final = min(relative_end, (double)length);

    // 14. Let newLen be max(final - first, 0).
    auto new_length = max(final - first, 0.0);

    // 15. Let ctor be ? SpeciesConstructor(O, %ArrayBuffer%).
    auto* constructor = TRY(species_constructor(vm, array_buffer_object, realm.intrinsics().array_buffer_constructor()));

    // 16. Let new be ? Construct(ctor, « 𝔽(newLen) »).
    auto new_array_buffer = TRY(construct(vm, *constructor, Value(new_length)));

    // 17. Perform ? RequireInternalSlot(new, [[ArrayBufferData]]).
    if (!is<ArrayBuffer>(new_array_buffer.ptr()))
        return vm.throw_completion<TypeError>(ErrorType::SpeciesConstructorDidNotCreate, "an ArrayBuffer");
    auto* new_array_buffer_object = static_cast<ArrayBuffer*>(new_array_buffer.ptr());

    // 18. If IsSharedArrayBuffer(new) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 19. If IsDetachedBuffer(new) is true, throw a TypeError exception.
    if (new_array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(ErrorType::SpeciesConstructorReturned, "a detached ArrayBuffer");

    // 20. If SameValue(new, O) is true, throw a TypeError exception.
    if (same_value(new_array_buffer_object, array_buffer_object))
        return vm.throw_completion<TypeError>(ErrorType::SpeciesConstructorReturned, "same ArrayBuffer instance");

    // 21. If new.[[ArrayBufferByteLength]] < newLen, throw a TypeError exception.
    if (new_array_buffer_object->byte_length() < new_length)
        return vm.throw_completion<TypeError>(ErrorType::SpeciesConstructorReturned, "an ArrayBuffer smaller than requested");

    // 22. NOTE: Side-effects of the above steps may have detached O.
    // 23. If IsDetachedBuffer(O) is true, throw a TypeError exception.
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 24. Let fromBuf be O.[[ArrayBufferData]].
    auto& from_buf = array_buffer_object->buffer();

    // 25. Let toBuf be new.[[ArrayBufferData]].
    auto& to_buf = new_array_buffer_object->buffer();

    // 26. Perform CopyDataBlockBytes(toBuf, 0, fromBuf, first, newLen).
    copy_data_block_bytes(to_buf, 0, from_buf, first, new_length);

    // 27. Return new.
    return new_array_buffer_object;
}

// 25.1.5.1 get ArrayBuffer.prototype.byteLength, https://tc39.es/ecma262/#sec-get-arraybuffer.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::byte_length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto array_buffer_object = TRY(typed_this_value(vm));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // NOTE: These steps are done in byte_length()
    // 4. If IsDetachedBuffer(O) is true, return +0𝔽.
    // 5. Let length be O.[[ArrayBufferByteLength]].
    // 6. Return 𝔽(length).
    return Value(array_buffer_object->byte_length());
}

// 25.1.5.4 get ArrayBuffer.prototype.detached, https://tc39.es/proposal-arraybuffer-transfer/#sec-get-arraybuffer.prototype.detached
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::detached_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto array_buffer_object = TRY(typed_this_value(vm));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. Return IsDetachedBuffer(O).
    return Value(array_buffer_object->is_detached());
}

// 25.1.5.5 ArrayBuffer.prototype.transfer ( [ newLength ] ), https://tc39.es/proposal-arraybuffer-transfer/#sec-arraybuffer.prototype.transfer
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::transfer)
{
    // 1. Let O be the this value.
    auto array_buffer_object = TRY(typed_this_value(vm));

    // 2. Return ? ArrayBufferCopyAndDetach(O, newLength, preserve-resizability).
    auto new_length = vm.argument(0);
    return TRY(array_buffer_copy_and_detach(vm, array_buffer_object, new_length, PreserveResizability::PreserveResizability));
}

// 25.1.5.6 ArrayBuffer.prototype.transferToFixedLength ( [ newLength ] ), https://tc39.es/proposal-arraybuffer-transfer/#sec-arraybuffer.prototype.transfertofixedlength
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::transfer_to_fixed_length)
{
    // 1. Let O be the this value.
    auto array_buffer_object = TRY(typed_this_value(vm));

    // 2. Return ? ArrayBufferCopyAndDetach(O, newLength, fixed-length).
    auto new_length = vm.argument(0);
    return TRY(array_buffer_copy_and_detach(vm, array_buffer_object, new_length, PreserveResizability::FixedLength));
}

}
