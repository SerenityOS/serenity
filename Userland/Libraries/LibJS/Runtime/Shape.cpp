/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

Shape* Shape::create_unique_clone() const
{
    auto new_shape = heap().allocate_without_realm<Shape>(m_realm);
    new_shape->m_unique = true;
    new_shape->m_prototype = m_prototype;
    ensure_property_table();
    new_shape->ensure_property_table();
    (*new_shape->m_property_table) = *m_property_table;
    new_shape->m_property_count = new_shape->m_property_table->size();
    return new_shape;
}

Shape* Shape::get_or_prune_cached_forward_transition(TransitionKey const& key)
{
    if (!m_forward_transitions)
        return nullptr;
    auto it = m_forward_transitions->find(key);
    if (it == m_forward_transitions->end())
        return nullptr;
    if (!it->value) {
        // The cached forward transition has gone stale (from garbage collection). Prune it.
        m_forward_transitions->remove(it);
        return nullptr;
    }
    return it->value;
}

Shape* Shape::get_or_prune_cached_prototype_transition(Object* prototype)
{
    if (!m_prototype_transitions)
        return nullptr;
    auto it = m_prototype_transitions->find(prototype);
    if (it == m_prototype_transitions->end())
        return nullptr;
    if (!it->value) {
        // The cached prototype transition has gone stale (from garbage collection). Prune it.
        m_prototype_transitions->remove(it);
        return nullptr;
    }
    return it->value;
}

Shape* Shape::create_put_transition(StringOrSymbol const& property_key, PropertyAttributes attributes)
{
    TransitionKey key { property_key, attributes };
    if (auto* existing_shape = get_or_prune_cached_forward_transition(key))
        return existing_shape;
    auto new_shape = heap().allocate_without_realm<Shape>(*this, property_key, attributes, TransitionType::Put);
    if (!m_forward_transitions)
        m_forward_transitions = make<HashMap<TransitionKey, WeakPtr<Shape>>>();
    m_forward_transitions->set(key, new_shape.ptr());
    return new_shape;
}

Shape* Shape::create_configure_transition(StringOrSymbol const& property_key, PropertyAttributes attributes)
{
    TransitionKey key { property_key, attributes };
    if (auto* existing_shape = get_or_prune_cached_forward_transition(key))
        return existing_shape;
    auto new_shape = heap().allocate_without_realm<Shape>(*this, property_key, attributes, TransitionType::Configure);
    if (!m_forward_transitions)
        m_forward_transitions = make<HashMap<TransitionKey, WeakPtr<Shape>>>();
    m_forward_transitions->set(key, new_shape.ptr());
    return new_shape;
}

Shape* Shape::create_prototype_transition(Object* new_prototype)
{
    if (auto* existing_shape = get_or_prune_cached_prototype_transition(new_prototype))
        return existing_shape;
    auto new_shape = heap().allocate_without_realm<Shape>(*this, new_prototype);
    if (!m_prototype_transitions)
        m_prototype_transitions = make<HashMap<GCPtr<Object>, WeakPtr<Shape>>>();
    m_prototype_transitions->set(new_prototype, new_shape.ptr());
    return new_shape;
}

Shape::Shape(Realm& realm)
    : m_realm(realm)
{
}

Shape::Shape(Shape& previous_shape, StringOrSymbol const& property_key, PropertyAttributes attributes, TransitionType transition_type)
    : m_realm(previous_shape.m_realm)
    , m_previous(&previous_shape)
    , m_property_key(property_key)
    , m_prototype(previous_shape.m_prototype)
    , m_property_count(transition_type == TransitionType::Put ? previous_shape.m_property_count + 1 : previous_shape.m_property_count)
    , m_attributes(attributes)
    , m_transition_type(transition_type)
{
}

Shape::Shape(Shape& previous_shape, Object* new_prototype)
    : m_realm(previous_shape.m_realm)
    , m_previous(&previous_shape)
    , m_prototype(new_prototype)
    , m_property_count(previous_shape.m_property_count)
    , m_transition_type(TransitionType::Prototype)
{
}

void Shape::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_realm);
    visitor.visit(m_prototype);
    visitor.visit(m_previous);
    m_property_key.visit_edges(visitor);
    if (m_property_table) {
        for (auto& it : *m_property_table)
            it.key.visit_edges(visitor);
    }
    visitor.ignore(m_prototype_transitions);
}

Optional<PropertyMetadata> Shape::lookup(StringOrSymbol const& property_key) const
{
    if (m_property_count == 0)
        return {};
    auto property = property_table().get(property_key);
    if (!property.has_value())
        return {};
    return property;
}

FLATTEN OrderedHashMap<StringOrSymbol, PropertyMetadata> const& Shape::property_table() const
{
    ensure_property_table();
    return *m_property_table;
}

void Shape::ensure_property_table() const
{
    if (m_property_table)
        return;
    m_property_table = make<OrderedHashMap<StringOrSymbol, PropertyMetadata>>();

    u32 next_offset = 0;

    Vector<Shape const&, 64> transition_chain;
    for (auto shape = m_previous; shape; shape = shape->m_previous) {
        if (shape->m_property_table) {
            *m_property_table = *shape->m_property_table;
            next_offset = shape->m_property_count;
            break;
        }
        transition_chain.append(*shape);
    }
    transition_chain.append(*this);

    for (auto const& shape : transition_chain.in_reverse()) {
        if (!shape.m_property_key.is_valid()) {
            // Ignore prototype transitions as they don't affect the key map.
            continue;
        }
        if (shape.m_transition_type == TransitionType::Put) {
            m_property_table->set(shape.m_property_key, { next_offset++, shape.m_attributes });
        } else if (shape.m_transition_type == TransitionType::Configure) {
            auto it = m_property_table->find(shape.m_property_key);
            VERIFY(it != m_property_table->end());
            it->value.attributes = shape.m_attributes;
        }
    }
}

void Shape::add_property_to_unique_shape(StringOrSymbol const& property_key, PropertyAttributes attributes)
{
    VERIFY(is_unique());
    VERIFY(m_property_table);
    VERIFY(!m_property_table->contains(property_key));
    m_property_table->set(property_key, { static_cast<u32>(m_property_table->size()), attributes });

    VERIFY(m_property_count < NumericLimits<u32>::max());
    ++m_property_count;

    ++m_unique_shape_serial_number;
}

void Shape::reconfigure_property_in_unique_shape(StringOrSymbol const& property_key, PropertyAttributes attributes)
{
    VERIFY(is_unique());
    VERIFY(m_property_table);
    auto it = m_property_table->find(property_key);
    VERIFY(it != m_property_table->end());
    it->value.attributes = attributes;
    m_property_table->set(property_key, it->value);

    ++m_unique_shape_serial_number;
}

void Shape::remove_property_from_unique_shape(StringOrSymbol const& property_key, size_t offset)
{
    VERIFY(is_unique());
    VERIFY(m_property_table);
    if (m_property_table->remove(property_key))
        --m_property_count;
    for (auto& it : *m_property_table) {
        VERIFY(it.value.offset != offset);
        if (it.value.offset > offset)
            --it.value.offset;
    }

    ++m_unique_shape_serial_number;
}

void Shape::add_property_without_transition(StringOrSymbol const& property_key, PropertyAttributes attributes)
{
    VERIFY(property_key.is_valid());
    ensure_property_table();
    if (m_property_table->set(property_key, { m_property_count, attributes }) == AK::HashSetResult::InsertedNewEntry) {
        VERIFY(m_property_count < NumericLimits<u32>::max());
        ++m_property_count;
    }
}

FLATTEN void Shape::add_property_without_transition(PropertyKey const& property_key, PropertyAttributes attributes)
{
    VERIFY(property_key.is_valid());
    add_property_without_transition(property_key.to_string_or_symbol(), attributes);
}

}
