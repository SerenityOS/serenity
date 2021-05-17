/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/IndexedProperties.h>

namespace JS {

constexpr const size_t SPARSE_ARRAY_HOLE_THRESHOLD = 200;
constexpr const size_t LENGTH_SETTER_GENERIC_STORAGE_THRESHOLD = 4 * MiB;

SimpleIndexedPropertyStorage::SimpleIndexedPropertyStorage(Vector<Value>&& initial_values)
    : m_array_size(initial_values.size())
    , m_packed_elements(move(initial_values))
{
}

bool SimpleIndexedPropertyStorage::has_index(u32 index) const
{
    return index < m_array_size && !m_packed_elements[index].is_empty();
}

Optional<ValueAndAttributes> SimpleIndexedPropertyStorage::get(u32 index) const
{
    if (index >= m_array_size)
        return {};
    return ValueAndAttributes { m_packed_elements[index], default_attributes };
}

void SimpleIndexedPropertyStorage::grow_storage_if_needed()
{
    if (m_array_size <= m_packed_elements.size())
        return;
    // Grow storage by 25% at a time.
    m_packed_elements.resize(m_array_size + (m_array_size / 4));
}

void SimpleIndexedPropertyStorage::put(u32 index, Value value, PropertyAttributes attributes)
{
    VERIFY(attributes == default_attributes);

    if (index >= m_array_size) {
        m_array_size = index + 1;
        grow_storage_if_needed();
    }
    m_packed_elements[index] = value;
}

void SimpleIndexedPropertyStorage::remove(u32 index)
{
    if (index < m_array_size)
        m_packed_elements[index] = {};
}

void SimpleIndexedPropertyStorage::insert(u32 index, Value value, PropertyAttributes attributes)
{
    VERIFY(attributes == default_attributes);
    m_array_size++;
    m_packed_elements.insert(index, value);
}

ValueAndAttributes SimpleIndexedPropertyStorage::take_first()
{
    m_array_size--;
    return { m_packed_elements.take_first(), default_attributes };
}

ValueAndAttributes SimpleIndexedPropertyStorage::take_last()
{
    m_array_size--;
    auto last_element = m_packed_elements[m_array_size];
    m_packed_elements[m_array_size] = {};
    return { last_element, default_attributes };
}

void SimpleIndexedPropertyStorage::set_array_like_size(size_t new_size)
{
    m_array_size = new_size;
    m_packed_elements.resize(new_size);
}

GenericIndexedPropertyStorage::GenericIndexedPropertyStorage(SimpleIndexedPropertyStorage&& storage)
{
    m_array_size = storage.array_like_size();
    for (size_t i = 0; i < storage.m_packed_elements.size(); ++i) {
        m_sparse_elements.set(i, { storage.m_packed_elements[i], default_attributes });
    }
}

bool GenericIndexedPropertyStorage::has_index(u32 index) const
{
    return m_sparse_elements.contains(index);
}

Optional<ValueAndAttributes> GenericIndexedPropertyStorage::get(u32 index) const
{
    if (index >= m_array_size)
        return {};
    return m_sparse_elements.get(index);
}

void GenericIndexedPropertyStorage::put(u32 index, Value value, PropertyAttributes attributes)
{
    if (index >= m_array_size)
        m_array_size = index + 1;
    m_sparse_elements.set(index, { value, attributes });
}

void GenericIndexedPropertyStorage::remove(u32 index)
{
    if (index >= m_array_size)
        return;
    if (index + 1 == m_array_size) {
        take_last();
        return;
    }
    m_sparse_elements.remove(index);
}

void GenericIndexedPropertyStorage::insert(u32 index, Value value, PropertyAttributes attributes)
{
    if (index >= m_array_size) {
        put(index, value, attributes);
        return;
    }

    m_array_size++;

    if (!m_sparse_elements.is_empty()) {
        HashMap<u32, ValueAndAttributes> new_sparse_elements;
        for (auto& entry : m_sparse_elements)
            new_sparse_elements.set(entry.key >= index ? entry.key + 1 : entry.key, entry.value);
        m_sparse_elements = move(new_sparse_elements);
    }

    m_sparse_elements.set(index, { value, attributes });
}

ValueAndAttributes GenericIndexedPropertyStorage::take_first()
{
    VERIFY(m_array_size > 0);
    m_array_size--;

    auto indices = m_sparse_elements.keys();
    quick_sort(indices);

    auto it = m_sparse_elements.find(indices.first());
    auto first_element = it->value;
    m_sparse_elements.remove(it);
    return first_element;
}

ValueAndAttributes GenericIndexedPropertyStorage::take_last()
{
    VERIFY(m_array_size > 0);
    m_array_size--;

    auto result = m_sparse_elements.get(m_array_size);
    if (!result.has_value())
        return {};
    m_sparse_elements.remove(m_array_size);
    return result.value();
}

void GenericIndexedPropertyStorage::set_array_like_size(size_t new_size)
{
    m_array_size = new_size;

    HashMap<u32, ValueAndAttributes> new_sparse_elements;
    for (auto& entry : m_sparse_elements) {
        if (entry.key < new_size)
            new_sparse_elements.set(entry.key, entry.value);
    }
    m_sparse_elements = move(new_sparse_elements);
}

IndexedPropertyIterator::IndexedPropertyIterator(const IndexedProperties& indexed_properties, u32 staring_index, bool skip_empty)
    : m_indexed_properties(indexed_properties)
    , m_index(staring_index)
    , m_skip_empty(skip_empty)
{
    if (m_skip_empty)
        skip_empty_indices();
}

IndexedPropertyIterator& IndexedPropertyIterator::operator++()
{
    m_index++;

    if (m_skip_empty)
        skip_empty_indices();

    return *this;
}

IndexedPropertyIterator& IndexedPropertyIterator::operator*()
{
    return *this;
}

bool IndexedPropertyIterator::operator!=(const IndexedPropertyIterator& other) const
{
    return m_index != other.m_index;
}

ValueAndAttributes IndexedPropertyIterator::value_and_attributes(Object* this_object, bool evaluate_accessors)
{
    if (m_index < m_indexed_properties.array_like_size())
        return m_indexed_properties.get(this_object, m_index, evaluate_accessors).value_or({});
    return {};
}

void IndexedPropertyIterator::skip_empty_indices()
{
    auto indices = m_indexed_properties.indices();
    for (auto i : indices) {
        if (i < m_index)
            continue;
        m_index = i;
        return;
    }
    m_index = m_indexed_properties.array_like_size();
}

Optional<ValueAndAttributes> IndexedProperties::get(Object* this_object, u32 index, bool evaluate_accessors) const
{
    auto result = m_storage->get(index);
    if (!evaluate_accessors)
        return result;
    if (!result.has_value())
        return {};
    auto& value = result.value();
    if (value.value.is_accessor()) {
        VERIFY(this_object);
        auto& accessor = value.value.as_accessor();
        return ValueAndAttributes { accessor.call_getter(this_object), value.attributes };
    }
    return result;
}

void IndexedProperties::put(Object* this_object, u32 index, Value value, PropertyAttributes attributes, bool evaluate_accessors)
{
    if (m_storage->is_simple_storage() && (attributes != default_attributes || index > (array_like_size() + SPARSE_ARRAY_HOLE_THRESHOLD))) {
        switch_to_generic_storage();
    }

    if (m_storage->is_simple_storage() || !evaluate_accessors) {
        m_storage->put(index, value, attributes);
        return;
    }

    auto value_here = m_storage->get(index);
    if (value_here.has_value() && value_here.value().value.is_accessor()) {
        VERIFY(this_object);
        value_here.value().value.as_accessor().call_setter(this_object, value);
    } else {
        m_storage->put(index, value, attributes);
    }
}

bool IndexedProperties::remove(u32 index)
{
    auto result = m_storage->get(index);
    if (!result.has_value())
        return true;
    if (!result.value().attributes.is_configurable())
        return false;
    m_storage->remove(index);
    return true;
}

void IndexedProperties::insert(u32 index, Value value, PropertyAttributes attributes)
{
    if (m_storage->is_simple_storage()) {
        if (attributes != default_attributes
            || index > (array_like_size() + SPARSE_ARRAY_HOLE_THRESHOLD)) {
            switch_to_generic_storage();
        }
    }
    m_storage->insert(index, value, attributes);
}

ValueAndAttributes IndexedProperties::take_first(Object* this_object)
{
    auto first = m_storage->take_first();
    if (first.value.is_accessor())
        return { first.value.as_accessor().call_getter(this_object), first.attributes };
    return first;
}

ValueAndAttributes IndexedProperties::take_last(Object* this_object)
{
    auto last = m_storage->take_last();
    if (last.value.is_accessor())
        return { last.value.as_accessor().call_getter(this_object), last.attributes };
    return last;
}

void IndexedProperties::append_all(Object* this_object, const IndexedProperties& properties, bool evaluate_accessors)
{
    if (m_storage->is_simple_storage() && !properties.m_storage->is_simple_storage())
        switch_to_generic_storage();

    for (auto it = properties.begin(false); it != properties.end(); ++it) {
        const auto& element = it.value_and_attributes(this_object, evaluate_accessors);
        if (this_object && this_object->vm().exception())
            return;
        m_storage->put(m_storage->array_like_size(), element.value, element.attributes);
    }
}

void IndexedProperties::set_array_like_size(size_t new_size)
{
    auto current_array_like_size = array_like_size();

    // We can't use simple storage for lengths that don't fit in an i32.
    // Also, to avoid gigantic unused storage allocations, let's put an (arbitrary) 4M cap on simple storage here.
    // This prevents something like "a = []; a.length = 0x80000000;" from allocating 2G entries.
    if (m_storage->is_simple_storage()
        && (new_size > NumericLimits<i32>::max()
            || (current_array_like_size < LENGTH_SETTER_GENERIC_STORAGE_THRESHOLD && new_size > LENGTH_SETTER_GENERIC_STORAGE_THRESHOLD))) {
        switch_to_generic_storage();
    }

    m_storage->set_array_like_size(new_size);
}

Vector<u32> IndexedProperties::indices() const
{
    if (m_storage->is_simple_storage()) {
        const auto& storage = static_cast<const SimpleIndexedPropertyStorage&>(*m_storage);
        const auto& elements = storage.elements();
        Vector<u32> indices;
        indices.ensure_capacity(storage.array_like_size());
        for (size_t i = 0; i < elements.size(); ++i) {
            if (!elements.at(i).is_empty())
                indices.unchecked_append(i);
        }
        return indices;
    }
    const auto& storage = static_cast<const GenericIndexedPropertyStorage&>(*m_storage);
    auto indices = storage.sparse_elements().keys();
    quick_sort(indices);
    return indices;
}

void IndexedProperties::switch_to_generic_storage()
{
    auto& storage = static_cast<SimpleIndexedPropertyStorage&>(*m_storage);
    m_storage = make<GenericIndexedPropertyStorage>(move(storage));
}

}
