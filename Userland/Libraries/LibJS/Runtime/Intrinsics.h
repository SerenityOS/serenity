/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class Intrinsics final : public Cell {
    JS_CELL(Intrinsics, Cell);

public:
    static Intrinsics* create(Realm&);

    Shape* empty_object_shape() { return m_empty_object_shape; }

    Shape* new_object_shape() { return m_new_object_shape; }
    Shape* new_ordinary_function_prototype_object_shape() { return m_new_ordinary_function_prototype_object_shape; }

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* proxy_constructor() { return m_proxy_constructor; }

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct constructor
    Object* async_from_sync_iterator_prototype() { return m_async_from_sync_iterator_prototype; }
    Object* async_generator_prototype() { return m_async_generator_prototype; }
    Object* generator_prototype() { return m_generator_prototype; }

    // Alias for the AsyncGenerator Prototype Object used by the spec (%AsyncGeneratorFunction.prototype.prototype%)
    Object* async_generator_function_prototype_prototype() { return m_async_generator_prototype; }
    // Alias for the Generator Prototype Object used by the spec (%GeneratorFunction.prototype.prototype%)
    Object* generator_function_prototype_prototype() { return m_generator_prototype; }

    // Not included in JS_ENUMERATE_INTL_OBJECTS due to missing distinct constructor
    Object* intl_segments_prototype() { return m_intl_segments_prototype; }

    // Global object functions
    FunctionObject* eval_function() const { return m_eval_function; }
    FunctionObject* is_finite_function() const { return m_is_finite_function; }
    FunctionObject* is_nan_function() const { return m_is_nan_function; }
    FunctionObject* parse_float_function() const { return m_parse_float_function; }
    FunctionObject* parse_int_function() const { return m_parse_int_function; }
    FunctionObject* decode_uri_function() const { return m_decode_uri_function; }
    FunctionObject* decode_uri_component_function() const { return m_decode_uri_component_function; }
    FunctionObject* encode_uri_function() const { return m_encode_uri_function; }
    FunctionObject* encode_uri_component_function() const { return m_encode_uri_component_function; }
    FunctionObject* escape_function() const { return m_escape_function; }
    FunctionObject* unescape_function() const { return m_unescape_function; }

    // Namespace/constructor object functions
    FunctionObject* array_prototype_values_function() const { return m_array_prototype_values_function; }
    FunctionObject* date_constructor_now_function() const { return m_date_constructor_now_function; }
    FunctionObject* json_parse_function() const { return m_json_parse_function; }
    FunctionObject* object_prototype_to_string_function() const { return m_object_prototype_to_string_function; }
    FunctionObject* throw_type_error_function() const { return m_throw_type_error_function; }

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName* snake_name##_constructor()                                          \
    {                                                                                    \
        return m_##snake_name##_constructor;                                             \
    }                                                                                    \
    Object* snake_name##_prototype()                                                     \
    {                                                                                    \
        return m_##snake_name##_prototype;                                               \
    }
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    Intl::ConstructorName* intl_##snake_name##_constructor()                  \
    {                                                                         \
        return m_intl_##snake_name##_constructor;                             \
    }                                                                         \
    Object* intl_##snake_name##_prototype()                                   \
    {                                                                         \
        return m_intl_##snake_name##_prototype;                               \
    }
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    Temporal::ConstructorName* temporal_##snake_name##_constructor()          \
    {                                                                         \
        return m_temporal_##snake_name##_constructor;                         \
    }                                                                         \
    Object* temporal_##snake_name##_prototype()                               \
    {                                                                         \
        return m_temporal_##snake_name##_prototype;                           \
    }
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    ClassName* snake_name##_object()          \
    {                                         \
        return m_##snake_name##_object;       \
    }
    JS_ENUMERATE_BUILTIN_NAMESPACE_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* snake_name##_prototype()          \
    {                                         \
        return m_##snake_name##_prototype;    \
    }
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

private:
    Intrinsics() = default;

    virtual void visit_edges(Visitor&) override;

    void initialize_intrinsics(Realm&);

    Shape* m_empty_object_shape { nullptr };
    Shape* m_new_object_shape { nullptr };
    Shape* m_new_ordinary_function_prototype_object_shape { nullptr };

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* m_proxy_constructor { nullptr };

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct constructor
    Object* m_async_from_sync_iterator_prototype { nullptr };
    Object* m_async_generator_prototype { nullptr };
    Object* m_generator_prototype { nullptr };

    // Not included in JS_ENUMERATE_INTL_OBJECTS due to missing distinct constructor
    Object* m_intl_segments_prototype { nullptr };

    // Global object functions
    FunctionObject* m_eval_function { nullptr };
    FunctionObject* m_is_finite_function { nullptr };
    FunctionObject* m_is_nan_function { nullptr };
    FunctionObject* m_parse_float_function { nullptr };
    FunctionObject* m_parse_int_function { nullptr };
    FunctionObject* m_decode_uri_function { nullptr };
    FunctionObject* m_decode_uri_component_function { nullptr };
    FunctionObject* m_encode_uri_function { nullptr };
    FunctionObject* m_encode_uri_component_function { nullptr };
    FunctionObject* m_escape_function { nullptr };
    FunctionObject* m_unescape_function { nullptr };

    // Namespace/constructor object functions
    FunctionObject* m_array_prototype_values_function { nullptr };
    FunctionObject* m_date_constructor_now_function { nullptr };
    FunctionObject* m_json_parse_function { nullptr };
    FunctionObject* m_object_prototype_to_string_function { nullptr };
    FunctionObject* m_throw_type_error_function { nullptr };

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
    ClassName* m_##snake_name##_object { nullptr };
    JS_ENUMERATE_BUILTIN_NAMESPACE_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
};

void add_restricted_function_properties(FunctionObject&, Realm&);

}
