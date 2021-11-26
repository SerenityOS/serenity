/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
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

class Shape final
    : public Cell
    , public Weakable<Shape> {
public:
    virtual ~Shape() override;

    enum class TransitionType {
        Invalid,
        Put,
        Configure,
        Prototype,
    };

    enum class ShapeWithoutGlobalObjectTag { Tag };

    explicit Shape(ShapeWithoutGlobalObjectTag);
    explicit Shape(Object& global_object);
    Shape(Shape& previous_shape, const StringOrSymbol& property_name, PropertyAttributes attributes, TransitionType);
    Shape(Shape& previous_shape, Object* new_prototype);

    Shape* create_put_transition(const StringOrSymbol&, PropertyAttributes attributes);
    Shape* create_configure_transition(const StringOrSymbol&, PropertyAttributes attributes);
    Shape* create_prototype_transition(Object* new_prototype);

    void add_property_without_transition(const StringOrSymbol&, PropertyAttributes);
    void add_property_without_transition(PropertyKey const&, PropertyAttributes);

    bool is_unique() const { return m_unique; }
    Shape* create_unique_clone() const;

    GlobalObject* global_object() const;

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
    virtual void visit_edges(Visitor&) override;

#ifdef JS_TRACK_ZOMBIE_CELLS
    virtual void did_become_zombie() override;
#endif

    Shape* get_or_prune_cached_forward_transition(TransitionKey const&);
    Shape* get_or_prune_cached_prototype_transition(Object* prototype);

    void ensure_property_table() const;

    PropertyAttributes m_attributes { 0 };
    TransitionType m_transition_type : 6 { TransitionType::Invalid };
    bool m_unique : 1 { false };

    Object* m_global_object { nullptr };

    mutable OwnPtr<HashMap<StringOrSymbol, PropertyMetadata>> m_property_table;

    HashMap<TransitionKey, WeakPtr<Shape>> m_forward_transitions;
    HashMap<Object*, WeakPtr<Shape>> m_prototype_transitions;
    Shape* m_previous { nullptr };
    StringOrSymbol m_property_name;
    Object* m_prototype { nullptr };
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
