/*
 * Copyright (c) 2022, Daniel Ehrenberg <dan@littledan.dev>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringObject.h>
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

    BooleanObject,

    NumberObject,

    BigIntObject,

    StringObject,

    DateObject,

    RegExpObject,

    // TODO: Define many more types

    // This tag or higher are understood to be errors
    ValueTagMax,
};

// Serializing and deserializing are each two passes:
// 1. Fill up the memory with all the values, but without translating references
// 2. Translate all the references into the appropriate form

class Serializer {
public:
    Serializer(JS::VM& vm, SerializationMemory& memory)
        : m_vm(vm)
        , m_memory(memory)
    {
    }

    // https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeinternal
    WebIDL::ExceptionOr<SerializationRecord> serialize(JS::Value value)
    {
        // 2. If memory[value] exists, then return memory[value].
        // FIXME: Do callers actually need a copy? or can they get away with a range?
        if (m_memory.contains(value)) {
            auto range = m_memory.get(value).value();
            return m_serialized.span().slice(range.start, range.end);
        }

        // 3. Let deep be false.
        [[maybe_unused]] bool deep = false;

        bool return_primitive_type = true;
        // 4. If Type(value) is Undefined, Null, Boolean, Number, BigInt, or String, then return { [[Type]]: "primitive", [[Value]]: value }.
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
            return_primitive_type = false;
        }

        if (return_primitive_type)
            return m_serialized;

        // 5. If Type(value) is Symbol, then throw a "DataCloneError" DOMException.
        if (value.is_symbol())
            return WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize Symbol"_fly_string);

        // 6. Let serialized be an uninitialized value.
        //    NOTE: We use the range of the soon-to-be-serialized value in our serialized data buffer
        //          to be the `serialized` spec value.
        auto serialized_start = m_serialized.size();

        // 7. If value has a [[BooleanData]] internal slot, then set serialized to { [[Type]]: "Boolean", [[BooleanData]]: value.[[BooleanData]] }.
        if (value.is_object() && is<JS::BooleanObject>(value.as_object())) {
            m_serialized.append(ValueTag::BooleanObject);
            auto& boolean_object = static_cast<JS::BooleanObject&>(value.as_object());
            m_serialized.append(bit_cast<u32>(static_cast<u32>(boolean_object.boolean())));
        }

        // 8. Otherwise, if value has a [[NumberData]] internal slot, then set serialized to { [[Type]]: "Number", [[NumberData]]: value.[[NumberData]] }.
        else if (value.is_object() && is<JS::NumberObject>(value.as_object())) {
            m_serialized.append(ValueTag::NumberObject);
            auto& number_object = static_cast<JS::NumberObject&>(value.as_object());
            double const number = number_object.number();
            m_serialized.append(bit_cast<u32*>(&number), 2);
        }

        // 9. Otherwise, if value has a [[BigIntData]] internal slot, then set serialized to { [[Type]]: "BigInt", [[BigIntData]]: value.[[BigIntData]] }.
        else if (value.is_object() && is<JS::BigIntObject>(value.as_object())) {
            m_serialized.append(ValueTag::BigIntObject);
            auto& bigint_object = static_cast<JS::BigIntObject&>(value.as_object());
            TRY(serialize_string(m_serialized, TRY_OR_THROW_OOM(m_vm, bigint_object.bigint().to_string())));
        }

        // 10. Otherwise, if value has a [[StringData]] internal slot, then set serialized to { [[Type]]: "String", [[StringData]]: value.[[StringData]] }.
        else if (value.is_object() && is<JS::StringObject>(value.as_object())) {
            m_serialized.append(ValueTag::StringObject);
            auto& string_object = static_cast<JS::StringObject&>(value.as_object());
            TRY(serialize_string(m_serialized, string_object.primitive_string()));
        }

        // 11. Otherwise, if value has a [[DateValue]] internal slot, then set serialized to { [[Type]]: "Date", [[DateValue]]: value.[[DateValue]] }.
        else if (value.is_object() && is<JS::Date>(value.as_object())) {
            m_serialized.append(ValueTag::DateObject);
            auto& date_object = static_cast<JS::Date&>(value.as_object());
            double const date_value = date_object.date_value();
            m_serialized.append(bit_cast<u32*>(&date_value), 2);
        }

        // 12. Otherwise, if value has a [[RegExpMatcher]] internal slot, then set serialized to
        //     { [[Type]]: "RegExp", [[RegExpMatcher]]: value.[[RegExpMatcher]], [[OriginalSource]]: value.[[OriginalSource]],
        //       [[OriginalFlags]]: value.[[OriginalFlags]] }.
        else if (value.is_object() && is<JS::RegExpObject>(value.as_object())) {
            m_serialized.append(ValueTag::RegExpObject);
            auto& regexp_object = static_cast<JS::RegExpObject&>(value.as_object());
            // Note: A Regex<ECMA262> object is perfectly happy to be reconstructed with just the source+flags
            //       In the future, we could optimize the work being done on the deserialize step by serializing
            //       more of the internal state (the [[RegExpMatcher]] internal slot)
            TRY(serialize_string(m_serialized, TRY_OR_THROW_OOM(m_vm, String::from_deprecated_string(regexp_object.pattern()))));
            TRY(serialize_string(m_serialized, TRY_OR_THROW_OOM(m_vm, String::from_deprecated_string(regexp_object.flags()))));
        }

        // 13 - 24: FIXME: Serialize other data types
        else {
            return throw_completion(WebIDL::DataCloneError::create(*m_vm.current_realm(), "Unsupported type"_fly_string));
        }

        // 25. Set memory[value] to serialized.
        auto serialized_end = m_serialized.size();
        m_memory.set(make_handle(value), { serialized_start, serialized_end });

        // Second pass: Update the objects to point to other objects in memory

        return m_serialized;
    }

private:
    JS::VM& m_vm;
    SerializationMemory& m_memory; // JS value -> index
    SerializationRecord m_serialized;

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, String const& string)
    {
        u64 const size = string.code_points().byte_length();
        // Append size of the string to the serialized structure.
        TRY_OR_THROW_OOM(m_vm, vector.try_append(bit_cast<u32*>(&size), 2));
        // Append the bytes of the string to the serialized structure.
        u64 byte_position = 0;
        ReadonlyBytes const bytes = { string.code_points().bytes(), string.code_points().byte_length() };
        while (byte_position < size) {
            u32 combined_value = 0;
            for (u8 i = 0; i < 4; ++i) {
                u8 const byte = bytes[byte_position];
                combined_value |= byte << (i * 8);
                byte_position++;
                if (byte_position == size)
                    break;
            }
            TRY_OR_THROW_OOM(m_vm, vector.try_append(combined_value));
        }
        return {};
    }

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, JS::PrimitiveString const& primitive_string)
    {
        auto string = primitive_string.utf8_string();
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
            case BooleanObject: {
                auto* realm = m_vm.current_realm();
                bool const value = static_cast<bool>(m_vector[position++]);
                m_memory.append(JS::BooleanObject::create(*realm, value));
                break;
            }
            case ValueTag::NumberObject: {
                auto* realm = m_vm.current_realm();
                u32 bits[2];
                bits[0] = m_vector[position++];
                bits[1] = m_vector[position++];
                double const value = *bit_cast<double*>(&bits);
                m_memory.append(JS::NumberObject::create(*realm, value));
                break;
            }
            case ValueTag::BigIntObject: {
                auto* realm = m_vm.current_realm();
                auto big_int = TRY(deserialize_big_int_primitive(m_vm, m_vector, position));
                m_memory.append(JS::BigIntObject::create(*realm, big_int));
                break;
            }
            case ValueTag::StringObject: {
                auto* realm = m_vm.current_realm();
                auto string = TRY(deserialize_string_primitive(m_vm, m_vector, position));
                m_memory.append(JS::StringObject::create(*realm, string, realm->intrinsics().string_prototype()));
                break;
            }
            case ValueTag::DateObject: {
                auto* realm = m_vm.current_realm();
                u32 bits[2];
                bits[0] = m_vector[position++];
                bits[1] = m_vector[position++];
                double const value = *bit_cast<double*>(&bits);
                m_memory.append(JS::Date::create(*realm, value));
                break;
            }
            case ValueTag::RegExpObject: {
                auto pattern = TRY(deserialize_string_primitive(m_vm, m_vector, position));
                auto flags = TRY(deserialize_string_primitive(m_vm, m_vector, position));
                m_memory.append(TRY(JS::regexp_create(m_vm, move(pattern), move(flags))));
                break;
            }
            default:
                m_error = "Unsupported type"_fly_string;
                break;
            }
        }
        return {};
    }

    WebIDL::ExceptionOr<JS::Value> result()
    {
        if (!m_error.has_value())
            return m_memory[0];
        return WebIDL::DataCloneError::create(*m_vm.current_realm(), m_error.value());
    }

private:
    JS::VM& m_vm;
    SerializationRecord const& m_vector;
    JS::MarkedVector<JS::Value> m_memory; // Index -> JS value
    Optional<FlyString> m_error;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::PrimitiveString>> deserialize_string_primitive(JS::VM& vm, Vector<u32> const& vector, u32& position)
    {
        u32 size_bits[2];
        size_bits[0] = vector[position++];
        size_bits[1] = vector[position++];
        u64 const size = *bit_cast<u64*>(&size_bits);

        Vector<u8> bytes;
        TRY_OR_THROW_OOM(vm, bytes.try_ensure_capacity(size));
        u64 byte_position = 0;
        while (position < vector.size() && byte_position < size) {
            for (u8 i = 0; i < 4; ++i) {
                bytes.append(vector[position] >> (i * 8) & 0xFF);
                byte_position++;
                if (byte_position == size)
                    break;
            }
            position++;
        }

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
    (void)for_storage;

    // 1. If memory was not supplied, let memory be an empty map.
    if (!memory.has_value())
        memory = SerializationMemory {};

    Serializer serializer(vm, *memory);
    return serializer.serialize(value);
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
