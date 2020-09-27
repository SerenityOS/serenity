/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/IndexedProperties.h>

namespace JS {

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

void SimpleIndexedPropertyStorage::put(u32 index, Value value, PropertyAttributes attributes)
{
    ASSERT(attributes == default_attributes);
    ASSERT(index < SPARSE_ARRAY_THRESHOLD);

    if (index >= m_array_size) {
        m_array_size = index + 1;
        if (index >= m_packed_elements.size())
            m_packed_elements.resize(index + MIN_PACKED_RESIZE_AMOUNT >= SPARSE_ARRAY_THRESHOLD ? SPARSE_ARRAY_THRESHOLD : index + MIN_PACKED_RESIZE_AMOUNT);
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
    ASSERT(attributes == default_attributes);
    ASSERT(index < SPARSE_ARRAY_THRESHOLD);
    m_array_size++;
    ASSERT(m_array_size <= SPARSE_ARRAY_THRESHOLD);
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
    ASSERT(new_size <= SPARSE_ARRAY_THRESHOLD);
    m_array_size = new_size;
    m_packed_elements.resize(new_size);
}

GenericIndexedPropertyStorage::GenericIndexedPropertyStorage(SimpleIndexedPropertyStorage&& storage)
{
    m_array_size = storage.array_like_size();
    for (auto& element : move(storage.m_packed_elements))
        m_packed_elements.append({ element, default_attributes });
}

bool GenericIndexedPropertyStorage::has_index(u32 index) const
{
    if (index < SPARSE_ARRAY_THRESHOLD)
        return index < m_packed_elements.size() && !m_packed_elements[index].value.is_empty();
    return m_sparse_elements.contains(index);
}

Optional<ValueAndAttributes> GenericIndexedPropertyStorage::get(u32 index) const
{
    if (index >= m_array_size)
        return {};
    if (index < SPARSE_ARRAY_THRESHOLD) {
        if (index >= m_packed_elements.size())
            return {};
        return m_packed_elements[index];
    }
    return m_sparse_elements.get(index);
}

void GenericIndexedPropertyStorage::put(u32 index, Value value, PropertyAttributes attributes)
{
    if (index >= m_array_size)
        m_array_size = index + 1;
    if (index < SPARSE_ARRAY_THRESHOLD) {
        if (index >= m_packed_elements.size())
            m_packed_elements.resize(index + MIN_PACKED_RESIZE_AMOUNT >= SPARSE_ARRAY_THRESHOLD ? SPARSE_ARRAY_THRESHOLD : index + MIN_PACKED_RESIZE_AMOUNT);
        m_packed_elements[index] = { value, attributes };
    } else {
        m_sparse_elements.set(index, { value, attributes });
    }
}

void GenericIndexedPropertyStorage::remove(u32 index)
{
    if (index >= m_array_size)
        return;
    if (index + 1 == m_array_size) {
        take_last();
        return;
    }
    if (index < SPARSE_ARRAY_THRESHOLD) {
        if (index < m_packed_elements.size())
            m_packed_elements[index] = {};
    } else {
        m_sparse_elements.remove(index);
    }
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

    if (index < SPARSE_ARRAY_THRESHOLD) {
        m_packed_elements.insert(index, { value, attributes });
    } else {
        m_sparse_elements.set(index, { value, attributes });
    }
}

ValueAndAttributes GenericIndexedPropertyStorage::take_first()
{
    ASSERT(m_array_size > 0);
    m_array_size--;

    if (!m_sparse_elements.is_empty()) {
        HashMap<u32, ValueAndAttributes> new_sparse_elements;
        for (auto& entry : m_sparse_elements)
            new_sparse_elements.set(entry.key - 1, entry.value);
        m_sparse_elements = move(new_sparse_elements);
    }

    return m_packed_elements.take_first();
}

ValueAndAttributes GenericIndexedPropertyStorage::take_last()
{
    ASSERT(m_array_size > 0);
    m_array_size--;

    if (m_array_size <= SPARSE_ARRAY_THRESHOLD) {
        auto last_element = m_packed_elements[m_array_size];
        m_packed_elements[m_array_size] = {};
        return last_element;
    } else {
        auto result = m_sparse_elements.get(m_array_size);
        m_sparse_elements.remove(m_array_size);
        ASSERT(result.has_value());
        return result.value();
    }
}

void GenericIndexedPropertyStorage::set_array_like_size(size_t new_size)
{
    m_array_size = new_size;
    if (new_size < SPARSE_ARRAY_THRESHOLD) {
        m_packed_elements.resize(new_size);
        m_sparse_elements.clear();
    } else {
        m_packed_elements.resize(SPARSE_ARRAY_THRESHOLD);

        HashMap<u32, ValueAndAttributes> new_sparse_elements;
        for (auto& entry : m_sparse_elements) {
            if (entry.key < new_size)
                new_sparse_elements.set(entry.key, entry.value);
        }
        m_sparse_elements = move(new_sparse_elements);
    }
}

IndexedPropertyIterator::IndexedPropertyIterator(const IndexedProperties& indexed_properties, u32 staring_index, bool skip_empty)
    : m_indexed_properties(indexed_properties)
    , m_index(staring_index)
    , m_skip_empty(skip_empty)
{
    while (m_skip_empty && m_index < m_indexed_properties.array_like_size()) {
        if (m_indexed_properties.has_index(m_index))
            break;
        m_index++;
    }
}

IndexedPropertyIterator& IndexedPropertyIterator::operator++()
{
    m_index++;

    while (m_skip_empty && m_index < m_indexed_properties.array_like_size()) {
        if (m_indexed_properties.has_index(m_index))
            break;
        m_index++;
    };

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
        return m_indexed_properties.get(this_object, m_index, evaluate_accessors).value();
    return {};
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
        ASSERT(this_object);
        auto& accessor = value.value.as_accessor();
        return ValueAndAttributes { accessor.call_getter(this_object), value.attributes };
    }
    return result;
}

void IndexedProperties::put(Object* this_object, u32 index, Value value, PropertyAttributes attributes, bool evaluate_accessors)
{
    if (m_storage->is_simple_storage() && (index >= SPARSE_ARRAY_THRESHOLD || attributes != default_attributes))
        switch_to_generic_storage();
    if (m_storage->is_simple_storage() || !evaluate_accessors) {
        m_storage->put(index, value, attributes);
        return;
    }

    auto value_here = m_storage->get(index);
    if (value_here.has_value() && value_here.value().value.is_accessor()) {
        ASSERT(this_object);
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
    if (m_storage->is_simple_storage() && (index >= SPARSE_ARRAY_THRESHOLD || attributes != default_attributes || array_like_size() == SPARSE_ARRAY_THRESHOLD))
        switch_to_generic_storage();
    m_storage->insert(index, move(value), attributes);
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
    if (m_storage->is_simple_storage() && new_size > SPARSE_ARRAY_THRESHOLD)
        switch_to_generic_storage();
    m_storage->set_array_like_size(new_size);
}

Vector<ValueAndAttributes> IndexedProperties::values_unordered() const
{
    if (m_storage->is_simple_storage()) {
        const auto& elements = static_cast<const SimpleIndexedPropertyStorage&>(*m_storage).elements();
        Vector<ValueAndAttributes> with_attributes;
        for (auto& value : elements)
            with_attributes.append({ value, default_attributes });
        return with_attributes;
    }

    auto& storage = static_cast<const GenericIndexedPropertyStorage&>(*m_storage);
    auto values = storage.packed_elements();
    values.ensure_capacity(values.size() + storage.sparse_elements().size());
    for (auto& entry : storage.sparse_elements())
        values.unchecked_append(entry.value);
    return values;
}

void IndexedProperties::switch_to_generic_storage()
{
    auto& storage = static_cast<SimpleIndexedPropertyStorage&>(*m_storage);
    m_storage = make<GenericIndexedPropertyStorage>(move(storage));
}

}
