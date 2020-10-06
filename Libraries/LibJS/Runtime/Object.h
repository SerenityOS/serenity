/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/IndexedProperties.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

#define JS_OBJECT(class_, base_class)                                   \
public:                                                                 \
    using Base = base_class;                                            \
    virtual const char* class_name() const override { return #class_; } \
    virtual bool inherits(const StringView& class_name) const override { return class_name == #class_ || Base::inherits(class_name); }

struct PropertyDescriptor {
    PropertyAttributes attributes;
    Value value;
    Function* getter { nullptr };
    Function* setter { nullptr };

    static PropertyDescriptor from_dictionary(VM&, const Object&);

    bool is_accessor_descriptor() const { return getter || setter; }
    bool is_data_descriptor() const { return !(value.is_empty() && !attributes.has_writable()); }
    bool is_generic_descriptor() const { return !is_accessor_descriptor() && !is_data_descriptor(); }
};

class Object : public Cell {
public:
    static Object* create_empty(GlobalObject&);

    explicit Object(Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~Object();

    virtual bool inherits(const StringView& class_name) const { return class_name == this->class_name(); }

    enum class PropertyKind {
        Key,
        Value,
        KeyAndValue,
    };

    enum class GetOwnPropertyReturnType {
        StringOnly,
        SymbolOnly,
    };

    enum class PutOwnPropertyMode {
        Put,
        DefineProperty,
    };

    Shape& shape() { return *m_shape; }
    const Shape& shape() const { return *m_shape; }

    GlobalObject& global_object() const { return shape().global_object(); }

    virtual Value get(const PropertyName&, Value receiver = {}) const;

    virtual bool has_property(const PropertyName&) const;
    bool has_own_property(const PropertyName&) const;

    virtual bool put(const PropertyName&, Value, Value receiver = {});

    Value get_own_property(const Object& this_object, PropertyName, Value receiver) const;
    Value get_own_properties(const Object& this_object, PropertyKind, bool only_enumerable_properties = false, GetOwnPropertyReturnType = GetOwnPropertyReturnType::StringOnly) const;
    virtual Optional<PropertyDescriptor> get_own_property_descriptor(const PropertyName&) const;
    Value get_own_property_descriptor_object(const PropertyName&) const;

    virtual bool define_property(const StringOrSymbol& property_name, const Object& descriptor, bool throw_exceptions = true);
    bool define_property(const PropertyName&, Value value, PropertyAttributes attributes = default_attributes, bool throw_exceptions = true);
    bool define_property_without_transition(const PropertyName&, Value value, PropertyAttributes attributes = default_attributes, bool throw_exceptions = true);
    bool define_accessor(const PropertyName&, Function& getter_or_setter, bool is_getter, PropertyAttributes attributes = default_attributes, bool throw_exceptions = true);

    bool define_native_function(const StringOrSymbol& property_name, AK::Function<Value(VM&, GlobalObject&)>, i32 length = 0, PropertyAttributes attributes = default_attributes);
    bool define_native_property(const StringOrSymbol& property_name, AK::Function<Value(VM&, GlobalObject&)> getter, AK::Function<void(VM&, GlobalObject&, Value)> setter, PropertyAttributes attributes = default_attributes);

    virtual Value delete_property(const PropertyName&);

    virtual bool is_array() const { return false; }
    virtual bool is_date() const { return false; }
    virtual bool is_error() const { return false; }
    virtual bool is_function() const { return false; }
    virtual bool is_native_function() const { return false; }
    virtual bool is_bound_function() const { return false; }
    virtual bool is_proxy_object() const { return false; }
    virtual bool is_regexp_object() const { return false; }
    virtual bool is_boolean_object() const { return false; }
    virtual bool is_string_object() const { return false; }
    virtual bool is_number_object() const { return false; }
    virtual bool is_symbol_object() const { return false; }
    virtual bool is_bigint_object() const { return false; }
    virtual bool is_string_iterator_object() const { return false; }
    virtual bool is_array_iterator_object() const { return false; }

    virtual const char* class_name() const override { return "Object"; }
    virtual void visit_children(Cell::Visitor&) override;

    virtual Object* prototype();
    virtual const Object* prototype() const;
    virtual bool set_prototype(Object* prototype);
    bool has_prototype(const Object* prototype) const;

    virtual bool is_extensible() const { return m_is_extensible; }
    virtual bool prevent_extensions();

    virtual Value value_of() const { return Value(const_cast<Object*>(this)); }
    virtual Value to_primitive(Value::PreferredType preferred_type = Value::PreferredType::Default) const;
    virtual Value to_string() const;

    Value get_direct(size_t index) const { return m_storage[index]; }

    const IndexedProperties& indexed_properties() const { return m_indexed_properties; }
    IndexedProperties& indexed_properties() { return m_indexed_properties; }
    void set_indexed_property_elements(Vector<Value>&& values) { m_indexed_properties = IndexedProperties(move(values)); }

    Value invoke(const StringOrSymbol& property_name, Optional<MarkedValueList> arguments = {});

    void ensure_shape_is_unique();

    void enable_transitions() { m_transitions_enabled = true; }
    void disable_transitions() { m_transitions_enabled = false; }

protected:
    enum class GlobalObjectTag { Tag };
    enum class ConstructWithoutPrototypeTag { Tag };
    explicit Object(GlobalObjectTag);
    Object(ConstructWithoutPrototypeTag, GlobalObject&);

private:
    virtual Value get_by_index(u32 property_index) const;
    virtual bool put_by_index(u32 property_index, Value);
    bool put_own_property(Object& this_object, const StringOrSymbol& property_name, Value, PropertyAttributes attributes, PutOwnPropertyMode = PutOwnPropertyMode::Put, bool throw_exceptions = true);
    bool put_own_property_by_index(Object& this_object, u32 property_index, Value, PropertyAttributes attributes, PutOwnPropertyMode = PutOwnPropertyMode::Put, bool throw_exceptions = true);

    Value call_native_property_getter(Object* this_object, Value property) const;
    void call_native_property_setter(Object* this_object, Value property, Value) const;

    void set_shape(Shape&);

    bool m_is_extensible { true };
    bool m_transitions_enabled { true };
    Shape* m_shape { nullptr };
    Vector<Value> m_storage;
    IndexedProperties m_indexed_properties;
};

}
