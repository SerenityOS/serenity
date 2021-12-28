/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

ThrowCompletionOr<TypedArrayBase*> typed_array_from(GlobalObject& global_object, Value typed_array_value)
{
    auto& vm = global_object.vm();

    auto* this_object = TRY(typed_array_value.to_object(global_object));
    if (!this_object->is_typed_array())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");

    return static_cast<TypedArrayBase*>(this_object);
}

// 23.2.4.3 ValidateTypedArray ( O ), https://tc39.es/ecma262/#sec-validatetypedarray
ThrowCompletionOr<void> validate_typed_array(GlobalObject& global_object, TypedArrayBase& typed_array)
{
    auto& vm = global_object.vm();

    // 1. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    if (!typed_array.is_typed_array())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");

    // 2. Assert: O has a [[ViewedArrayBuffer]] internal slot.

    // 3. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array.viewed_array_buffer();

    // 4. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    return {};
}

// 22.2.5.1.3 InitializeTypedArrayFromArrayBuffer, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraybuffer
static ThrowCompletionOr<void> initialize_typed_array_from_array_buffer(GlobalObject& global_object, TypedArrayBase& typed_array, ArrayBuffer& array_buffer, Value byte_offset, Value length)
{
    auto& vm = global_object.vm();

    // 1. Let constructorName be the String value of O.[[TypedArrayName]].

    // 2. Let elementSize be the Element Size value specified in Table 72 for constructorName.
    auto element_size = typed_array.element_size();

    // 3. Let offset be ? ToIndex(byteOffset).
    auto offset = TRY(byte_offset.to_index(global_object));

    // 4. If offset modulo elementSize ≠ 0, throw a RangeError exception.
    if (offset % element_size != 0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayInvalidByteOffset, typed_array.class_name(), element_size, offset);

    size_t new_length { 0 };

    // 5. If length is not undefined, then
    if (!length.is_undefined()) {
        // a. Let newLength be ? ToIndex(length).
        new_length = TRY(length.to_index(global_object));
    }

    // 6. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (array_buffer.is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 7. Let bufferByteLength be buffer.[[ArrayBufferByteLength]].
    auto buffer_byte_length = array_buffer.byte_length();

    Checked<size_t> new_byte_length;

    // 8. If length is undefined, then
    if (length.is_undefined()) {
        // a. If bufferByteLength modulo elementSize ≠ 0, throw a RangeError exception.
        if (buffer_byte_length % element_size != 0)
            return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayInvalidBufferLength, typed_array.class_name(), element_size, buffer_byte_length);

        // b. Let newByteLength be bufferByteLength - offset.
        // c. If newByteLength < 0, throw a RangeError exception.
        if (offset > buffer_byte_length)
            return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffset, offset, buffer_byte_length);
        new_byte_length = buffer_byte_length;
        new_byte_length -= offset;
    }
    // 9. Else,
    else {
        // a. Let newByteLength be newLength × elementSize.
        new_byte_length = new_length;
        new_byte_length *= element_size;

        // b. If offset + newByteLength > bufferByteLength, throw a RangeError exception.
        Checked<size_t> new_byte_end = new_byte_length;
        new_byte_end += offset;

        if (new_byte_end.has_overflow())
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

        if (new_byte_end.value() > buffer_byte_length)
            return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffsetOrLength, offset, new_byte_end.value(), buffer_byte_length);
    }

    if (new_byte_length.has_overflow())
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

    // 10. Set O.[[ViewedArrayBuffer]] to buffer.
    typed_array.set_viewed_array_buffer(&array_buffer);

    // 11. Set O.[[ByteLength]] to newByteLength.
    typed_array.set_byte_length(new_byte_length.value());

    // 12. Set O.[[ByteOffset]] to offset.
    typed_array.set_byte_offset(offset);

    // 13. Set O.[[ArrayLength]] to newByteLength / elementSize.
    typed_array.set_array_length(new_byte_length.value() / element_size);

    return {};
}

// 23.2.5.1.2 InitializeTypedArrayFromTypedArray ( O, srcArray ), https://tc39.es/ecma262/#sec-initializetypedarrayfromtypedarray
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_typed_array(GlobalObject& global_object, TypedArray<T>& dest_array, TypedArrayBase& src_array)
{
    auto& vm = global_object.vm();

    // 1. Let srcData be srcArray.[[ViewedArrayBuffer]].
    auto* src_data = src_array.viewed_array_buffer();
    VERIFY(src_data);

    // 2. If IsDetachedBuffer(srcData) is true, throw a TypeError exception.
    if (src_data->is_detached())
        return vm.template throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 3. Let constructorName be the String value of O.[[TypedArrayName]].
    // 4. Let elementType be the Element Type value in Table 72 for constructorName.

    // 5. Let elementLength be srcArray.[[ArrayLength]].
    auto element_length = src_array.array_length();

    // 6. Let srcName be the String value of srcArray.[[TypedArrayName]].
    // 7. Let srcType be the Element Type value in Table 72 for srcName.

    // 8. Let srcElementSize be the Element Size value specified in Table 72 for srcName.
    auto src_element_size = src_array.element_size();

    // 9. Let srcByteOffset be srcArray.[[ByteOffset]].
    auto src_byte_offset = src_array.byte_offset();

    // 10. Let elementSize be the Element Size value specified in Table 72 for constructorName.
    auto element_size = dest_array.element_size();

    // 11. Let byteLength be elementSize × elementLength.
    Checked<size_t> byte_length = element_size;
    byte_length *= element_length;
    if (byte_length.has_overflow())
        return vm.template throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

    // FIXME:
    // 12. If IsSharedArrayBuffer(srcData) is false, then
    //     a. Let bufferConstructor be ? SpeciesConstructor(srcData, %ArrayBuffer%).
    // 13. Else,
    //    a. Let bufferConstructor be %ArrayBuffer%.
    // 14. If elementType is the same as srcType, then
    //    a. Let data be ? CloneArrayBuffer(srcData, srcByteOffset, byteLength, bufferConstructor).
    // 15. Else,

    // a. Let data be ? AllocateArrayBuffer(bufferConstructor, byteLength).
    auto* data = TRY(allocate_array_buffer(global_object, *global_object.array_buffer_constructor(), byte_length.value()));

    // b. If IsDetachedBuffer(srcData) is true, throw a TypeError exception.
    if (src_data->is_detached())
        return vm.template throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // c. If srcArray.[[ContentType]] ≠ O.[[ContentType]], throw a TypeError exception.
    if (src_array.content_type() != dest_array.content_type())
        return vm.template throw_completion<TypeError>(global_object, ErrorType::TypedArrayContentTypeMismatch, dest_array.class_name(), src_array.class_name());

    // d. Let srcByteIndex be srcByteOffset.
    u64 src_byte_index = src_byte_offset;

    // e. Let targetByteIndex be 0.
    u64 target_byte_index = 0;

    // f. Let count be elementLength.
    // g. Repeat, while count > 0,
    for (u32 i = 0; i < element_length; ++i) {
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

    // 16. Set O.[[ViewedArrayBuffer]] to data.
    dest_array.set_viewed_array_buffer(data);

    // 17. Set O.[[ByteLength]] to byteLength.
    dest_array.set_byte_length(byte_length.value());

    // 18. Set O.[[ByteOffset]] to 0.
    dest_array.set_byte_offset(0);

    // 19. Set O.[[ArrayLength]] to elementLength.
    dest_array.set_array_length(element_length);

    return {};
}

// 23.2.5.1.5 InitializeTypedArrayFromArrayLike, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraylike
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_array_like(GlobalObject& global_object, TypedArray<T>& typed_array, const Object& array_like)
{
    auto& vm = global_object.vm();

    // 1. Let len be ? LengthOfArrayLike(arrayLike).
    auto length = TRY(length_of_array_like(global_object, array_like));

    // 2. Perform ? AllocateTypedArrayBuffer(O, len).

    // 23.2.5.1.6 AllocateTypedArrayBuffer ( O, length )

    // Enforce 2GB "Excessive Length" limit
    if (length > NumericLimits<i32>::max() / sizeof(T))
        return vm.template throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

    // 1. Assert: O.[[ViewedArrayBuffer]] is undefined.
    // 2. Let constructorName be the String value of O.[[TypedArrayName]].

    // 3. Let elementSize be the Element Size value specified in Table 72 for constructorName.
    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, length))
        return vm.template throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

    // 4. Let byteLength be elementSize × length.
    auto byte_length = element_size * length;

    // 5. Let data be ? AllocateArrayBuffer(%ArrayBuffer%, byteLength).
    auto* data = TRY(allocate_array_buffer(global_object, *global_object.array_buffer_constructor(), byte_length));

    // 6. Set O.[[ViewedArrayBuffer]] to data.
    typed_array.set_viewed_array_buffer(data);

    // 7. Set O.[[ByteLength]] to byteLength.
    typed_array.set_byte_length(byte_length);

    // 8. Set O.[[ByteOffset]] to 0.
    typed_array.set_byte_offset(0);

    // 9. Set O.[[ArrayLength]] to length.
    typed_array.set_array_length(length);

    // 10. Return O.
    // End of 23.2.5.1.6

    // 4. Repeat, while k < len,
    for (size_t k = 0; k < length; k++) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ? Get(arrayLike, Pk).
        auto k_value = TRY(array_like.get(k));

        // c. Perform ? Set(O, Pk, kValue, true).
        TRY(typed_array.set(k, k_value, Object::ShouldThrowExceptions::Yes));

        // d. Set k to k + 1.
    }

    return {};
}

// 23.2.5.1.4 InitializeTypedArrayFromList, https://tc39.es/ecma262/#sec-initializetypedarrayfromlist
template<typename T>
static ThrowCompletionOr<void> initialize_typed_array_from_list(GlobalObject& global_object, TypedArray<T>& typed_array, const MarkedValueList& list)
{
    auto& vm = global_object.vm();

    // 1. Let len be the number of elements in values.
    auto length = list.size();

    // 2. Perform ? AllocateTypedArrayBuffer(O, len).

    // Enforce 2GB "Excessive Length" limit
    if (length > NumericLimits<i32>::max() / sizeof(T))
        return vm.template throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");

    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, length))
        return vm.template throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
    auto byte_length = element_size * length;
    auto array_buffer = TRY(allocate_array_buffer(global_object, *global_object.array_buffer_constructor(), byte_length));

    typed_array.set_viewed_array_buffer(array_buffer);
    typed_array.set_byte_length(byte_length);
    typed_array.set_byte_offset(0);
    typed_array.set_array_length(length);

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

    return {};
}

// 23.2.4.2 TypedArrayCreate ( constructor, argumentList ), https://tc39.es/ecma262/#typedarray-create
ThrowCompletionOr<TypedArrayBase*> typed_array_create(GlobalObject& global_object, FunctionObject& constructor, MarkedValueList arguments)
{
    auto& vm = global_object.vm();

    Optional<Value> first_argument;
    if (!arguments.is_empty())
        first_argument = arguments[0];
    // 1. Let newTypedArray be ? Construct(constructor, argumentList).
    auto* new_typed_array = TRY(construct(global_object, constructor, move(arguments)));

    // 2. Perform ? ValidateTypedArray(newTypedArray).
    if (!new_typed_array->is_typed_array())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");
    auto& typed_array = *static_cast<TypedArrayBase*>(new_typed_array);
    TRY(validate_typed_array(global_object, typed_array));

    // 3. If argumentList is a List of a single Number, then
    if (first_argument.has_value() && first_argument->is_number()) {
        // a. If newTypedArray.[[ArrayLength]] < ℝ(argumentList[0]), throw a TypeError exception.
        if (typed_array.array_length() < first_argument->as_double())
            return vm.throw_completion<TypeError>(global_object, ErrorType::InvalidLength, "typed array");
    }

    // 4. Return newTypedArray.
    return &typed_array;
}

void TypedArrayBase::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                                             \
    ThrowCompletionOr<ClassName*> ClassName::create(GlobalObject& global_object, u32 length, FunctionObject& new_target)               \
    {                                                                                                                                  \
        auto* prototype = TRY(get_prototype_from_constructor(global_object, new_target, &GlobalObject::snake_name##_prototype));       \
        return global_object.heap().allocate<ClassName>(global_object, length, *prototype);                                            \
    }                                                                                                                                  \
                                                                                                                                       \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length)                                                              \
    {                                                                                                                                  \
        return global_object.heap().allocate<ClassName>(global_object, length, *global_object.snake_name##_prototype());               \
    }                                                                                                                                  \
                                                                                                                                       \
    ClassName::ClassName(u32 length, Object& prototype)                                                                                \
        : TypedArray(length, prototype)                                                                                                \
    {                                                                                                                                  \
        if constexpr (StringView { #ClassName }.is_one_of("BigInt64Array", "BigUint64Array"))                                          \
            m_content_type = ContentType::BigInt;                                                                                      \
        else                                                                                                                           \
            m_content_type = ContentType::Number;                                                                                      \
    }                                                                                                                                  \
                                                                                                                                       \
    ClassName::~ClassName() { }                                                                                                        \
                                                                                                                                       \
    String ClassName::element_name() const                                                                                             \
    {                                                                                                                                  \
        return vm().names.ClassName.as_string();                                                                                       \
    }                                                                                                                                  \
                                                                                                                                       \
    PrototypeName::PrototypeName(GlobalObject& global_object)                                                                          \
        : Object(*global_object.typed_array_prototype())                                                                               \
    {                                                                                                                                  \
    }                                                                                                                                  \
                                                                                                                                       \
    PrototypeName::~PrototypeName() { }                                                                                                \
                                                                                                                                       \
    void PrototypeName::initialize(GlobalObject& global_object)                                                                        \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        Object::initialize(global_object);                                                                                             \
        define_direct_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                               \
    }                                                                                                                                  \
                                                                                                                                       \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                                                      \
        : TypedArrayConstructor(vm().names.ClassName.as_string(), *global_object.typed_array_constructor())                            \
    {                                                                                                                                  \
    }                                                                                                                                  \
                                                                                                                                       \
    ConstructorName::~ConstructorName() { }                                                                                            \
                                                                                                                                       \
    void ConstructorName::initialize(GlobalObject& global_object)                                                                      \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        NativeFunction::initialize(global_object);                                                                                     \
                                                                                                                                       \
        /* 23.2.6.2 TypedArray.prototype, https://tc39.es/ecma262/#sec-typedarray.prototype */                                         \
        define_direct_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                         \
                                                                                                                                       \
        /* 23.2.6.1 TypedArray.BYTES_PER_ELEMENT, https://tc39.es/ecma262/#sec-typedarray.bytes_per_element */                         \
        define_direct_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                               \
                                                                                                                                       \
        define_direct_property(vm.names.length, Value(3), Attribute::Configurable);                                                    \
    }                                                                                                                                  \
                                                                                                                                       \
    /* 23.2.5.1 TypedArray ( ...args ), https://tc39.es/ecma262/#sec-typedarray */                                                     \
    ThrowCompletionOr<Value> ConstructorName::call()                                                                                   \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ClassName);                  \
    }                                                                                                                                  \
                                                                                                                                       \
    /* 23.2.5.1 TypedArray ( ...args ), https://tc39.es/ecma262/#sec-typedarray */                                                     \
    ThrowCompletionOr<Object*> ConstructorName::construct(FunctionObject& new_target)                                                  \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        if (vm.argument_count() == 0)                                                                                                  \
            return TRY(ClassName::create(global_object(), 0, new_target));                                                             \
                                                                                                                                       \
        auto first_argument = vm.argument(0);                                                                                          \
        if (first_argument.is_object()) {                                                                                              \
            auto* typed_array = TRY(ClassName::create(global_object(), 0, new_target));                                                \
            if (first_argument.as_object().is_typed_array()) {                                                                         \
                auto& arg_typed_array = static_cast<TypedArrayBase&>(first_argument.as_object());                                      \
                TRY(initialize_typed_array_from_typed_array(global_object(), *typed_array, arg_typed_array));                          \
            } else if (is<ArrayBuffer>(first_argument.as_object())) {                                                                  \
                auto& array_buffer = static_cast<ArrayBuffer&>(first_argument.as_object());                                            \
                /* NOTE: I added the padding below to not reindent 150+ lines for a single line change. If you edit this, and the   */ \
                /*       width happens to change anyway, feel free to remove it.                                                    */ \
                TRY(initialize_typed_array_from_array_buffer(global_object(), *typed_array, array_buffer, /*                        */ \
                    vm.argument(1), vm.argument(2)));                                                                                  \
            } else {                                                                                                                   \
                auto iterator = TRY(first_argument.get_method(global_object(), *vm.well_known_symbol_iterator()));                     \
                if (iterator) {                                                                                                        \
                    auto values = TRY(iterable_to_list(global_object(), first_argument, iterator));                                    \
                    TRY(initialize_typed_array_from_list(global_object(), *typed_array, values));                                      \
                } else {                                                                                                               \
                    TRY(initialize_typed_array_from_array_like(global_object(), *typed_array, first_argument.as_object()));            \
                }                                                                                                                      \
            }                                                                                                                          \
            return typed_array;                                                                                                        \
        }                                                                                                                              \
                                                                                                                                       \
        auto array_length_or_error = first_argument.to_index(global_object());                                                         \
        if (array_length_or_error.is_error()) {                                                                                        \
            auto error = array_length_or_error.release_error();                                                                        \
            if (error.value()->is_object() && is<RangeError>(error.value()->as_object())) {                                            \
                /* Re-throw more specific RangeError */                                                                                \
                vm.clear_exception();                                                                                                  \
                return vm.throw_completion<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                      \
            }                                                                                                                          \
            return error;                                                                                                              \
        }                                                                                                                              \
        auto array_length = array_length_or_error.release_value();                                                                     \
        if (array_length > NumericLimits<i32>::max() / sizeof(Type))                                                                   \
            return vm.throw_completion<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                          \
        /* FIXME: What is the best/correct behavior here? */                                                                           \
        if (Checked<u32>::multiplication_would_overflow(array_length, sizeof(Type)))                                                   \
            return vm.throw_completion<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                          \
        return TRY(ClassName::create(global_object(), array_length, new_target));                                                      \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
