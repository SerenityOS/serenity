/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

static void initialize_typed_array_from_array_buffer(GlobalObject& global_object, TypedArrayBase& typed_array, ArrayBuffer& array_buffer, Value byte_offset, Value length)
{
    // 22.2.5.1.3 InitializeTypedArrayFromArrayBuffer, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraybuffer

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
template<typename T>
static void initialize_typed_array_from_typed_array(GlobalObject& global_object, TypedArray<T>& dest_array, TypedArrayBase& src_array)
{
    auto& vm = global_object.vm();
    if (vm.exception())
        return;

    auto* source_array_buffer = src_array.viewed_array_buffer();
    VERIFY(source_array_buffer);
    if (source_array_buffer->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return;
    }

    auto src_array_length = src_array.array_length();
    auto dest_element_size = dest_array.element_size();
    Checked byte_length = src_array_length * dest_element_size;
    if (byte_length.has_overflow()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }

    // FIXME: 17.b If IsDetachedBuffer(array_buffer) is true, throw a TypeError exception.
    // FIXME: 17.c If src_array.[[ContentType]] != dest_array.[[ContentType]], throw a TypeError exception.
    auto array_buffer = ArrayBuffer::create(global_object, byte_length.value());
    dest_array.set_array_length(src_array_length);
    dest_array.set_viewed_array_buffer(array_buffer);
    dest_array.set_byte_offset(0);
    dest_array.set_byte_length(array_buffer->byte_length());

    for (u32 i = 0; i < src_array_length; i++) {
        Value v;
#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (is<JS::ClassName>(src_array)) {                                                  \
        auto& src = static_cast<JS::ClassName&>(src_array);                              \
        v = src.get_by_index(i);                                                         \
    }
        JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

        VERIFY(!v.is_empty());

        dest_array.put_by_index(i, v);
    }
}
template<typename T>
static void initialize_typed_array_from_array_like(GlobalObject& global_object, TypedArray<T>& typed_array, const Object& array_like)
{
    // 23.2.5.1.5 InitializeTypedArrayFromArrayLike, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraylike

    auto& vm = global_object.vm();
    auto length = length_of_array_like(global_object, array_like);
    if (vm.exception())
        return;

    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, length)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }
    auto byte_length = element_size * length;
    auto array_buffer = ArrayBuffer::create(global_object, byte_length);
    typed_array.set_viewed_array_buffer(array_buffer);
    typed_array.set_byte_length(byte_length);
    typed_array.set_byte_offset(0);
    typed_array.set_array_length(length);

    for (size_t k = 0; k < length; k++) {
        auto value = array_like.get(k).value_or(js_undefined());
        if (vm.exception())
            return;
        typed_array.put_by_index(k, value);
        if (vm.exception())
            return;
    }
}
template<typename T>
static void initialize_typed_array_from_list(GlobalObject& global_object, TypedArray<T>& typed_array, const MarkedValueList& list)
{
    // 23.2.5.1.4 InitializeTypedArrayFromList, https://tc39.es/ecma262/#sec-initializetypedarrayfromlist

    auto element_size = typed_array.element_size();
    if (Checked<size_t>::multiplication_would_overflow(element_size, list.size())) {
        global_object.vm().throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "typed array");
        return;
    }
    auto byte_length = element_size * list.size();
    auto array_buffer = ArrayBuffer::create(global_object, byte_length);
    typed_array.set_viewed_array_buffer(array_buffer);
    typed_array.set_byte_length(byte_length);
    typed_array.set_byte_offset(0);
    typed_array.set_array_length(list.size());

    auto& vm = global_object.vm();
    for (size_t k = 0; k < list.size(); k++) {
        auto value = list[k];
        typed_array.put_by_index(k, value);
        if (vm.exception())
            return;
    }
}

void TypedArrayBase::visit_edges(Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                                             \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length)                                                              \
    {                                                                                                                                  \
        return global_object.heap().allocate<ClassName>(global_object, length, *global_object.snake_name##_prototype());               \
    }                                                                                                                                  \
                                                                                                                                       \
    ClassName::ClassName(u32 length, Object& prototype)                                                                                \
        : TypedArray(length, prototype)                                                                                                \
    {                                                                                                                                  \
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
        auto& vm = this->vm();                                                                                                         \
        define_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                                      \
    }                                                                                                                                  \
                                                                                                                                       \
    PrototypeName::~PrototypeName() { }                                                                                                \
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
        define_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                                \
                                                                                                                                       \
        define_property(vm.names.length, Value(3), Attribute::Configurable);                                                           \
                                                                                                                                       \
        /* 23.2.6.1 TypedArray.BYTES_PER_ELEMENT, https://tc39.es/ecma262/#sec-typedarray.bytes_per_element */                         \
        define_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                                      \
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
    Value ConstructorName::construct(Function&)                                                                                        \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        if (vm.argument_count() == 0)                                                                                                  \
            return ClassName::create(global_object(), 0);                                                                              \
                                                                                                                                       \
        auto first_argument = vm.argument(0);                                                                                          \
        if (first_argument.is_object()) {                                                                                              \
            auto* typed_array = ClassName::create(global_object(), 0);                                                                 \
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
                auto iterator = first_argument.as_object().get(vm.well_known_symbol_iterator());                                       \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
                if (iterator.is_function()) {                                                                                          \
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
            /* Re-throw more specific RangeError */                                                                                    \
            vm.clear_exception();                                                                                                      \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        if (array_length > NumericLimits<i32>::max()) {                                                                                \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        /* FIXME: What is the best/correct behavior here? */                                                                           \
        if (Checked<u32>::multiplication_would_overflow(array_length, sizeof(Type))) {                                                 \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        return ClassName::create(global_object(), array_length);                                                                       \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
