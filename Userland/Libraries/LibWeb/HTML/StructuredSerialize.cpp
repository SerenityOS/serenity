/*
 * Copyright (c) 2022 Daniel Ehrenberg <dan@littledan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/HTML/StructuredSerialize.h>

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
    Serializer(JS::GlobalObject& global_object) : m_global_object(global_object) { }

    void serialize(JS::Value value) {
        if (value.is_number()) {
            m_serialized.append(ValueTag::NumberPrimitive);
            double number = value.as_double();
            m_serialized.append(bit_cast<u32*>(&number), 2);
        } else {
            // TODO: Define many more types
            m_error = "Unsupported type"sv;
        }
    }

    JS::ThrowCompletionOr<Vector<u32>> result() {
        if (m_error.is_null())
            return m_serialized;
        else
            // TODO: Replace with the proper DataCloneError DOM exception
            return m_global_object.vm().throw_completion<Bindings::DOMExceptionWrapper>(m_global_object, DOM::DataCloneError::create("Unsupported type"));
    }


  private:
    AK::StringView m_error;
    AK::HashTable<JS::Value, u32> m_memory;  // JS value -> index
    Vector<u32> m_serialized;
    JS::GlobalObject& m_global_object;
};

class Deserializer {
  public:
    Deserializer(JS::GlobalObject& global_object, const Vector<u32>& v)
        : m_global_object(global_object), m_vector(v) { }

    void deserialize() {
        // First pass: fill up the memory with new values
        u32 position = 0;
        while (position < m_vector.size()) {
            switch(m_vector[position++]) {
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

    ErrorOr<JS::Value> result() {
        if (m_error.is_null())
            return m_memory[0];
        else
            return AK::Error::from_string_literal(m_error);
    }

  private:
    JS::GlobalObject& m_global_object;
    const Vector<u32>& m_vector;
    Vector<JS::Value> m_memory;  // Index -> JS value
    StringView m_error;
};

JS::ThrowCompletionOr<Vector<u32>> structured_serialize(JS::GlobalObject& global_object, JS::Value value) {
    Serializer serializer(global_object);
    serializer.serialize(value);
    return serializer.result(); // TODO: Avoid several copies of vector
}

ErrorOr<JS::Value> structured_deserialize(JS::GlobalObject& global_object, const Vector<u32>& vector) {
    Deserializer deserializer(global_object, vector);
    deserializer.deserialize();
    return deserializer.result();
}

}
