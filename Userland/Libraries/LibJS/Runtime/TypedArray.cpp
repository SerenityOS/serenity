/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

TypedArrayBase* typed_array_from(GlobalObject& global_object, Value typed_array_value)
{
    auto& vm = global_object.vm();

    auto* this_object = typed_array_value.to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");
        return nullptr;
    }

    return static_cast<TypedArrayBase*>(this_object);
}

// 23.2.4.3 ValidateTypedArray ( O ), https://tc39.es/ecma262/#sec-validatetypedarray
void validate_typed_array(GlobalObject& global_object, TypedArrayBase& typed_array)
{
    auto& vm = global_object.vm();

    if (!typed_array.is_typed_array())
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");
    else if (typed_array.viewed_array_buffer()->is_detached())
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
}

// 22.2.5.1.3 InitializeTypedArrayFromArrayBuffer, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraybuffer
static void initialize_typed_array_from_array_buffer(GlobalObject& global_object, TypedArrayBase& typed_array, ArrayBuffer& array_buffer, Value byte_offset, Value length)
{
    auto& vm = global_object.vm();
    auto element_size = typed_array.element_size();
    auto offset = byte_offset.to_index(global_object);
    if (vm.exception())
        return;
    if (offset % element_size != 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayInvalidByteOffset, typed_array.class_name(), element_size, offset);
        return;
    }
    size_t new_length { 0 };
    if (!length.is_undefined()) {
        new_length = length.to_index(global_object);
        if (vm.exception())
            return;
    }

    if (array_buffer.is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return;
    }

    auto buffer_byte_length = array_buffer.byte_length();
    Checked<size_t> new_byte_length;
    if (length.is_undefined()) {
        if (buffer_byte_length % element_size != 0) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayInvalidBufferLength, typed_array.class_name(), element_size, buffer_byte_length);
            return;
        }
        if (offset > buffer_byte_length) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffset, offset, buffer_byte_length);
            return;
        }
        new_byte_length = buffer_byte_length;
        new_byte_length -= offset;
    } else {
        new_byte_length = new_length;
        new_byte_length *= element_size;

        Checked<size_t> new_byte_end = new_byte_length;
        new_byte_end += offset;

        if (new_byte_end.has_overflow()) {
            vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
            return;
        }

        if (new_byte_end.value() > buffer_byte_length) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffsetOrLength, offset, new_byte_end.value(), buffer_byte_length);
            return;
        }
    }
    if (new_byte_length.has_overflow()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }

    typed_array.set_viewed_array_buffer(&array_buffer);
    typed_array.set_byte_length(new_byte_length.value());
    typed_array.set_byte_offset(offset);
    typed_array.set_array_length(new_byte_length.value() / element_size);
}

// 23.2.5.1.2 InitializeTypedArrayFromTypedArray ( O, srcArray ), https://tc39.es/ecma262/#sec-initializetypedarrayfromtypedarray
template<typename T>
static void initialize_typed_array_from_typed_array(GlobalObject& global_object, TypedArray<T>& dest_array, TypedArrayBase& src_array)
{
    auto& vm = global_object.vm();
    if (vm.exception())
        return;

    auto* src_data = src_array.viewed_array_buffer();
    VERIFY(src_data);
    if (src_data->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return;
    }

    auto element_length = src_array.array_length();
    auto src_element_size = src_array.element_size();
    auto src_byte_offset = src_array.byte_offset();
    auto element_size = dest_array.element_size();
    Checked<size_t> byte_length = element_size;
    byte_length *= element_length;
    if (byte_length.has_overflow()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }

    // FIXME: Determine and use bufferConstructor
    auto data = ArrayBuffer::create(global_object, byte_length.value());

    if (src_data->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return;
    }

    if (src_array.content_type() != dest_array.content_type()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::TypedArrayContentTypeMismatch, dest_array.class_name(), src_array.class_name());
        return;
    }

    u64 src_byte_index = src_byte_offset;
    u64 target_byte_index = 0;
    for (u32 i = 0; i < element_length; ++i) {
        auto value = src_array.get_value_from_buffer(src_byte_index, ArrayBuffer::Order::Unordered);
        data->template set_value<T>(target_byte_index, value, true, ArrayBuffer::Order::Unordered);
        src_byte_index += src_element_size;
        target_byte_index += element_size;
    }

    dest_array.set_viewed_array_buffer(data);
    dest_array.set_byte_length(byte_length.value());
    dest_array.set_byte_offset(0);
    dest_array.set_array_length(element_length);
}

// 23.2.5.1.5 InitializeTypedArrayFromArrayLike, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraylike
template<typename T>
static void initialize_typed_array_from_array_like(GlobalObject& global_object, TypedArray<T>& typed_array, const Object& array_like)
{
    auto& vm = global_object.vm();
    auto length = length_of_array_like(global_object, array_like);
    if (vm.exception())
        return;

    // Enforce 2GB "Excessive Length" limit
    if (length > NumericLimits<i32>::max() / sizeof(T)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }

    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, length)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }
    auto byte_length = element_size * length;
    auto array_buffer = ArrayBuffer::create(global_object, byte_length);
    if (!array_buffer)
        return;

    typed_array.set_viewed_array_buffer(array_buffer);
    typed_array.set_byte_length(byte_length);
    typed_array.set_byte_offset(0);
    typed_array.set_array_length(length);

    for (size_t k = 0; k < length; k++) {
        auto value = array_like.get(k);
        if (vm.exception())
            return;
        typed_array.set(k, value, Object::ShouldThrowExceptions::Yes);
        if (vm.exception())
            return;
    }
}

// 23.2.5.1.4 InitializeTypedArrayFromList, https://tc39.es/ecma262/#sec-initializetypedarrayfromlist
template<typename T>
static void initialize_typed_array_from_list(GlobalObject& global_object, TypedArray<T>& typed_array, const MarkedValueList& list)
{
    auto& vm = global_object.vm();
    // Enforce 2GB "Excessive Length" limit
    if (list.size() > NumericLimits<i32>::max() / sizeof(T)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }

    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, list.size())) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }
    auto byte_length = element_size * list.size();
    auto array_buffer = ArrayBuffer::create(global_object, byte_length);
    if (!array_buffer)
        return;

    typed_array.set_viewed_array_buffer(array_buffer);
    typed_array.set_byte_length(byte_length);
    typed_array.set_byte_offset(0);
    typed_array.set_array_length(list.size());

    for (size_t k = 0; k < list.size(); k++) {
        auto value = list[k];
        typed_array.set(k, value, Object::ShouldThrowExceptions::Yes);
        if (vm.exception())
            return;
    }
}

// 23.2.4.2 TypedArrayCreate ( constructor, argumentList ), https://tc39.es/ecma262/#typedarray-create
TypedArrayBase* typed_array_create(GlobalObject& global_object, FunctionObject& constructor, MarkedValueList arguments)
{
    auto& vm = global_object.vm();

    auto argument_count = arguments.size();
    auto first_argument = argument_count > 0 ? arguments[0] : js_undefined();

    auto new_typed_array = vm.construct(constructor, constructor, move(arguments));
    if (vm.exception())
        return nullptr;
    if (!new_typed_array.is_object() || !new_typed_array.as_object().is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "TypedArray");
        return nullptr;
    }
    auto& typed_array = static_cast<TypedArrayBase&>(new_typed_array.as_object());
    if (typed_array.viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return nullptr;
    }
    if (argument_count == 1 && first_argument.is_number() && typed_array.array_length() < first_argument.as_double()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::InvalidLength, "typed array");
        return nullptr;
    }
    return &typed_array;
}

void TypedArrayBase::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                                             \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length, FunctionObject& new_target)                                  \
    {                                                                                                                                  \
        auto& vm = global_object.vm();                                                                                                 \
        auto* prototype = get_prototype_from_constructor(global_object, new_target, &GlobalObject::snake_name##_prototype);            \
        if (vm.exception())                                                                                                            \
            return {};                                                                                                                 \
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
    Value ConstructorName::call()                                                                                                      \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ClassName);                          \
        return {};                                                                                                                     \
    }                                                                                                                                  \
                                                                                                                                       \
    /* 23.2.5.1 TypedArray ( ...args ), https://tc39.es/ecma262/#sec-typedarray */                                                     \
    Value ConstructorName::construct(FunctionObject& new_target)                                                                       \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        if (vm.argument_count() == 0)                                                                                                  \
            return ClassName::create(global_object(), 0, new_target);                                                                  \
                                                                                                                                       \
        auto first_argument = vm.argument(0);                                                                                          \
        if (first_argument.is_object()) {                                                                                              \
            auto* typed_array = ClassName::create(global_object(), 0, new_target);                                                     \
            if (vm.exception())                                                                                                        \
                return {};                                                                                                             \
            if (first_argument.as_object().is_typed_array()) {                                                                         \
                auto& arg_typed_array = static_cast<TypedArrayBase&>(first_argument.as_object());                                      \
                initialize_typed_array_from_typed_array(global_object(), *typed_array, arg_typed_array);                               \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
            } else if (is<ArrayBuffer>(first_argument.as_object())) {                                                                  \
                auto& array_buffer = static_cast<ArrayBuffer&>(first_argument.as_object());                                            \
                initialize_typed_array_from_array_buffer(global_object(), *typed_array, array_buffer, vm.argument(1), vm.argument(2)); \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
            } else {                                                                                                                   \
                auto iterator = first_argument.get_method(global_object(), *vm.well_known_symbol_iterator());                          \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
                if (iterator) {                                                                                                        \
                    auto values = iterable_to_list(global_object(), first_argument, iterator);                                         \
                    if (vm.exception())                                                                                                \
                        return {};                                                                                                     \
                    initialize_typed_array_from_list(global_object(), *typed_array, values);                                           \
                } else {                                                                                                               \
                    initialize_typed_array_from_array_like(global_object(), *typed_array, first_argument.as_object());                 \
                }                                                                                                                      \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
            }                                                                                                                          \
            return typed_array;                                                                                                        \
        }                                                                                                                              \
                                                                                                                                       \
        auto array_length = first_argument.to_index(global_object());                                                                  \
        if (vm.exception()) {                                                                                                          \
            if (vm.exception()->value().is_object() && is<RangeError>(vm.exception()->value().as_object())) {                          \
                /* Re-throw more specific RangeError */                                                                                \
                vm.clear_exception();                                                                                                  \
                vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                              \
            }                                                                                                                          \
            return {};                                                                                                                 \
        }                                                                                                                              \
        if (array_length > NumericLimits<i32>::max() / sizeof(Type)) {                                                                 \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        /* FIXME: What is the best/correct behavior here? */                                                                           \
        if (Checked<u32>::multiplication_would_overflow(array_length, sizeof(Type))) {                                                 \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        return ClassName::create(global_object(), array_length, new_target);                                                           \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
