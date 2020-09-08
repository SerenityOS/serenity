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

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

const u32 SPARSE_ARRAY_THRESHOLD = 200;
const u32 MIN_PACKED_RESIZE_AMOUNT = 20;

struct ValueAndAttributes {
    Value value;
    PropertyAttributes attributes { default_attributes };
};

class IndexedProperties;
class IndexedPropertyIterator;
class GenericIndexedPropertyStorage;

class IndexedPropertyStorage {
public:
    virtual ~IndexedPropertyStorage() {};

    virtual bool has_index(u32 index) const = 0;
    virtual Optional<ValueAndAttributes> get(u32 index) const = 0;
    virtual void put(u32 index, Value value, PropertyAttributes attributes = default_attributes) = 0;
    virtual void remove(u32 index) = 0;

    virtual void insert(u32 index, Value value, PropertyAttributes attributes = default_attributes) = 0;
    virtual ValueAndAttributes take_first() = 0;
    virtual ValueAndAttributes take_last() = 0;

    virtual size_t size() const = 0;
    virtual size_t array_like_size() const = 0;
    virtual void set_array_like_size(size_t new_size) = 0;

    virtual bool is_simple_storage() const { return false; }
};

class SimpleIndexedPropertyStorage final : public IndexedPropertyStorage {
public:
    SimpleIndexedPropertyStorage() = default;
    explicit SimpleIndexedPropertyStorage(Vector<Value>&& initial_values);

    virtual bool has_index(u32 index) const override;
    virtual Optional<ValueAndAttributes> get(u32 index) const override;
    virtual void put(u32 index, Value value, PropertyAttributes attributes = default_attributes) override;
    virtual void remove(u32 index) override;

    virtual void insert(u32 index, Value value, PropertyAttributes attributes = default_attributes) override;
    virtual ValueAndAttributes take_first() override;
    virtual ValueAndAttributes take_last() override;

    virtual size_t size() const override { return m_packed_elements.size(); }
    virtual size_t array_like_size() const override { return m_array_size; }
    virtual void set_array_like_size(size_t new_size) override;

    virtual bool is_simple_storage() const override { return true; }
    const Vector<Value>& elements() const { return m_packed_elements; }

private:
    friend GenericIndexedPropertyStorage;

    size_t m_array_size { 0 };
    Vector<Value> m_packed_elements;
};

class GenericIndexedPropertyStorage final : public IndexedPropertyStorage {
public:
    explicit GenericIndexedPropertyStorage(SimpleIndexedPropertyStorage&&);

    virtual bool has_index(u32 index) const override;
    virtual Optional<ValueAndAttributes> get(u32 index) const override;
    virtual void put(u32 index, Value value, PropertyAttributes attributes = default_attributes) override;
    virtual void remove(u32 index) override;

    virtual void insert(u32 index, Value value, PropertyAttributes attributes = default_attributes) override;
    virtual ValueAndAttributes take_first() override;
    virtual ValueAndAttributes take_last() override;

    virtual size_t size() const override { return m_packed_elements.size() + m_sparse_elements.size(); }
    virtual size_t array_like_size() const override { return m_array_size; }
    virtual void set_array_like_size(size_t new_size) override;

    const Vector<ValueAndAttributes>& packed_elements() const { return m_packed_elements; }
    const HashMap<u32, ValueAndAttributes>& sparse_elements() const { return m_sparse_elements; }

private:
    size_t m_array_size { 0 };
    Vector<ValueAndAttributes> m_packed_elements;
    HashMap<u32, ValueAndAttributes> m_sparse_elements;
};

class IndexedPropertyIterator {
public:
    IndexedPropertyIterator(const IndexedProperties&, u32 starting_index, bool skip_empty);

    IndexedPropertyIterator& operator++();
    IndexedPropertyIterator& operator*();
    bool operator!=(const IndexedPropertyIterator&) const;

    u32 index() const { return m_index; };
    ValueAndAttributes value_and_attributes(Object* this_object, bool evaluate_accessors = true);

private:
    const IndexedProperties& m_indexed_properties;
    u32 m_index;
    bool m_skip_empty;
};

class IndexedProperties {
public:
    IndexedProperties() = default;

    IndexedProperties(Vector<Value>&& values)
        : m_storage(make<SimpleIndexedPropertyStorage>(move(values)))
    {
    }

    bool has_index(u32 index) const { return m_storage->has_index(index); }
    Optional<ValueAndAttributes> get(Object* this_object, u32 index, bool evaluate_accessors = true) const;
    void put(Object* this_object, u32 index, Value value, PropertyAttributes attributes = default_attributes, bool evaluate_accessors = true);
    bool remove(u32 index);

    void insert(u32 index, Value value, PropertyAttributes attributes = default_attributes);
    ValueAndAttributes take_first(Object* this_object);
    ValueAndAttributes take_last(Object* this_object);

    void append(Value value, PropertyAttributes attributes = default_attributes) { put(nullptr, array_like_size(), value, attributes, false); }
    void append_all(Object* this_object, const IndexedProperties& properties, bool evaluate_accessors = true);

    IndexedPropertyIterator begin(bool skip_empty = true) const { return IndexedPropertyIterator(*this, 0, skip_empty); };
    IndexedPropertyIterator end() const { return IndexedPropertyIterator(*this, array_like_size(), false); };

    size_t size() const { return m_storage->size(); }
    bool is_empty() const { return size() == 0; }
    size_t array_like_size() const { return m_storage->array_like_size(); }
    void set_array_like_size(size_t);

    Vector<ValueAndAttributes> values_unordered() const;

private:
    void switch_to_generic_storage();

    NonnullOwnPtr<IndexedPropertyStorage> m_storage { make<SimpleIndexedPropertyStorage>() };
};

}
