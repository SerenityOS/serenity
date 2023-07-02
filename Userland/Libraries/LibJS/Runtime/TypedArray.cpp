/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

ThrowCompletionOr<TypedArrayBase*> typed_array_from(VM& vm, Value typed_array_value)
{
    auto this_object = TRY(typed_array_value.to_object(vm));
    if (!this_object->is_typed_array())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObjectOfType, "TypedArray");

    return static_cast<TypedArrayBase*>(this_object.ptr());
}

// 23.2.4.4 ValidateTypedArray ( O ), https://tc39.es/proposal-resizablearraybuffer/#sec-validatetypedarray
ThrowCompletionOr<void> validate_typed_array(VM& vm, TypedArrayBase& typed_array)
{
    // 1. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    if (!typed_array.is_typed_array())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObjectOfType, "TypedArray");

    // 2. Assert: O has a [[ViewedArrayBuffer]] internal slot.

    // 3. Let buffer be O.[[ViewedArrayBuffer]].
    static_cast<void>(typed_array.viewed_array_buffer());

    // 4. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 5. If IsIntegerIndexedObjectOutOfBounds(O, getBufferByteLength) is true, throw a TypeError exception.
    if (is_integer_indexed_object_out_of_bounds(vm, typed_array, get_buffer_byte_length))
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "validate_typed_array");

    // 6. Return unused
    return {};
}

// 23.2.5.1.3 InitializeTypedArrayFromArrayBuffer ( O, buffer, byteOffset, length ), https://tc39.es/proposal-resizablearraybuffer/#sec-initializetypedarrayfromarraybuffer
static ThrowCompletionOr<void> initialize_typed_array_from_array_buffer(VM& vm, TypedArrayBase& typed_array, ArrayBuffer& array_buffer, Value byte_offset, Value length)
{
    // 1. Let elementSize be TypedArrayElementSize(O).
    auto element_size = typed_array.element_size();

    // 2. Let offset be ? ToIndex(byteOffset).
    auto offset = TRY(byte_offset.to_index(vm));

    // 3. If offset modulo elementSize ≠ 0, throw a RangeError exception.
    if (offset % element_size != 0)
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidByteOffset, typed_array.class_name(), element_size, offset);

    // 4. Let bufferIsResizable be IsResizableArrayBuffer(buffer).
    auto buffer_is_resizable = array_buffer.is_resizable();

    size_t new_length { 0 };

    // 5. If length is not undefined, then
    if (!length.is_undefined()) {
        // a. Let newLength be ? ToIndex(length).
        new_length = TRY(length.to_index(vm));
    }

    // 6. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (array_buffer.is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 7. Let bufferByteLength be ArrayBufferByteLength(buffer, SeqCst).
    auto buffer_byte_length = array_buffer_byte_length(vm, array_buffer, ArrayBuffer::Order::SeqCst);

    Checked<size_t> new_byte_length;

    // 8. If length is undefined and bufferIsResizable is true, then
    if (length.is_undefined() && buffer_is_resizable) {
        // a. If offset > bufferByteLength, throw a RangeError exception.
        if (offset > buffer_byte_length)
            return vm.throw_completion<RangeError>(ErrorType::TypedArrayOutOfRangeByteOffset, offset, buffer_byte_length);

        // a. Set O.[[ByteLength]] to auto.
        typed_array.set_byte_length_to_auto();

        // c. Set O.[[ArrayLength]] to auto.
        typed_array.set_array_length_to_auto();
    }
    // 9. Else,
    else {
        // a. If length is undefined, then
        if (length.is_undefined()) {
            // i. If bufferByteLength modulo elementSize ≠ 0, throw a RangeError exception.
            if (buffer_byte_length % element_size != 0)
                return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidBufferLength, typed_array.class_name(), element_size, buffer_byte_length);

            // ii. Let newByteLength be bufferByteLength - offset.
            new_byte_length = buffer_byte_length;
            new_byte_length -= offset;

            // NOTE: The overflow check handles this step
            // iii. If newByteLength < 0, throw a RangeError exception.
            if (new_byte_length.has_overflow())
                return vm.throw_completion<RangeError>(ErrorType::TypedArrayOutOfRangeByteOffset, offset, buffer_byte_length);
        }
        // b. Else,
        else {
            // i. Let newByteLength be newLength × elementSize.
            new_byte_length = new_length;
            new_byte_length *= element_size;

            // ii. If offset + newByteLength > bufferByteLength, throw a RangeError exception.
            Checked<size_t> new_byte_end = new_byte_length;
            new_byte_end += offset;

            if (new_byte_end.has_overflow())
                return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");

            if (new_byte_end.value() > buffer_byte_length)
                return vm.throw_completion<RangeError>(ErrorType::TypedArrayOutOfRangeByteOffsetOrLength, offset, new_byte_end.value(), buffer_byte_length);
        }

        // c. Set O.[[ByteLength]] to newByteLength.
        typed_array.set_byte_length(new_byte_length.value());

        // d. Set O.[[ArrayLength]] to newByteLength / elementSize
        typed_array.set_array_length(new_byte_length.value() / element_size);
    }

    // 10. Set O.[[ViewedArrayBuffer]] to buffer.
    typed_array.set_viewed_array_buffer(&array_buffer);

    // 11. Set O.[[ByteOffset]] to offset.
    typed_array.set_byte_offset(offset);

    // 13. Return unused.
    return {};
}

// 23.2.5.1.2 InitializeTypedArrayFromTypedArray ( O, srcArray ), https://tc39.es/proposal-resizablearraybuffer/#sec-initializetypedarrayfromtypedarray
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_typed_array(VM& vm, TypedArray<T>& dest_array, TypedArrayBase& src_array)
{
    auto& realm = *vm.current_realm();

    // 1. Let srcData be srcArray.[[ViewedArrayBuffer]].
    auto* src_data = src_array.viewed_array_buffer();
    VERIFY(src_data);

    // 2. Let elementType be TypedArrayElementType(O).
    // 3. Let elementSize be TypedArrayElementSize(O).
    auto element_size = dest_array.element_size();

    // 4. Let getSrcBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_src_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 5. Let elementLength be IntegerIndexedObjectLength(srcArray, getSrcBufferByteLength).
    auto element_length = integer_indexed_object_length(vm, src_array, get_src_buffer_byte_length);

    // 6. If elementLength is out-of-bounds, throw a TypeError exception.
    if (!element_length.has_value())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "element length");

    // 7. Let srcType be TypedArrayElementType(srcArray).
    // 8. Let srcElementSize be TypedArrayElementSize(srcArray).
    auto src_element_size = src_array.element_size();

    // 9. Let srcByteOffset be srcArray.[[ByteOffset]].
    auto src_byte_offset = src_array.byte_offset();

    // 10. Let byteLength be elementSize × elementLength.
    Checked<size_t> byte_length = element_size;
    byte_length *= element_length.value();
    if (byte_length.has_overflow())
        return vm.template throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");

    ArrayBuffer* data = nullptr;

    // 11. If elementType is the same as srcType, then
    if (dest_array.element_name() == src_array.element_name()) {
        // a. Let data be ? CloneArrayBuffer(srcData, srcByteOffset, byteLength).
        data = TRY(clone_array_buffer(vm, *src_data, src_byte_offset, byte_length.value()));
    }
    // 12. Else,
    else {
        // a. Let data be ? AllocateArrayBuffer(bufferConstructor, byteLength).
        data = TRY(allocate_array_buffer(vm, realm.intrinsics().array_buffer_constructor(), byte_length.value()));

        // b. If srcArray.[[ContentType]] ≠ O.[[ContentType]], throw a TypeError exception.
        if (src_array.content_type() != dest_array.content_type())
            return vm.template throw_completion<TypeError>(ErrorType::TypedArrayContentTypeMismatch, dest_array.class_name(), src_array.class_name());

        // c. Let srcByteIndex be srcByteOffset.
        u64 src_byte_index = src_byte_offset;

        // d. Let targetByteIndex be 0.
        u64 target_byte_index = 0;

        // e. Let count be elementLength.
        // f. Repeat, while count > 0,
        for (u32 i = 0; i < element_length.value(); ++i) {
            // i. Let value be GetValueFromBuffer(srcData, srcByteIndex, srcType, true, Unordered).
            auto value = src_array.get_value_from_buffer(src_byte_index, ArrayBuffer::Order::Unordered);

            // ii. Perform SetValueInBuffer(data, targetByteIndex, elementType, value, true, Unordered).
            data->template set_value<T>(target_byte_index, value, true, ArrayBuffer::Order::Unordered);

            // iii. Set srcByteIndex to srcByteIndex + srcElementSize.
            src_byte_index += src_element_size;

            // iv. Set targetByteIndex to targetByteIndex + elementSize.
            target_byte_index += element_size;

            // v. Set count to count - 1.
        }
    }

    // 13. Set O.[[ViewedArrayBuffer]] to data.
    dest_array.set_viewed_array_buffer(data);

    // 14. Set O.[[ByteLength]] to byteLength.
    dest_array.set_byte_length(byte_length.value());

    // 15. Set O.[[ByteOffset]] to 0.
    dest_array.set_byte_offset(0);

    // 16. Set O.[[ArrayLength]] to elementLength.
    dest_array.set_array_length(element_length.value());

    // 17. Return unused.
    return {};
}

// 23.2.5.1.6 AllocateTypedArrayBuffer ( O, length ), https://tc39.es/ecma262/#sec-allocatetypedarraybuffer
template<typename T>
static ThrowCompletionOr<void> allocate_typed_array_buffer(VM& vm, TypedArray<T>& typed_array, size_t length)
{
    auto& realm = *vm.current_realm();

    // Enforce 2GB "Excessive Length" limit
    if (length > NumericLimits<i32>::max() / sizeof(T))
        return vm.template throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");

    // 1. Assert: O.[[ViewedArrayBuffer]] is undefined.

    // 2. Let elementSize be TypedArrayElementSize(O).
    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, length))
        return vm.template throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");

    // 3. Let byteLength be elementSize × length.
    auto byte_length = element_size * length;

    // 4. Let data be ? AllocateArrayBuffer(%ArrayBuffer%, byteLength).
    auto* data = TRY(allocate_array_buffer(vm, realm.intrinsics().array_buffer_constructor(), byte_length));

    // 5. Set O.[[ViewedArrayBuffer]] to data.
    typed_array.set_viewed_array_buffer(data);

    // 6. Set O.[[ByteLength]] to byteLength.
    typed_array.set_byte_length(byte_length);

    // 7. Set O.[[ByteOffset]] to 0.
    typed_array.set_byte_offset(0);

    // 8. Set O.[[ArrayLength]] to length.
    typed_array.set_array_length(length);

    // 9. Return unused.
    return {};
}

// 23.2.5.1.5 InitializeTypedArrayFromArrayLike, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraylike
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_array_like(VM& vm, TypedArray<T>& typed_array, Object const& array_like)
{
    // 1. Let len be ? LengthOfArrayLike(arrayLike).
    auto length = TRY(length_of_array_like(vm, array_like));

    // 2. Perform ? AllocateTypedArrayBuffer(O, len).
    TRY(allocate_typed_array_buffer(vm, typed_array, length));

    // 3. Let k be 0.
    // 4. Repeat, while k < len,
    for (size_t k = 0; k < length; k++) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ? Get(arrayLike, Pk).
        auto k_value = TRY(array_like.get(k));

        // c. Perform ? Set(O, Pk, kValue, true).
        TRY(typed_array.set(k, k_value, Object::ShouldThrowExceptions::Yes));

        // d. Set k to k + 1.
    }

    // 5. Return unused.
    return {};
}

// 23.2.5.1.4 InitializeTypedArrayFromList, https://tc39.es/ecma262/#sec-initializetypedarrayfromlist
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_list(VM& vm, TypedArray<T>& typed_array, MarkedVector<Value> const& list)
{
    // 1. Let len be the number of elements in values.
    auto length = list.size();

    // 2. Perform ? AllocateTypedArrayBuffer(O, len).
    TRY(allocate_typed_array_buffer(vm, typed_array, length));

    // 3. Let k be 0.
    // 4. Repeat, while k < len,
    for (size_t k = 0; k < length; k++) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be the first element of values and remove that element from values.
        auto value = list[k];

        // c. Perform ? Set(O, Pk, kValue, true).
        TRY(typed_array.set(k, value, Object::ShouldThrowExceptions::Yes));

        // d. Set k to k + 1.
    }

    // 5. Assert: values is now an empty List.
    // 6. Return unused.
    return {};
}

// 23.2.4.2 TypedArrayCreate ( constructor, argumentList ), https://tc39.es/ecma262/#typedarray-create
ThrowCompletionOr<TypedArrayBase*> typed_array_create(VM& vm, FunctionObject& constructor, MarkedVector<Value> arguments)
{
    Optional<Value> first_argument;
    if (!arguments.is_empty())
        first_argument = arguments[0];
    // 1. Let newTypedArray be ? Construct(constructor, argumentList).
    auto new_typed_array = TRY(construct(vm, constructor, move(arguments)));

    // 2. Perform ? ValidateTypedArray(newTypedArray).
    if (!new_typed_array->is_typed_array())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObjectOfType, "TypedArray");
    auto& typed_array = *static_cast<TypedArrayBase*>(new_typed_array.ptr());
    TRY(validate_typed_array(vm, typed_array));

    // 3. If argumentList is a List of a single Number, then
    if (first_argument.has_value() && first_argument->is_number()) {
        // a. If newTypedArray.[[ArrayLength]] < ℝ(argumentList[0]), throw a TypeError exception.
        if (typed_array.idempotent_array_length() < first_argument->as_double())
            return vm.throw_completion<TypeError>(ErrorType::InvalidLength, "typed array");
    }

    // 4. Return newTypedArray.
    return &typed_array;
}

// 23.2.4.3 TypedArrayCreateSameType ( exemplar, argumentList ), https://tc39.es/ecma262/#sec-typedarray-create-same-type
ThrowCompletionOr<TypedArrayBase*> typed_array_create_same_type(VM& vm, TypedArrayBase const& exemplar, MarkedVector<Value> arguments)
{
    auto& realm = *vm.current_realm();

    // 1. Let constructor be the intrinsic object associated with the constructor name exemplar.[[TypedArrayName]] in Table 68.
    auto constructor = (realm.intrinsics().*exemplar.intrinsic_constructor())();

    // 2. Let result be ? TypedArrayCreate(constructor, argumentList).
    auto* result = TRY(typed_array_create(vm, constructor, move(arguments)));

    // 3. Assert: result has [[TypedArrayName]] and [[ContentType]] internal slots.
    // 4. Assert: result.[[ContentType]] is exemplar.[[ContentType]].
    // 5. Return result.
    return result;
}

// 23.2.4.7 CompareTypedArrayElements ( x, y, comparefn ), https://tc39.es/ecma262/#sec-typedarray-create-same-type
ThrowCompletionOr<double> compare_typed_array_elements(VM& vm, Value x, Value y, FunctionObject* comparefn)
{
    // 1. Assert: x is a Number and y is a Number, or x is a BigInt and y is a BigInt.
    VERIFY(((x.is_number() && y.is_number()) || (x.is_bigint() && y.is_bigint())));

    // 2. If comparefn is not undefined, then
    if (comparefn != nullptr) {
        // a. Let v be ? ToNumber(? Call(comparefn, undefined, « x, y »)).
        auto value = TRY(call(vm, comparefn, js_undefined(), x, y));
        auto value_number = TRY(value.to_number(vm));

        // b. If v is NaN, return +0𝔽.
        if (value_number.is_nan())
            return 0;

        // c. Return v.
        return value_number.as_double();
    }

    // 3. If x and y are both NaN, return +0𝔽.
    if (x.is_nan() && y.is_nan())
        return 0;

    // 4. If x is NaN, return 1𝔽.
    if (x.is_nan())
        return 1;

    // 5. If y is NaN, return -1𝔽.
    if (y.is_nan())
        return -1;

    // 6. If x < y, return -1𝔽.
    if (x.is_bigint()
            ? (x.as_bigint().big_integer() < y.as_bigint().big_integer())
            : (x.as_double() < y.as_double()))
        return -1;

    // 7. If x > y, return 1𝔽.
    if (x.is_bigint()
            ? (x.as_bigint().big_integer() > y.as_bigint().big_integer())
            : (x.as_double() > y.as_double()))
        return 1;

    // 8. If x is -0𝔽 and y is +0𝔽, return -1𝔽.
    if (x.is_negative_zero() && y.is_positive_zero())
        return -1;

    // 9. If x is +0𝔽 and y is -0𝔽, return 1𝔽.
    if (x.is_positive_zero() && y.is_negative_zero())
        return 1;

    // 10. Return +0𝔽.
    return 0;
}

// 3.3 IntegerIndexedObjectByteLength ( O, getBufferByteLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-integerindexedobjectbytelength
size_t integer_indexed_object_byte_length(VM& vm, TypedArrayBase const& typed_array, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length)
{
    // 1. Let length be IntegerIndexedObjectLength(O, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, typed_array, get_buffer_byte_length);

    // 2. If length is out-of-bounds or length = 0, return 0.
    if (!length.has_value() || length.value() == 0)
        return 0;

    // 3. If O.[[ByteLength]] is not auto, return O.[[ByteLength]].
    if (!typed_array.is_byte_length_auto())
        return typed_array.byte_length().value();

    // 4. Let elementSize be TypedArrayElementSize(O).
    auto element_size = typed_array.element_size();

    // 5. Return length × elementSize.
    Checked<size_t> object_byte_length = length.value();
    object_byte_length *= element_size;

    // FIXME: Not exactly sure what we should do when overflow occurs.
    //        Just return 0 as if it succeeded for now.
    if (object_byte_length.has_overflow()) {
        dbgln("integer_indexed_object_byte_length(): object_byte_length overflowed, returning as if succeeded.");
        return 0;
    }

    return object_byte_length.value();
}

// 3.4 IntegerIndexedObjectLength ( O, getBufferByteLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-integerindexedobjectbytelength
Optional<size_t> integer_indexed_object_length(VM& vm, TypedArrayBase const& typed_array, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length)
{
    // 1. If IsIntegerIndexedObjectOutOfBounds(O, getBufferByteLength) is true, return out-of-bounds.
    if (is_integer_indexed_object_out_of_bounds(vm, typed_array, get_buffer_byte_length))
        return {};

    // 2. If O.[[ArrayLength]] is not auto, return O.[[ArrayLength]].
    if (!typed_array.is_array_length_auto())
        return typed_array.array_length().value();

    // 3. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array.viewed_array_buffer();

    // 4. Let bufferByteLength be getBufferByteLength(buffer).
    auto buffer_byte_length = get_buffer_byte_length(vm, *buffer);

    // 5. Assert: IsResizableArrayBuffer(buffer) is true.
    VERIFY(buffer->is_resizable());

    // 6. Let byteOffset be O.[[ByteOffset]].
    auto byte_offset = typed_array.byte_offset();

    // 7. Let elementSize be TypedArrayElementSize(O).
    auto element_size = typed_array.element_size();

    // NOTE: Integer division implicitly floors
    // 8. Return floor((bufferByteLength - byteOffset) / elementSize).
    return (buffer_byte_length - byte_offset) / element_size;
}

// 3.5 IsIntegerIndexedObjectOutOfBounds ( O, getBufferByteLength ), https://tc39.es/proposal-resizablearraybuffer/#sec-integerindexedobjectbytelength
bool is_integer_indexed_object_out_of_bounds(VM& vm, TypedArrayBase const& typed_array, IdempotentArrayBufferByteLengthGetter& get_buffer_byte_length)
{
    // 1. Let buffer be O.[[ViewedArrayBuffer]].
    auto buffer = typed_array.viewed_array_buffer();

    // 2. If IsDetachedBuffer(O.[[ViewedArrayBuffer]]) is true, return true.
    if (buffer->is_detached())
        return true;

    // 3. Let bufferByteLength be getBufferByteLength(buffer).
    auto buffer_byte_length = get_buffer_byte_length(vm, *buffer);

    // 4. Let byteOffsetStart be O.[[ByteOffset]].
    auto byte_offset_start = typed_array.byte_offset();

    // 5. If O.[[ArrayLength]] is auto, then
    Checked<size_t> byte_offset_end;
    if (typed_array.is_array_length_auto()) {
        // a. Let byteOffsetEnd be bufferByteLength.
        byte_offset_end = buffer_byte_length;
    }
    // 6. Else,
    else {
        // a. Let elementSize be TypedArrayElementSize(O).
        auto element_size = typed_array.element_size();

        // b. Let byteOffsetEnd be byteOffsetStart + O.[[ArrayLength]] × elementSize.
        byte_offset_end = typed_array.array_length().value();
        byte_offset_end *= element_size;
        byte_offset_end += byte_offset_start;

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return true as if it succeeded for now.
        if (byte_offset_end.has_overflow()) {
            dbgln("is_integer_indexed_object_out_of_bounds(): byte_offset_end overflowed, returning as if succeeded.");
            return true;
        }
    }

    // 7. If byteOffsetStart > bufferByteLength or byteOffsetEnd > bufferByteLength, return true.
    if (byte_offset_start > buffer_byte_length || byte_offset_end > buffer_byte_length)
        return true;

    // 8. NOTE: 0-length TypedArrays are not considered out-of-bounds.
    // 9. Return false.
    return false;
}

// 3.6 IsArrayBufferViewOutOfBounds ( O ), https://tc39.es/proposal-resizablearraybuffer/#sec-isarraybufferviewoutofbounds
bool is_array_buffer_view_out_of_bounds(VM& vm, TypedArrayBase const& typed_array)
{
    // 1. Let buffer be O.[[ViewedArrayBuffer]].
    auto buffer = typed_array.viewed_array_buffer();

    // 2. If IsDetachedBuffer(buffer) is true, return true.
    if (buffer->is_detached())
        return true;

    // 3. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // FIXME: 4. If IsSharedArrayBuffer(buffer) is true, then
    // a. Assert: If O has a [[DataView]] internal slot, IsViewOutOfBounds(O, getBufferByteLength) is false. Else, IsIntegerIndexedObjectOutOfBounds(O, getBufferByteLength) is false.
    // b. NOTE: SharedArrayBuffers can only grow, and views on it cannot go out of bounds after construction. This is special-cased in this operation to avoid shared memory loads of the buffer's byte length, which are not necessary for this check.
    // c. Return false.

    // FIXME: 5. If O has a [[DataView]] internal slot, return IsViewOutOfBounds(O, getBufferByteLength).

    // 6. Return IsIntegerIndexedObjectOutOfBounds(O, getBufferByteLength).
    return is_integer_indexed_object_out_of_bounds(vm, typed_array, get_buffer_byte_length);
}

void TypedArrayBase::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                                                                                             \
    ThrowCompletionOr<NonnullGCPtr<ClassName>> ClassName::create(Realm& realm, u32 length, FunctionObject& new_target)                                                                 \
    {                                                                                                                                                                                  \
        auto* prototype = TRY(get_prototype_from_constructor(realm.vm(), new_target, &Intrinsics::snake_name##_prototype));                                                            \
        auto array_buffer = TRY(ArrayBuffer::create(realm, length * sizeof(UnderlyingBufferDataType)));                                                                                \
        return MUST_OR_THROW_OOM(realm.heap().allocate<ClassName>(realm, *prototype, length, *array_buffer));                                                                          \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ThrowCompletionOr<NonnullGCPtr<ClassName>> ClassName::create(Realm& realm, u32 length)                                                                                             \
    {                                                                                                                                                                                  \
        auto array_buffer = TRY(ArrayBuffer::create(realm, length * sizeof(UnderlyingBufferDataType)));                                                                                \
        return create(realm, length, *array_buffer);                                                                                                                                   \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    NonnullGCPtr<ClassName> ClassName::create(Realm& realm, u32 length, ArrayBuffer& array_buffer)                                                                                     \
    {                                                                                                                                                                                  \
        return realm.heap().allocate<ClassName>(realm, realm.intrinsics().snake_name##_prototype(), length, array_buffer).release_allocated_value_but_fixme_should_propagate_errors(); \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ClassName::ClassName(Object& prototype, u32 length, ArrayBuffer& array_buffer)                                                                                                     \
        : TypedArray(prototype,                                                                                                                                                        \
            bit_cast<TypedArrayBase::IntrinsicConstructor>(&Intrinsics::snake_name##_constructor), length, array_buffer)                                                               \
    {                                                                                                                                                                                  \
        if constexpr (#ClassName##sv.is_one_of("BigInt64Array", "BigUint64Array"))                                                                                                     \
            m_content_type = ContentType::BigInt;                                                                                                                                      \
        else                                                                                                                                                                           \
            m_content_type = ContentType::Number;                                                                                                                                      \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ClassName::~ClassName()                                                                                                                                                            \
    {                                                                                                                                                                                  \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    DeprecatedFlyString const& ClassName::element_name() const                                                                                                                         \
    {                                                                                                                                                                                  \
        return vm().names.ClassName.as_string();                                                                                                                                       \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    PrototypeName::PrototypeName(Object& prototype)                                                                                                                                    \
        : Object(ConstructWithPrototypeTag::Tag, prototype)                                                                                                                            \
    {                                                                                                                                                                                  \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    PrototypeName::~PrototypeName()                                                                                                                                                    \
    {                                                                                                                                                                                  \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ThrowCompletionOr<void> PrototypeName::initialize(Realm& realm)                                                                                                                    \
    {                                                                                                                                                                                  \
        auto& vm = this->vm();                                                                                                                                                         \
        MUST_OR_THROW_OOM(Base::initialize(realm));                                                                                                                                    \
        define_direct_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                                                                               \
                                                                                                                                                                                       \
        return {};                                                                                                                                                                     \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ConstructorName::ConstructorName(Realm& realm, Object& prototype)                                                                                                                  \
        : TypedArrayConstructor(realm.vm().names.ClassName.as_string(), prototype)                                                                                                     \
    {                                                                                                                                                                                  \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ConstructorName::~ConstructorName()                                                                                                                                                \
    {                                                                                                                                                                                  \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    ThrowCompletionOr<void> ConstructorName::initialize(Realm& realm)                                                                                                                  \
    {                                                                                                                                                                                  \
        auto& vm = this->vm();                                                                                                                                                         \
        MUST_OR_THROW_OOM(NativeFunction::initialize(realm));                                                                                                                          \
                                                                                                                                                                                       \
        /* 23.2.6.2 TypedArray.prototype, https://tc39.es/ecma262/#sec-typedarray.prototype */                                                                                         \
        define_direct_property(vm.names.prototype, realm.intrinsics().snake_name##_prototype(), 0);                                                                                    \
                                                                                                                                                                                       \
        /* 23.2.6.1 TypedArray.BYTES_PER_ELEMENT, https://tc39.es/ecma262/#sec-typedarray.bytes_per_element */                                                                         \
        define_direct_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                                                                               \
                                                                                                                                                                                       \
        define_direct_property(vm.names.length, Value(3), Attribute::Configurable);                                                                                                    \
                                                                                                                                                                                       \
        return {};                                                                                                                                                                     \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    /* 23.2.5.1 TypedArray ( ...args ), https://tc39.es/ecma262/#sec-typedarray */                                                                                                     \
    ThrowCompletionOr<Value> ConstructorName::call()                                                                                                                                   \
    {                                                                                                                                                                                  \
        auto& vm = this->vm();                                                                                                                                                         \
        return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, vm.names.ClassName);                                                                                   \
    }                                                                                                                                                                                  \
                                                                                                                                                                                       \
    /* 23.2.5.1 TypedArray ( ...args ), https://tc39.es/ecma262/#sec-typedarray */                                                                                                     \
    ThrowCompletionOr<NonnullGCPtr<Object>> ConstructorName::construct(FunctionObject& new_target)                                                                                     \
    {                                                                                                                                                                                  \
        auto& vm = this->vm();                                                                                                                                                         \
        auto& realm = *vm.current_realm();                                                                                                                                             \
                                                                                                                                                                                       \
        if (vm.argument_count() == 0)                                                                                                                                                  \
            return TRY(ClassName::create(realm, 0, new_target));                                                                                                                       \
                                                                                                                                                                                       \
        auto first_argument = vm.argument(0);                                                                                                                                          \
        if (first_argument.is_object()) {                                                                                                                                              \
            auto typed_array = TRY(ClassName::create(realm, 0, new_target));                                                                                                           \
            if (first_argument.as_object().is_typed_array()) {                                                                                                                         \
                auto& arg_typed_array = static_cast<TypedArrayBase&>(first_argument.as_object());                                                                                      \
                TRY(initialize_typed_array_from_typed_array(vm, *typed_array, arg_typed_array));                                                                                       \
            } else if (is<ArrayBuffer>(first_argument.as_object())) {                                                                                                                  \
                auto& array_buffer = static_cast<ArrayBuffer&>(first_argument.as_object());                                                                                            \
                TRY(initialize_typed_array_from_array_buffer(vm, *typed_array, array_buffer,                                                                                           \
                    vm.argument(1), vm.argument(2)));                                                                                                                                  \
            } else {                                                                                                                                                                   \
                auto iterator = TRY(first_argument.get_method(vm, vm.well_known_symbol_iterator()));                                                                                   \
                if (iterator) {                                                                                                                                                        \
                    auto values = TRY(iterable_to_list(vm, first_argument, iterator));                                                                                                 \
                    TRY(initialize_typed_array_from_list(vm, *typed_array, values));                                                                                                   \
                } else {                                                                                                                                                               \
                    TRY(initialize_typed_array_from_array_like(vm, *typed_array, first_argument.as_object()));                                                                         \
                }                                                                                                                                                                      \
            }                                                                                                                                                                          \
            return typed_array;                                                                                                                                                        \
        }                                                                                                                                                                              \
                                                                                                                                                                                       \
        auto array_length_or_error = first_argument.to_index(vm);                                                                                                                      \
        if (array_length_or_error.is_error()) {                                                                                                                                        \
            auto error = array_length_or_error.release_error();                                                                                                                        \
            if (error.value()->is_object() && is<RangeError>(error.value()->as_object())) {                                                                                            \
                /* Re-throw more specific RangeError */                                                                                                                                \
                return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");                                                                                       \
            }                                                                                                                                                                          \
            return error;                                                                                                                                                              \
        }                                                                                                                                                                              \
        auto array_length = array_length_or_error.release_value();                                                                                                                     \
        if (array_length > NumericLimits<i32>::max() / sizeof(Type))                                                                                                                   \
            return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");                                                                                           \
        /* FIXME: What is the best/correct behavior here? */                                                                                                                           \
        if (Checked<u32>::multiplication_would_overflow(array_length, sizeof(Type)))                                                                                                   \
            return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "typed array");                                                                                           \
        return TRY(ClassName::create(realm, array_length, new_target));                                                                                                                \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
