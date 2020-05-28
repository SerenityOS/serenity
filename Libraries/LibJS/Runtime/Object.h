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

class Object : public Cell {
public:
    static Object* create_empty(Interpreter&, GlobalObject&);

    explicit Object(Object* prototype);
    virtual ~Object();

    enum class GetOwnPropertyMode {
        Key,
        Value,
        KeyAndValue,
    };

    enum class PutOwnPropertyMode {
        Put,
        DefineProperty,
    };

    Shape& shape() { return *m_shape; }
    const Shape& shape() const { return *m_shape; }

    Value get(PropertyName) const;

    bool has_property(PropertyName) const;
    bool has_own_property(PropertyName) const;

    bool put(PropertyName, Value);

    Value get_own_property(const Object& this_object, PropertyName) const;
    Value get_own_properties(const Object& this_object, GetOwnPropertyMode, u8 attributes = default_attributes) const;
    Value get_own_property_descriptor(PropertyName) const;

    bool define_property(const FlyString& property_name, const Object& descriptor, bool throw_exceptions = true);
    bool define_property(PropertyName, Value value, u8 attributes = default_attributes, bool throw_exceptions = true);

    bool define_native_function(const FlyString& property_name, AK::Function<Value(Interpreter&)>, i32 length = 0, u8 attribute = default_attributes);
    bool define_native_property(const FlyString& property_name, AK::Function<Value(Interpreter&)> getter, AK::Function<void(Interpreter&, Value)> setter, u8 attribute = default_attributes);

    Value delete_property(PropertyName);

    virtual bool is_array() const { return false; }
    virtual bool is_boolean() const { return false; }
    virtual bool is_date() const { return false; }
    virtual bool is_error() const { return false; }
    virtual bool is_function() const { return false; }
    virtual bool is_native_function() const { return false; }
    virtual bool is_bound_function() const { return false; }
    virtual bool is_native_property() const { return false; }
    virtual bool is_string_object() const { return false; }
    virtual bool is_symbol_object() const { return false; }

    virtual const char* class_name() const override { return "Object"; }
    virtual void visit_children(Cell::Visitor&) override;

    Object* prototype();
    const Object* prototype() const;
    void set_prototype(Object*);
    bool has_prototype(const Object* prototype) const;

    virtual Value value_of() const { return Value(const_cast<Object*>(this)); }
    virtual Value to_primitive(Value::PreferredType preferred_type = Value::PreferredType::Default) const;
    virtual Value to_string() const;

    Value get_direct(size_t index) const { return m_storage[index]; }

    const IndexedProperties& indexed_properties() const { return m_indexed_properties; }
    IndexedProperties& indexed_properties() { return m_indexed_properties; }
    void set_indexed_property_elements(Vector<Value>&& values) { m_indexed_properties = IndexedProperties(move(values)); }

    Value invoke(const FlyString& property_name, Optional<MarkedValueList> arguments = {}) const;

private:
    virtual Value get_by_index(u32 property_index) const;
    virtual bool put_by_index(u32 property_index, Value);
    bool put_own_property(Object& this_object, const FlyString& property_name, Value, u8 attributes, PutOwnPropertyMode = PutOwnPropertyMode::Put, bool throw_exceptions = true);
    bool put_own_property_by_index(Object& this_object, u32 property_index, Value, u8 attributes, PutOwnPropertyMode = PutOwnPropertyMode::Put, bool throw_exceptions = true);

    void set_shape(Shape&);
    void ensure_shape_is_unique();

    Shape* m_shape { nullptr };
    Vector<Value> m_storage;
    IndexedProperties m_indexed_properties;
};

}
