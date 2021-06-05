/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ScopeObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class GlobalObject : public ScopeObject {
    JS_OBJECT(GlobalObject, ScopeObject);

public:
    explicit GlobalObject();
    virtual void initialize_global_object();

    virtual ~GlobalObject() override;

    virtual Optional<Variable> get_from_scope(const FlyString&) const override;
    virtual void put_to_scope(const FlyString&, Variable) override;
    virtual bool has_this_binding() const override;
    virtual Value get_this_binding(GlobalObject&) const override;

    Console& console() { return *m_console; }

    Shape* empty_object_shape() { return m_empty_object_shape; }

    Shape* new_object_shape() { return m_new_object_shape; }
    Shape* new_script_function_prototype_object_shape() { return m_new_script_function_prototype_object_shape; }

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* proxy_constructor() { return m_proxy_constructor; }

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName* snake_name##_constructor() { return m_##snake_name##_constructor; } \
    Object* snake_name##_prototype() { return m_##snake_name##_prototype; }
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* snake_name##_prototype() { return m_##snake_name##_prototype; }
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

protected:
    virtual void visit_edges(Visitor&) override;

    template<typename ConstructorType>
    void initialize_constructor(const FlyString& property_name, ConstructorType*&, Object* prototype);
    template<typename ConstructorType>
    void add_constructor(const FlyString& property_name, ConstructorType*&, Object* prototype);

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

    Shape* m_empty_object_shape { nullptr };
    Shape* m_new_object_shape { nullptr };
    Shape* m_new_script_function_prototype_object_shape { nullptr };

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    ProxyConstructor* m_proxy_constructor { nullptr };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName* m_##snake_name##_constructor { nullptr };                           \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
};

template<typename ConstructorType>
inline void GlobalObject::initialize_constructor(const FlyString& property_name, ConstructorType*& constructor, Object* prototype)
{
    auto& vm = this->vm();
    constructor = heap().allocate<ConstructorType>(*this, *this);
    constructor->define_property(vm.names.name, js_string(heap(), property_name), Attribute::Configurable);
    if (vm.exception())
        return;
    if (prototype) {
        prototype->define_property(vm.names.constructor, constructor, Attribute::Writable | Attribute::Configurable);
        if (vm.exception())
            return;
    }
}

template<typename ConstructorType>
inline void GlobalObject::add_constructor(const FlyString& property_name, ConstructorType*& constructor, Object* prototype)
{
    initialize_constructor(property_name, constructor, prototype);
    define_property(property_name, constructor, Attribute::Writable | Attribute::Configurable);
}

inline GlobalObject* Shape::global_object() const
{
    return static_cast<GlobalObject*>(m_global_object);
}

template<>
inline bool Object::fast_is<GlobalObject>() const { return is_global_object(); }

}
