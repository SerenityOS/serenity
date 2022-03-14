/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, Jamie Mansfield <jmansfield@cadixdev.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBufferPrototype::ArrayBufferPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ArrayBufferPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.slice, slice, 2, attr);
    define_native_function(vm.names.resize, resize, 1, attr);
    define_native_accessor(vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.maxByteLength, max_byte_length_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.resizable, resizable_getter, {}, Attribute::Configurable);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.ArrayBuffer.as_string()), Attribute::Configurable);
}

// 25.1.5.3 ArrayBuffer.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-arraybuffer.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::slice)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto* array_buffer_object = TRY(typed_this_value(global_object));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. If IsDetachedBuffer(O) is true, throw a TypeError exception.
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 5. Let len be O.[[ArrayBufferByteLength]].
    auto length = array_buffer_object->byte_length();

    // 6. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(vm.argument(0).to_integer_or_infinity(global_object));

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
    auto relative_end = vm.argument(1).is_undefined() ? length : TRY(vm.argument(1).to_integer_or_infinity(global_object));

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
    auto* constructor = TRY(species_constructor(global_object, *array_buffer_object, *global_object.array_buffer_constructor()));

    // 16. Let new be ? Construct(ctor, « 𝔽(newLen) »).
    auto* new_array_buffer = TRY(construct(global_object, *constructor, Value(new_length)));

    // 17. Perform ? RequireInternalSlot(new, [[ArrayBufferData]]).
    if (!is<ArrayBuffer>(new_array_buffer))
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorDidNotCreate, "an ArrayBuffer");
    auto* new_array_buffer_object = static_cast<ArrayBuffer*>(new_array_buffer);

    // 18. If IsSharedArrayBuffer(new) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 19. If IsDetachedBuffer(new) is true, throw a TypeError exception.
    if (new_array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "a detached ArrayBuffer");

    // 20. If SameValue(new, O) is true, throw a TypeError exception.
    if (same_value(new_array_buffer_object, array_buffer_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "same ArrayBuffer instance");

    // 21. If new.[[ArrayBufferByteLength]] < newLen, throw a TypeError exception.
    if (new_array_buffer_object->byte_length() < new_length)
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "an ArrayBuffer smaller than requested");

    // 22. NOTE: Side-effects of the above steps may have detached O.
    // 23. If IsDetachedBuffer(O) is true, throw a TypeError exception.
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 24. Let fromBuf be O.[[ArrayBufferData]].
    // 25. Let toBuf be new.[[ArrayBufferData]].
    // 26. Perform CopyDataBlockBytes(toBuf, 0, fromBuf, first, newLen).
    // FIXME: Implement this to specification
    array_buffer_object->buffer().span().slice(first, new_length).copy_to(new_array_buffer_object->buffer().span());

    // 27. Return new.
    return new_array_buffer_object;
}

// 1.3.5 ArrayBuffer.prototype.resize ( newLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-arraybuffer.prototype.resize
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::resize)
{
    // 1. Let O be the this value.
    auto* array_buffer_object = TRY(typed_this_value(global_object));

    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferMaxByteLength]]).
    if (!array_buffer_object->is_resizable_array_buffer())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAResizableArrayBuffer);

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. If IsDetachedBuffer(O) is true, throw a TypeError exception.
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 5. Let newByteLength be ? ToIntegerOrInfinity(newLength).
    auto new_byte_length = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    // 6. If newByteLength < 0 or newByteLength > O.[[ArrayBufferMaxByteLength]], throw a RangeError exception.
    if (new_byte_length < 0 || new_byte_length > array_buffer_object->max_byte_length())
        return vm.throw_completion<RangeError>(global_object, ErrorType::NewByteLengthOutOfRange);

    // 7. Let hostHandled be ? HostResizeArrayBuffer(O, newByteLength).
    auto host_handled = TRY(vm.host_resize_array_buffer(global_object, (size_t)new_byte_length));

    // 8. If hostHandled is handled, return undefined.
    if (host_handled == VM::HostResizeArrayBufferResult::Handled)
        return js_undefined();

    // 9. Let oldBlock be O.[[ArrayBufferData]].
    // 10. Let newBlock be ? CreateByteDataBlock(newByteLength).
    // 11. Let copyLength be min(newByteLength, O.[[ArrayBufferByteLength]]).
    // 12. Perform CopyDataBlockBytes(newBlock, 0, oldBlock, 0, copyLength).
    // 13. NOTE: Neither creation of the new Data Block nor copying from the old Data Block are observable. Implementations reserve the right to implement this method as in-place growth or shrinkage.
    // 14. Set O.[[ArrayBufferData]] to newBlock.
    auto old_byte_length = array_buffer_object->byte_length();
    if (array_buffer_object->buffer().try_resize((size_t)new_byte_length).is_error())
        return global_object.vm().throw_completion<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, new_byte_length);

    // Resizing an `AK::ByteBuffer` does not zero initialize any new capacity, but we want it to be anyway
    if (new_byte_length > old_byte_length) {
        // This performs bounds checking whereas a raw `memset` call would not be
        array_buffer_object->buffer().bytes().slice(old_byte_length).fill(0);
    }

    // 15. Set O.[[ArrayBufferByteLength]] to newLength.
    // This value is managed by the `AK::ByteBuffer` internally, it already has its new value

    // 16. Return undefined.
    return js_undefined();
}

// 25.1.5.1 get ArrayBuffer.prototype.byteLength, https://tc39.es/ecma262/#sec-get-arraybuffer.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::byte_length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto* array_buffer_object = TRY(typed_this_value(global_object));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. If IsDetachedBuffer(O) is true, return +0𝔽.
    if (array_buffer_object->is_detached())
        return Value(0);

    // 5. Let length be O.[[ArrayBufferByteLength]].
    auto length = array_buffer_object->byte_length();

    // 6. Return 𝔽(length).
    return Value(length);
}

// 1.3.2 get ArrayBuffer.prototype.maxByteLength, https://tc39.es/proposal-resizablearraybuffer/#sec-get-arraybuffer.prototype.maxbytelength
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::max_byte_length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto* array_buffer_object = TRY(typed_this_value(global_object));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. If IsDetachedBuffer(O) is true, return +0𝔽.
    if (array_buffer_object->is_detached())
        return Value(0);

    // 5. If IsResizableArrayBuffer(O) is true, then
    if (array_buffer_object->is_resizable_array_buffer()) {
        // a. Let length be O.[[ArrayBufferMaxByteLength]].
        return array_buffer_object->max_byte_length();
    }

    // 6. Else
    //     a. Let length be O.[[ArrayBufferByteLength]].
    // 7. Return 𝔽(length).
    return array_buffer_object->byte_length();
}

// 1.3.3 get ArrayBuffer.prototype.resizable, https://tc39.es/proposal-resizablearraybuffer/#sec-get-arraybuffer.prototype.resizable
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::resizable_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]]).
    auto* array_buffer_object = TRY(typed_this_value(global_object));

    // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception.
    // FIXME: Check for shared buffer

    // 4. Return IsResizableArrayBuffer(O).
    return array_buffer_object->is_resizable_array_buffer();
}

}
