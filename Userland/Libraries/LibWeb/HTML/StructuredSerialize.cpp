/*
 * Copyright (c) 2022, Daniel Ehrenberg <dan@littledan.dev>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
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
    // Unused, for ease of catching bugs.
    Empty,

    // UndefinedPrimitive is serialized indicating that the Type is Undefined, no value is serialized.
    UndefinedPrimitive,

    // NullPrimitive is serialized indicating that the Type is Null, no value is serialized.
    NullPrimitive,

    // Following u32 is the boolean value.
    BooleanPrimitive,

    // Following two u32s are the double value.
    NumberPrimitive,

    // The BigIntPrimitive is serialized as a string in base 10 representation.
    // Following two u32s representing the length of the string, then the following u32s, equal to size, is the string representation.
    BigIntPrimitive,

    // Following two u32s representing the length of the string, then the following u32s, equal to size, is the string representation.
    StringPrimitive,

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

    WebIDL::ExceptionOr<void> serialize(JS::Value value)
    {
        if (value.is_undefined()) {
            m_serialized.append(ValueTag::UndefinedPrimitive);
        } else if (value.is_null()) {
            m_serialized.append(ValueTag::NullPrimitive);
        } else if (value.is_boolean()) {
            m_serialized.append(ValueTag::BooleanPrimitive);
            m_serialized.append(static_cast<u32>(value.as_bool()));
        } else if (value.is_number()) {
            m_serialized.append(ValueTag::NumberPrimitive);
            double number = value.as_double();
            m_serialized.append(bit_cast<u32*>(&number), 2);
        } else if (value.is_bigint()) {
            m_serialized.append(ValueTag::BigIntPrimitive);
            auto& val = value.as_bigint();
            TRY(serialize_string(m_serialized, TRY_OR_THROW_OOM(m_vm, val.to_string())));
        } else if (value.is_string()) {
            m_serialized.append(ValueTag::StringPrimitive);
            TRY(serialize_string(m_serialized, value.as_string()));
        } else {
            // TODO: Define many more types
            m_error = "Unsupported type"sv;
        }

        // Second pass: Update the objects to point to other objects in memory

        return {};
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

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, String const& string)
    {
        u64 const size = string.code_points().length();
        // Append size of the string to the serialized structure.
        TRY_OR_THROW_OOM(m_vm, vector.try_append(bit_cast<u32*>(&size), 2));
        for (auto code_point : string.code_points()) {
            // Append each code point to the serialized structure.
            TRY_OR_THROW_OOM(m_vm, vector.try_append(code_point));
        }
        return {};
    }

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, JS::PrimitiveString const& primitive_string)
    {
        auto string = TRY(Bindings::throw_dom_exception_if_needed(m_vm, [&primitive_string]() {
            return primitive_string.utf8_string();
        }));
        TRY(serialize_string(vector, string));
        return {};
    }
};

class Deserializer {
public:
    Deserializer(JS::VM& vm, JS::Realm& target_realm, SerializationRecord const& v)
        : m_vm(vm)
        , m_vector(v)
        , m_memory(target_realm.heap())
    {
    }

    WebIDL::ExceptionOr<void> deserialize()
    {
        // First pass: fill up the memory with new values
        u32 position = 0;
        while (position < m_vector.size()) {
            switch (m_vector[position++]) {
            case ValueTag::UndefinedPrimitive: {
                m_memory.append(JS::js_undefined());
                break;
            }
            case ValueTag::NullPrimitive: {
                m_memory.append(JS::js_null());
                break;
            }
            case ValueTag::BooleanPrimitive: {
                m_memory.append(JS::Value(static_cast<bool>(m_vector[position++])));
                break;
            }
            case ValueTag::NumberPrimitive: {
                u32 bits[2];
                bits[0] = m_vector[position++];
                bits[1] = m_vector[position++];
                double value = *bit_cast<double*>(&bits);
                m_memory.append(JS::Value(value));
                break;
            }
            case ValueTag::BigIntPrimitive: {
                auto big_int = TRY(deserialize_big_int_primitive(m_vm, m_vector, position));
                m_memory.append(JS::Value { big_int });
                break;
            }
            case ValueTag::StringPrimitive: {
                auto string = TRY(deserialize_string_primitive(m_vm, m_vector, position));
                m_memory.append(JS::Value { string });
                break;
            }
            default:
                m_error = "Unsupported type"sv;
                break;
            }
        }
        return {};
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

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::PrimitiveString>> deserialize_string_primitive(JS::VM& vm, Vector<u32> const& vector, u32& position)
    {
        u32 size_bits[2];
        size_bits[0] = vector[position++];
        size_bits[1] = vector[position++];
        u64 const size = *bit_cast<u64*>(&size_bits);

        u8 bits[size];
        for (u32 i = 0; i < size; ++i)
            bits[i] = vector[position++];

        ReadonlyBytes const bytes = { bits, size };

        return TRY(Bindings::throw_dom_exception_if_needed(vm, [&vm, &bytes]() {
            return JS::PrimitiveString::create(vm, StringView { bytes });
        }));
    }

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::BigInt>> deserialize_big_int_primitive(JS::VM& vm, Vector<u32> const& vector, u32& position)
    {
        auto string = TRY(deserialize_string_primitive(vm, vector, position));
        auto string_view = TRY(Bindings::throw_dom_exception_if_needed(vm, [&string]() {
            return string->utf8_string_view();
        }));
        return JS::BigInt::create(vm, ::Crypto::SignedBigInteger::from_base(10, string_view.substring_view(0, string_view.length() - 1)));
    }
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
    TRY(serializer.serialize(value));
    return serializer.result(); // TODO: Avoid several copies of vector
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structureddeserialize
WebIDL::ExceptionOr<JS::Value> structured_deserialize(JS::VM& vm, SerializationRecord const& serialized, JS::Realm& target_realm, Optional<SerializationMemory> memory)
{
    // FIXME: Do the spec steps
    (void)memory;

    Deserializer deserializer(vm, target_realm, serialized);
    TRY(deserializer.deserialize());
    return deserializer.result();
}

}
