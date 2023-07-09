/*
 * Copyright (c) 2022, Daniel Ehrenberg <dan@littledan.dev>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

// Binary format:
// A list of adjacent shallow values, which may contain references to other
// values (noted by their position in the list, one value following another).
// This list represents the "memory" in the StructuredSerialize algorithm.
// The first item in the list is the root, i.e., the value of everything.
// The format is generally u32-aligned (hence this leaking out into the type)
// Each value has a length based on its type, as defined below.
//
// (Should more redundancy be added, e.g., for lengths/positions of values?)

enum ValueTag {
    // Unused, for ease of catching bugs
    Empty,

    // Following two u32s are the double value
    NumberPrimitive,

    // TODO: Define many more types

    // This tag or higher are understood to be errors
    ValueTagMax,
};

// Serializing and deserializing are each two passes:
// 1. Fill up the memory with all the values, but without translating references
// 2. Translate all the references into the appropriate form

class Serializer {
public:
    Serializer(JS::VM& vm)
        : m_vm(vm)
    {
    }

    void serialize(JS::Value value)
    {
        if (value.is_number()) {
            m_serialized.append(ValueTag::NumberPrimitive);
            double number = value.as_double();
            m_serialized.append(bit_cast<u32*>(&number), 2);
        } else {
            // TODO: Define many more types
            m_error = "Unsupported type"sv;
        }
    }

    WebIDL::ExceptionOr<Vector<u32>> result()
    {
        if (m_error.is_null())
            return m_serialized;
        return throw_completion(WebIDL::DataCloneError::create(*m_vm.current_realm(), m_error));
    }

private:
    AK::StringView m_error;
    SerializationMemory m_memory; // JS value -> index
    SerializationRecord m_serialized;
    JS::VM& m_vm;
};

class Deserializer {
public:
    Deserializer(JS::VM& vm, JS::Realm& target_realm, SerializationRecord const& v)
        : m_vm(vm)
        , m_vector(v)
        , m_memory(target_realm.heap())
    {
    }

    void deserialize()
    {
        // First pass: fill up the memory with new values
        u32 position = 0;
        while (position < m_vector.size()) {
            switch (m_vector[position++]) {
            case ValueTag::NumberPrimitive: {
                u32 bits[2];
                bits[0] = m_vector[position++];
                bits[1] = m_vector[position++];
                double value = *bit_cast<double*>(&bits);
                m_memory.append(JS::Value(value));
                break;
            }
            default:
                m_error = "Unsupported type"sv;
                return;
            }
        }

        // Second pass: Update the objects to point to other objects in memory
    }

    WebIDL::ExceptionOr<JS::Value> result()
    {
        if (m_error.is_null())
            return m_memory[0];
        return throw_completion(WebIDL::DataCloneError::create(*m_vm.current_realm(), m_error));
    }

private:
    JS::VM& m_vm;
    SerializationRecord const& m_vector;
    JS::MarkedVector<JS::Value> m_memory; // Index -> JS value
    StringView m_error;
};

// https://html.spec.whatwg.org/multipage/structured-data.html#structuredserialize
WebIDL::ExceptionOr<SerializationRecord> structured_serialize(JS::VM& vm, JS::Value value)
{
    // 1. Return ? StructuredSerializeInternal(value, false).
    return structured_serialize_internal(vm, value, false, {});
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeforstorage
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_for_storage(JS::VM& vm, JS::Value value)
{
    // 1. Return ? StructuredSerializeInternal(value, true).
    return structured_serialize_internal(vm, value, true, {});
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeinternal
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_internal(JS::VM& vm, JS::Value value, bool for_storage, Optional<SerializationMemory> memory)
{
    // FIXME: Do the spec steps
    (void)for_storage;
    (void)memory;

    Serializer serializer(vm);
    serializer.serialize(value);
    return serializer.result(); // TODO: Avoid several copies of vector
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structureddeserialize
WebIDL::ExceptionOr<JS::Value> structured_deserialize(JS::VM& vm, SerializationRecord const& serialized, JS::Realm& target_realm, Optional<SerializationMemory> memory)
{
    // FIXME: Do the spec steps
    (void)memory;

    Deserializer deserializer(vm, target_realm, serialized);
    deserializer.deserialize();
    return deserializer.result();
}

}
