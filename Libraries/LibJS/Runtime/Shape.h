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
#include <AK/OwnPtr.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/PropertyAttributes.h>
#include <LibJS/Runtime/StringOrSymbol.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

struct PropertyMetadata {
    size_t offset { 0 };
    PropertyAttributes attributes { 0 };
};

struct TransitionKey {
    StringOrSymbol property_name;
    PropertyAttributes attributes { 0 };

    bool operator==(const TransitionKey& other) const
    {
        return property_name == other.property_name && attributes == other.attributes;
    }
};

class Shape final : public Cell {
public:
    virtual ~Shape() override;

    enum class TransitionType {
        Invalid,
        Put,
        Configure,
        Prototype,
    };

    explicit Shape(GlobalObject&);
    Shape(Shape& previous_shape, const StringOrSymbol& property_name, PropertyAttributes attributes, TransitionType);
    Shape(Shape& previous_shape, Object* new_prototype);

    Shape* create_put_transition(const StringOrSymbol&, PropertyAttributes attributes);
    Shape* create_configure_transition(const StringOrSymbol&, PropertyAttributes attributes);
    Shape* create_prototype_transition(Object* new_prototype);

    void add_property_without_transition(const StringOrSymbol&, PropertyAttributes);

    bool is_unique() const { return m_unique; }
    Shape* create_unique_clone() const;

    GlobalObject& global_object() const { return m_global_object; }

    Object* prototype() { return m_prototype; }
    const Object* prototype() const { return m_prototype; }

    Optional<PropertyMetadata> lookup(const StringOrSymbol&) const;
    const HashMap<StringOrSymbol, PropertyMetadata>& property_table() const;
    size_t property_count() const;

    struct Property {
        StringOrSymbol key;
        PropertyMetadata value;
    };

    Vector<Property> property_table_ordered() const;

    void set_prototype_without_transition(Object* new_prototype) { m_prototype = new_prototype; }

    void remove_property_from_unique_shape(const StringOrSymbol&, size_t offset);
    void add_property_to_unique_shape(const StringOrSymbol&, PropertyAttributes attributes);
    void reconfigure_property_in_unique_shape(const StringOrSymbol& property_name, PropertyAttributes attributes);

private:
    virtual const char* class_name() const override { return "Shape"; }
    virtual void visit_children(Visitor&) override;

    void ensure_property_table() const;

    GlobalObject& m_global_object;

    mutable OwnPtr<HashMap<StringOrSymbol, PropertyMetadata>> m_property_table;

    HashMap<TransitionKey, Shape*> m_forward_transitions;
    Shape* m_previous { nullptr };
    StringOrSymbol m_property_name;
    PropertyAttributes m_attributes { 0 };
    bool m_unique { false };
    Object* m_prototype { nullptr };
    TransitionType m_transition_type { TransitionType::Invalid };
    size_t m_property_count { 0 };
};

}

template<>
struct AK::Traits<JS::TransitionKey> : public GenericTraits<JS::TransitionKey> {
    static unsigned hash(const JS::TransitionKey& key)
    {
        return pair_int_hash(key.attributes.bits(), Traits<JS::StringOrSymbol>::hash(key.property_name));
    }
};
