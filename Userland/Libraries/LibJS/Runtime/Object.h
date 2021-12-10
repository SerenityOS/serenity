/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/IndexedProperties.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

#define JS_OBJECT(class_, base_class) \
public:                               \
    using Base = base_class;          \
    virtual const char* class_name() const override { return #class_; }

struct PrivateElement {
    enum class Kind {
        Field,
        Method,
        Accessor
    };

    PrivateName key;
    Kind kind { Kind::Field };
    Value value;
};

class Object : public Cell {
public:
    static Object* create(GlobalObject&, Object* prototype);

    explicit Object(Object& prototype);
    explicit Object(Shape&);
    virtual void initialize(GlobalObject&) override;
    virtual ~Object();

    enum class PropertyKind {
        Key,
        Value,
        KeyAndValue,
    };

    enum class IntegrityLevel {
        Sealed,
        Frozen,
    };

    enum class ShouldThrowExceptions {
        No,
        Yes,
    };

    // Please DO NOT make up your own non-standard methods unless you
    // have a very good reason to do so. If any object abstract
    // operation from the spec is missing, add it instead.
    // Functionality for implementation details like shapes and
    // property storage are obviously exempt from this rule :^)
    //
    // Methods named [[Foo]]() in the spec are named internal_foo()
    // here, as they are "The [[Foo]] internal method of a ... object".
    // They must be virtual and may be overridden. All other methods
    // follow the regular PascalCase name converted to camel_case
    // naming convention and must not be virtual.

    // 7.1 Type Conversion, https://tc39.es/ecma262/#sec-type-conversion

    ThrowCompletionOr<Value> ordinary_to_primitive(Value::PreferredType preferred_type) const;

    // 7.2 Testing and Comparison Operations, https://tc39.es/ecma262/#sec-testing-and-comparison-operations

    ThrowCompletionOr<bool> is_extensible() const;

    // 7.3 Operations on Objects, https://tc39.es/ecma262/#sec-operations-on-objects

    ThrowCompletionOr<Value> get(PropertyKey const&) const;
    ThrowCompletionOr<bool> set(PropertyKey const&, Value, ShouldThrowExceptions);
    ThrowCompletionOr<bool> create_data_property(PropertyKey const&, Value);
    ThrowCompletionOr<bool> create_method_property(PropertyKey const&, Value);
    ThrowCompletionOr<bool> create_data_property_or_throw(PropertyKey const&, Value);
    ThrowCompletionOr<bool> create_non_enumerable_data_property_or_throw(PropertyKey const&, Value);
    ThrowCompletionOr<bool> define_property_or_throw(PropertyKey const&, PropertyDescriptor const&);
    ThrowCompletionOr<bool> delete_property_or_throw(PropertyKey const&);
    ThrowCompletionOr<bool> has_property(PropertyKey const&) const;
    ThrowCompletionOr<bool> has_own_property(PropertyKey const&) const;
    ThrowCompletionOr<bool> set_integrity_level(IntegrityLevel);
    ThrowCompletionOr<bool> test_integrity_level(IntegrityLevel) const;
    ThrowCompletionOr<MarkedValueList> enumerable_own_property_names(PropertyKind kind) const;
    ThrowCompletionOr<Object*> copy_data_properties(Value source, HashTable<PropertyKey> const& seen_names, GlobalObject& global_object);

    PrivateElement* private_element_find(PrivateName const& name);
    ThrowCompletionOr<void> private_field_add(PrivateName const& name, Value value);
    ThrowCompletionOr<void> private_method_or_accessor_add(PrivateElement element);
    ThrowCompletionOr<Value> private_get(PrivateName const& name);
    ThrowCompletionOr<void> private_set(PrivateName const& name, Value value);
    ThrowCompletionOr<void> define_field(Variant<PropertyKey, PrivateName> name, ECMAScriptFunctionObject* initializer);

    // 10.1 Ordinary Object Internal Methods and Internal Slots, https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots

    virtual ThrowCompletionOr<Object*> internal_get_prototype_of() const;
    virtual ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype);
    virtual ThrowCompletionOr<bool> internal_is_extensible() const;
    virtual ThrowCompletionOr<bool> internal_prevent_extensions();
    virtual ThrowCompletionOr<Optional<PropertyDescriptor>> internal_get_own_property(PropertyKey const&) const;
    virtual ThrowCompletionOr<bool> internal_define_own_property(PropertyKey const&, PropertyDescriptor const&);
    virtual ThrowCompletionOr<bool> internal_has_property(PropertyKey const&) const;
    virtual ThrowCompletionOr<Value> internal_get(PropertyKey const&, Value receiver) const;
    virtual ThrowCompletionOr<bool> internal_set(PropertyKey const&, Value value, Value receiver);
    virtual ThrowCompletionOr<bool> internal_delete(PropertyKey const&);
    virtual ThrowCompletionOr<MarkedValueList> internal_own_property_keys() const;

    ThrowCompletionOr<bool> ordinary_set_with_own_descriptor(PropertyKey const&, Value, Value, Optional<PropertyDescriptor>);

    // 10.4.7 Immutable Prototype Exotic Objects, https://tc39.es/ecma262/#sec-immutable-prototype-exotic-objects

    ThrowCompletionOr<bool> set_immutable_prototype(Object* prototype);

    // 20.1 Object Objects, https://tc39.es/ecma262/#sec-object-objects

    ThrowCompletionOr<Object*> define_properties(Value properties);

    // Implementation-specific storage abstractions

    Optional<ValueAndAttributes> storage_get(PropertyKey const&) const;
    bool storage_has(PropertyKey const&) const;
    void storage_set(PropertyKey const&, ValueAndAttributes const&);
    void storage_delete(PropertyKey const&);

    // Non-standard methods

    Value get_without_side_effects(const PropertyKey&) const;

    void define_direct_property(PropertyKey const& property_name, Value value, PropertyAttributes attributes) { storage_set(property_name, { value, attributes }); };
    void define_direct_accessor(PropertyKey const&, FunctionObject* getter, FunctionObject* setter, PropertyAttributes attributes);

    void define_native_function(PropertyKey const&, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)>, i32 length, PropertyAttributes attributes);
    void define_native_accessor(PropertyKey const&, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> getter, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> setter, PropertyAttributes attributes);

    virtual bool is_function() const { return false; }
    virtual bool is_typed_array() const { return false; }
    virtual bool is_string_object() const { return false; }
    virtual bool is_global_object() const { return false; }
    virtual bool is_proxy_object() const { return false; }
    virtual bool is_native_function() const { return false; }
    virtual bool is_ecmascript_function_object() const { return false; }

    // B.3.7 The [[IsHTMLDDA]] Internal Slot, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot
    virtual bool is_htmldda() const { return false; }

    bool has_parameter_map() const { return m_has_parameter_map; }
    void set_has_parameter_map() { m_has_parameter_map = true; }

    virtual const char* class_name() const override { return "Object"; }
    virtual void visit_edges(Cell::Visitor&) override;

    Value get_direct(size_t index) const { return m_storage[index]; }

    const IndexedProperties& indexed_properties() const { return m_indexed_properties; }
    IndexedProperties& indexed_properties() { return m_indexed_properties; }
    void set_indexed_property_elements(Vector<Value>&& values) { m_indexed_properties = IndexedProperties(move(values)); }

    Shape& shape() { return *m_shape; }
    Shape const& shape() const { return *m_shape; }

    GlobalObject& global_object() const { return *shape().global_object(); }

    void ensure_shape_is_unique();

    template<typename T>
    bool fast_is() const = delete;

protected:
    enum class GlobalObjectTag { Tag };
    enum class ConstructWithoutPrototypeTag { Tag };
    explicit Object(GlobalObjectTag);
    Object(ConstructWithoutPrototypeTag, GlobalObject&);

    void set_prototype(Object*);

    // [[Extensible]]
    bool m_is_extensible { true };

    // [[ParameterMap]]
    bool m_has_parameter_map { false };

private:
    void set_shape(Shape& shape) { m_shape = &shape; }

    Object* prototype() { return shape().prototype(); }
    Object const* prototype() const { return shape().prototype(); }

    Shape* m_shape { nullptr };
    Vector<Value> m_storage;
    IndexedProperties m_indexed_properties;
    OwnPtr<Vector<PrivateElement>> m_private_elements; // [[PrivateElements]]
};

}
