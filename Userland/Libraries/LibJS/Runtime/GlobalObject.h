/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class GlobalObject : public Object {
    JS_OBJECT(GlobalObject, Object);

public:
    explicit GlobalObject();
    virtual void initialize_global_object();

    virtual ~GlobalObject() override;

    Console& console() { return *m_console; }

    Realm* associated_realm();
    void set_associated_realm(Badge<Realm>, Realm&);

    Shape* empty_object_shape() { return m_empty_object_shape; }

    Shape* new_object_shape() { return m_new_object_shape; }
    Shape* new_ordinary_function_prototype_object_shape() { return m_new_ordinary_function_prototype_object_shape; }

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* proxy_constructor() { return m_proxy_constructor; }

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct constructor
    GeneratorPrototype* generator_prototype() { return m_generator_prototype; }
    AsyncFromSyncIteratorPrototype* async_from_sync_iterator_prototype() { return m_async_from_sync_iterator_prototype; }

    // Not included in JS_ENUMERATE_INTL_OBJECTS due to missing distinct constructor
    Intl::SegmentsPrototype* intl_segments_prototype() { return m_intl_segments_prototype; }

    FunctionObject* array_prototype_values_function() const { return m_array_prototype_values_function; }
    FunctionObject* date_constructor_now_function() const { return m_date_constructor_now_function; }
    FunctionObject* eval_function() const { return m_eval_function; }
    FunctionObject* throw_type_error_function() const { return m_throw_type_error_function; }
    FunctionObject* json_parse_function() const { return m_json_parse_function; }

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName* snake_name##_constructor() { return m_##snake_name##_constructor; } \
    Object* snake_name##_prototype() { return m_##snake_name##_prototype; }
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                              \
    Intl::ConstructorName* intl_##snake_name##_constructor() { return m_intl_##snake_name##_constructor; } \
    Object* intl_##snake_name##_prototype() { return m_intl_##snake_name##_prototype; }
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                                          \
    Temporal::ConstructorName* temporal_##snake_name##_constructor() { return m_temporal_##snake_name##_constructor; } \
    Object* temporal_##snake_name##_prototype() { return m_temporal_##snake_name##_prototype; }
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* snake_name##_prototype() { return m_##snake_name##_prototype; }
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

protected:
    virtual void visit_edges(Visitor&) override;

    template<typename ConstructorType>
    void initialize_constructor(PropertyKey const&, ConstructorType*&, Object* prototype);
    template<typename ConstructorType>
    void add_constructor(PropertyKey const&, ConstructorType*&, Object* prototype);

private:
    virtual bool is_global_object() const final { return true; }

    JS_DECLARE_NATIVE_FUNCTION(gc);
    JS_DECLARE_NATIVE_FUNCTION(is_nan);
    JS_DECLARE_NATIVE_FUNCTION(is_finite);
    JS_DECLARE_NATIVE_FUNCTION(parse_float);
    JS_DECLARE_NATIVE_FUNCTION(parse_int);
    JS_DECLARE_NATIVE_FUNCTION(eval);
    JS_DECLARE_NATIVE_FUNCTION(encode_uri);
    JS_DECLARE_NATIVE_FUNCTION(decode_uri);
    JS_DECLARE_NATIVE_FUNCTION(encode_uri_component);
    JS_DECLARE_NATIVE_FUNCTION(decode_uri_component);
    JS_DECLARE_NATIVE_FUNCTION(escape);
    JS_DECLARE_NATIVE_FUNCTION(unescape);

    NonnullOwnPtr<Console> m_console;

    WeakPtr<Realm> m_associated_realm;

    Shape* m_empty_object_shape { nullptr };
    Shape* m_new_object_shape { nullptr };
    Shape* m_new_ordinary_function_prototype_object_shape { nullptr };

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* m_proxy_constructor { nullptr };

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct constructor
    GeneratorPrototype* m_generator_prototype { nullptr };
    AsyncFromSyncIteratorPrototype* m_async_from_sync_iterator_prototype { nullptr };

    // Not included in JS_ENUMERATE_INTL_OBJECTS due to missing distinct constructor
    Intl::SegmentsPrototype* m_intl_segments_prototype { nullptr };

    FunctionObject* m_array_prototype_values_function { nullptr };
    FunctionObject* m_date_constructor_now_function { nullptr };
    FunctionObject* m_eval_function { nullptr };
    FunctionObject* m_throw_type_error_function { nullptr };
    FunctionObject* m_json_parse_function { nullptr };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName* m_##snake_name##_constructor { nullptr };                           \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    Intl::ConstructorName* m_intl_##snake_name##_constructor { nullptr };     \
    Object* m_intl_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)     \
    Temporal::ConstructorName* m_temporal_##snake_name##_constructor { nullptr }; \
    Object* m_temporal_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
};

template<typename ConstructorType>
inline void GlobalObject::initialize_constructor(PropertyKey const& property_key, ConstructorType*& constructor, Object* prototype)
{
    auto& vm = this->vm();
    constructor = heap().allocate<ConstructorType>(*this, *this);
    constructor->define_direct_property(vm.names.name, js_string(heap(), property_key.as_string()), Attribute::Configurable);
    if (prototype)
        prototype->define_direct_property(vm.names.constructor, constructor, Attribute::Writable | Attribute::Configurable);
}

template<typename ConstructorType>
inline void GlobalObject::add_constructor(PropertyKey const& property_key, ConstructorType*& constructor, Object* prototype)
{
    // Some constructors are pre-initialized separately.
    if (!constructor)
        initialize_constructor(property_key, constructor, prototype);
    define_direct_property(property_key, constructor, Attribute::Writable | Attribute::Configurable);
}

inline GlobalObject* Shape::global_object() const
{
    return static_cast<GlobalObject*>(m_global_object);
}

template<>
inline bool Object::fast_is<GlobalObject>() const { return is_global_object(); }

template<typename... Args>
[[nodiscard]] ALWAYS_INLINE ThrowCompletionOr<Value> Value::invoke(GlobalObject& global_object, PropertyKey const& property_key, Args... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedVector<Value> arglist { global_object.vm().heap() };
        (..., arglist.append(move(args)));
        return invoke_internal(global_object, property_key, move(arglist));
    }

    return invoke_internal(global_object, property_key, Optional<MarkedVector<Value>> {});
}

}
