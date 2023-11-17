/*
 * Copyright (c) 2022, Daniel Ehrenberg <dan@littledan.dev>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/Map.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Set.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/TypedArray.h>
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

    GrowableSharedArrayBuffer,

    SharedArrayBuffer,

    ResizeableArrayBuffer,

    ArrayBuffer,

    ArrayBufferView,

    MapObject,

    SetObject,

    ErrorObject,

    ArrayObject,

    Object,

    ObjectReference,

    // TODO: Define many more types

    // This tag or higher are understood to be errors
    ValueTagMax,
};

enum ErrorType {
    Error,
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ClassName,
    JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
};

static ErrorType error_name_to_type(String const& name)
{
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (name == #ClassName##sv)                                                          \
        return ErrorType::ClassName;
    JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
    return Error;
}

// Serializing and deserializing are each two passes:
// 1. Fill up the memory with all the values, but without translating references
// 2. Translate all the references into the appropriate form

class Serializer {
public:
    Serializer(JS::VM& vm, SerializationMemory& memory, bool for_storage)
        : m_vm(vm)
        , m_memory(memory)
        , m_for_storage(for_storage)
    {
    }

    // https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeinternal
    WebIDL::ExceptionOr<SerializationRecord> serialize(JS::Value value)
    {
        // 2. If memory[value] exists, then return memory[value].
        if (m_memory.contains(value)) {
            auto index = m_memory.get(value).value();
            return Vector<u32> { ValueTag::ObjectReference, index };
        }

        // 3. Let deep be false.
        auto deep = false;

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

        // 13. Otherwise, if value has an [[ArrayBufferData]] internal slot, then:
        else if (value.is_object() && is<JS::ArrayBuffer>(value.as_object())) {
            TRY(serialize_array_buffer(m_serialized, static_cast<JS::ArrayBuffer&>(value.as_object())));
        }

        // 14. Otherwise, if value has a [[ViewedArrayBuffer]] internal slot, then:
        else if (value.is_object() && is<JS::TypedArrayBase>(value.as_object())) {
            TRY(serialize_viewed_array_buffer(m_serialized, static_cast<JS::TypedArrayBase&>(value.as_object())));
        } else if (value.is_object() && is<JS::DataView>(value.as_object())) {
            TRY(serialize_viewed_array_buffer(m_serialized, static_cast<JS::DataView&>(value.as_object())));
        }

        // 15. Otherwise, if value has [[MapData]] internal slot, then:
        else if (value.is_object() && is<JS::Map>(value.as_object())) {
            // 1. Set serialized to { [[Type]]: "Map", [[MapData]]: a new empty List }.
            m_serialized.append(ValueTag::MapObject);
            // 2. Set deep to true.
            deep = true;
        }

        // 16. Otherwise, if value has [[SetData]] internal slot, then:
        else if (value.is_object() && is<JS::Set>(value.as_object())) {
            // 1. Set serialized to { [[Type]]: "Set", [[SetData]]: a new empty List }.
            m_serialized.append(ValueTag::SetObject);
            // 2. Set deep to true.
            deep = true;
        }

        // 17. Otherwise, if value has an [[ErrorData]] internal slot and value is not a platform object, then:
        else if (value.is_object() && is<JS::Error>(value.as_object()) && !is<Bindings::PlatformObject>(value.as_object())) {
            // 1. Let name be ? Get(value, "name").
            auto name_property = TRY(value.as_object().get(m_vm.names.name));

            // FIXME: Spec bug - https://github.com/whatwg/html/issues/9923
            // MISSING STEP: Set name to ? ToString(name).
            auto name = TRY(name_property.to_string(m_vm));

            // 2. If name is not one of "Error", "EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError", or "URIError", then set name to "Error".
            auto type = error_name_to_type(name);

            // 3. Let valueMessageDesc be ? value.[[GetOwnProperty]]("message").
            auto value_message_descriptor = TRY(value.as_object().internal_get_own_property(m_vm.names.message));

            // 4. Let message be undefined if IsDataDescriptor(valueMessageDesc) is false, and ? ToString(valueMessageDesc.[[Value]]) otherwise.
            Optional<String> message;
            if (value_message_descriptor.has_value() && value_message_descriptor->is_data_descriptor())
                message = TRY(value_message_descriptor->value->to_string(m_vm));

            // 5. Set serialized to { [[Type]]: "Error", [[Name]]: name, [[Message]]: message }.
            // FIXME: 6. User agents should attach a serialized representation of any interesting accompanying data which are not yet specified, notably the stack property, to serialized.
            m_serialized.append(ValueTag::ErrorObject);
            m_serialized.append(type);
            m_serialized.append(message.has_value());
            if (message.has_value())
                TRY(serialize_string(m_serialized, *message));
        }

        // 18. Otherwise, if value is an Array exotic object, then:
        else if (value.is_object() && is<JS::Array>(value.as_object())) {
            // 1. Let valueLenDescriptor be ? OrdinaryGetOwnProperty(value, "length").
            // 2. Let valueLen be valueLenDescriptor.[[Value]].
            // NON-STANDARD: Array objects in LibJS do not have a real length property, so it must be accessed the usual way
            u64 length = MUST(JS::length_of_array_like(m_vm, value.as_object()));

            // 3. Set serialized to { [[Type]]: "Array", [[Length]]: valueLen, [[Properties]]: a new empty List }.
            m_serialized.append(ValueTag::ArrayObject);
            m_serialized.append(bit_cast<u32*>(&length), 2);

            // 4. Set deep to true.
            deep = true;
        }

        // FIXME: 19. Otherwise, if value is a platform object that is a serializable object:

        // 20. Otherwise, if value is a platform object, then throw a "DataCloneError" DOMException.
        else if (value.is_object() && is<Bindings::PlatformObject>(value.as_object())) {
            return throw_completion(WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize platform objects"_fly_string));
        }

        // 21. Otherwise, if IsCallable(value) is true, then throw a "DataCloneError" DOMException.
        else if (value.is_function()) {
            return throw_completion(WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize functions"_fly_string));
        }

        // FIXME: 22. Otherwise, if value has any internal slot other than [[Prototype]] or [[Extensible]], then throw a "DataCloneError" DOMException.

        // FIXME: 23. Otherwise, if value is an exotic object and value is not the %Object.prototype% intrinsic object associated with any realm, then throw a "DataCloneError" DOMException.

        // 24. Otherwise:
        else {
            // 1. Set serialized to { [[Type]]: "Object", [[Properties]]: a new empty List }.
            m_serialized.append(ValueTag::Object);

            // 2. Set deep to true.
            deep = true;
        }

        // 25. Set memory[value] to serialized.
        m_memory.set(make_handle(value), m_next_id++);

        // 26. If deep is true, then:
        if (deep) {
            // 1. If value has a [[MapData]] internal slot, then:
            if (value.is_object() && is<JS::Map>(value.as_object())) {
                auto const& map = static_cast<JS::Map const&>(value.as_object());
                // 1. Let copiedList be a new empty List.
                Vector<JS::Value> copied_list;
                copied_list.ensure_capacity(map.map_size() * 2);
                // 2. For each Record { [[Key]], [[Value]] } entry of value.[[MapData]]:
                for (auto const& entry : static_cast<JS::Map const&>(value.as_object())) {
                    // 1. Let copiedEntry be a new Record { [[Key]]: entry.[[Key]], [[Value]]: entry.[[Value]] }.
                    // 2. If copiedEntry.[[Key]] is not the special value empty, append copiedEntry to copiedList.
                    copied_list.append(entry.key);
                    copied_list.append(entry.value);
                }
                u64 size = map.map_size();
                m_serialized.append(bit_cast<u32*>(&size), 2);
                // 3. For each Record { [[Key]], [[Value]] } entry of copiedList:
                for (auto copied_value : copied_list) {
                    // 1. Let serializedKey be ? StructuredSerializeInternal(entry.[[Key]], forStorage, memory).
                    // 2. Let serializedValue be ? StructuredSerializeInternal(entry.[[Value]], forStorage, memory).
                    auto serialized_value = TRY(structured_serialize_internal(m_vm, copied_value, m_for_storage, m_memory));

                    // 3. Append { [[Key]]: serializedKey, [[Value]]: serializedValue } to serialized.[[MapData]].
                    m_serialized.extend(serialized_value);
                }
            }

            // 2. Otherwise, if value has a [[SetData]] internal slot, then:
            else if (value.is_object() && is<JS::Set>(value.as_object())) {
                auto const& set = static_cast<JS::Set const&>(value.as_object());
                // 1. Let copiedList be a new empty List.
                Vector<JS::Value> copied_list;
                copied_list.ensure_capacity(set.set_size());
                // 2. For each entry of value.[[SetData]]:
                for (auto const& entry : static_cast<JS::Set const&>(value.as_object())) {
                    // 1. If entry is not the special value empty, append entry to copiedList.
                    copied_list.append(entry.key);
                }
                u64 size = set.set_size();
                m_serialized.append(bit_cast<u32*>(&size), 2);
                // 3. For each entry of copiedList:
                for (auto copied_value : copied_list) {
                    // 1. Let serializedEntry be ? StructuredSerializeInternal(entry, forStorage, memory).
                    auto serialized_value = TRY(structured_serialize_internal(m_vm, copied_value, m_for_storage, m_memory));

                    // 2. Append serializedEntry to serialized.[[SetData]].
                    m_serialized.extend(serialized_value);
                }
            }

            // FIXME: 3. Otherwise, if value is a platform object that is a serializable object, then perform the serialization steps for value's primary interface, given value, serialized, and forStorage.

            // 4. Otherwise, for each key in ! EnumerableOwnProperties(value, key):
            else {
                u64 property_count = 0;
                auto count_offset = m_serialized.size();
                m_serialized.append(bit_cast<u32*>(&property_count), 2);
                for (auto key : MUST(value.as_object().enumerable_own_property_names(JS::Object::PropertyKind::Key))) {
                    auto property_key = MUST(JS::PropertyKey::from_value(m_vm, key));

                    // 1. If ! HasOwnProperty(value, key) is true, then:
                    if (MUST(value.as_object().has_own_property(property_key))) {
                        // 1. Let inputValue be ? value.[[Get]](key, value).
                        auto input_value = TRY(value.as_object().internal_get(property_key, value));

                        // 2. Let outputValue be ? StructuredSerializeInternal(inputValue, forStorage, memory).
                        auto output_value = TRY(structured_serialize_internal(m_vm, input_value, m_for_storage, m_memory));

                        // 3. Append { [[Key]]: key, [[Value]]: outputValue } to serialized.[[Properties]].
                        TRY(serialize_string(m_serialized, key.as_string()));
                        m_serialized.extend(output_value);

                        property_count++;
                    }
                }
                memcpy(m_serialized.data() + count_offset, &property_count, sizeof(property_count));
            }
        }

        // 27. Return serialized.
        return m_serialized;
    }

private:
    JS::VM& m_vm;
    SerializationMemory& m_memory; // JS value -> index
    u32 m_next_id { 0 };
    SerializationRecord m_serialized;
    bool m_for_storage { false };

    WebIDL::ExceptionOr<void> serialize_bytes(Vector<u32>& vector, ReadonlyBytes bytes)
    {
        // Append size of the buffer to the serialized structure.
        u64 const size = bytes.size();
        TRY_OR_THROW_OOM(m_vm, vector.try_append(bit_cast<u32*>(&size), 2));
        // Append the bytes of the buffer to the serialized structure.
        u64 byte_position = 0;
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

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, DeprecatedFlyString const& string)
    {
        return serialize_bytes(vector, string.view().bytes());
    }

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, String const& string)
    {
        return serialize_bytes(vector, { string.code_points().bytes(), string.code_points().byte_length() });
    }

    WebIDL::ExceptionOr<void> serialize_string(Vector<u32>& vector, JS::PrimitiveString const& primitive_string)
    {
        auto string = primitive_string.utf8_string();
        TRY(serialize_string(vector, string));
        return {};
    }

    WebIDL::ExceptionOr<void> serialize_array_buffer(Vector<u32>& vector, JS::ArrayBuffer const& array_buffer)
    {
        // 13. Otherwise, if value has an [[ArrayBufferData]] internal slot, then:

        // FIXME: 1.  If IsSharedArrayBuffer(value) is true, then:
        if (false) {
            // 1. If the current settings object's cross-origin isolated capability is false, then throw a "DataCloneError" DOMException.
            // NOTE: This check is only needed when serializing (and not when deserializing) as the cross-origin isolated capability cannot change
            //       over time and a SharedArrayBuffer cannot leave an agent cluster.
            if (current_settings_object().cross_origin_isolated_capability() == CanUseCrossOriginIsolatedAPIs::No)
                return WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize SharedArrayBuffer when cross-origin isolated"_fly_string);

            // 2. If forStorage is true, then throw a "DataCloneError" DOMException.
            if (m_for_storage)
                return WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize SharedArrayBuffer for storage"_fly_string);

            // FIXME: 3. If value has an [[ArrayBufferMaxByteLength]] internal slot, then set serialized to { [[Type]]: "GrowableSharedArrayBuffer",
            //           [[ArrayBufferData]]: value.[[ArrayBufferData]], [[ArrayBufferByteLengthData]]: value.[[ArrayBufferByteLengthData]],
            //           [[ArrayBufferMaxByteLength]]: value.[[ArrayBufferMaxByteLength]], [[AgentCluster]]: the surrounding agent's agent cluster }.
            // FIXME: 4. Otherwise, set serialized to { [[Type]]: "SharedArrayBuffer", [[ArrayBufferData]]: value.[[ArrayBufferData]],
            //           [[ArrayBufferByteLength]]: value.[[ArrayBufferByteLength]], [[AgentCluster]]: the surrounding agent's agent cluster }.
        }
        // 2. Otherwise:
        else {
            // 1. If IsDetachedBuffer(value) is true, then throw a "DataCloneError" DOMException.
            if (array_buffer.is_detached())
                return WebIDL::DataCloneError::create(*m_vm.current_realm(), "Cannot serialize detached ArrayBuffer"_fly_string);

            // 2. Let size be value.[[ArrayBufferByteLength]].
            auto size = array_buffer.byte_length();

            // 3. Let dataCopy be ? CreateByteDataBlock(size).
            //    NOTE: This can throw a RangeError exception upon allocation failure.
            auto data_copy = TRY(JS::create_byte_data_block(m_vm, size));

            // 4. Perform CopyDataBlockBytes(dataCopy, 0, value.[[ArrayBufferData]], 0, size).
            JS::copy_data_block_bytes(data_copy.buffer(), 0, array_buffer.buffer(), 0, size);

            // FIXME: 5. If value has an [[ArrayBufferMaxByteLength]] internal slot, then set serialized to { [[Type]]: "ResizableArrayBuffer",
            //    [[ArrayBufferData]]: dataCopy, [[ArrayBufferByteLength]]: size, [[ArrayBufferMaxByteLength]]: value.[[ArrayBufferMaxByteLength]] }.
            if (false) {
            }
            // 6. Otherwise, set serialized to { [[Type]]: "ArrayBuffer", [[ArrayBufferData]]: dataCopy, [[ArrayBufferByteLength]]: size }.
            else {
                vector.append(ValueTag::ArrayBuffer);
                TRY(serialize_bytes(vector, data_copy.buffer().bytes()));
            }
        }
        return {};
    }

    template<OneOf<JS::TypedArrayBase, JS::DataView> ViewType>
    WebIDL::ExceptionOr<void> serialize_viewed_array_buffer(Vector<u32>& vector, ViewType const& view)
    {
        // 14. Otherwise, if value has a [[ViewedArrayBuffer]] internal slot, then:

        // FIXME: 1. If IsArrayBufferViewOutOfBounds(value) is true, then throw a "DataCloneError" DOMException.

        // 2. Let buffer be the value of value's [[ViewedArrayBuffer]] internal slot.
        auto* buffer = view.viewed_array_buffer();

        // 3. Let bufferSerialized be ? StructuredSerializeInternal(buffer, forStorage, memory).
        auto buffer_serialized = TRY(structured_serialize_internal(m_vm, JS::Value(buffer), m_for_storage, m_memory));

        // 4. Assert: bufferSerialized.[[Type]] is "ArrayBuffer", "ResizableArrayBuffer", "SharedArrayBuffer", or "GrowableSharedArrayBuffer".
        // NOTE: We currently only implement this for ArrayBuffer
        VERIFY(buffer_serialized[0] == ValueTag::ArrayBuffer);

        // 5. If value has a [[DataView]] internal slot, then set serialized to { [[Type]]: "ArrayBufferView", [[Constructor]]: "DataView",
        //    [[ArrayBufferSerialized]]: bufferSerialized, [[ByteLength]]: value.[[ByteLength]], [[ByteOffset]]: value.[[ByteOffset]] }.
        if constexpr (IsSame<ViewType, JS::DataView>) {
            vector.append(ValueTag::ArrayBufferView);
            vector.extend(move(buffer_serialized));           // [[ArrayBufferSerialized]]
            TRY(serialize_string(vector, "DataView"_string)); // [[Constructor]]
            vector.append(view.byte_length());
            vector.append(view.byte_offset());
        }

        // 6. Otherwise:
        else {
            // 1. Assert: value has a [[TypedArrayName]] internal slot.
            //    NOTE: Handled by constexpr check and template constraints
            // 2. Set serialized to { [[Type]]: "ArrayBufferView", [[Constructor]]: value.[[TypedArrayName]],
            //    [[ArrayBufferSerialized]]: bufferSerialized, [[ByteLength]]: value.[[ByteLength]],
            //    [[ByteOffset]]: value.[[ByteOffset]], [[ArrayLength]]: value.[[ArrayLength]] }.
            vector.append(ValueTag::ArrayBufferView);
            vector.extend(move(buffer_serialized));             // [[ArrayBufferSerialized]]
            TRY(serialize_string(vector, view.element_name())); // [[Constructor]]
            vector.append(view.byte_length());
            vector.append(view.byte_offset());
            vector.append(view.array_length());
        }
        return {};
    }
};

class Deserializer {
public:
    Deserializer(JS::VM& vm, JS::Realm& target_realm, ReadonlySpan<u32> serialized, DeserializationMemory& memory)
        : m_vm(vm)
        , m_serialized(serialized)
        , m_memory(memory)
    {
        VERIFY(vm.current_realm() == &target_realm);
    }

    // https://html.spec.whatwg.org/multipage/structured-data.html#structureddeserialize
    WebIDL::ExceptionOr<JS::Value> deserialize()
    {
        auto tag = m_serialized[m_position++];

        // 2. If memory[serialized] exists, then return memory[serialized].
        if (tag == ValueTag::ObjectReference) {
            auto index = m_serialized[m_position++];
            return m_memory[index];
        }

        // 3. Let deep be false.
        auto deep = false;

        // 4. Let value be an uninitialized value.
        JS::Value value;

        auto is_primitive = false;
        switch (tag) {
        // 5. If serialized.[[Type]] is "primitive", then set value to serialized.[[Value]].
        case ValueTag::UndefinedPrimitive: {
            value = JS::js_undefined();
            is_primitive = true;
            break;
        }
        case ValueTag::NullPrimitive: {
            value = JS::js_null();
            is_primitive = true;
            break;
        }
        case ValueTag::BooleanPrimitive: {
            value = JS::Value(static_cast<bool>(m_serialized[m_position++]));
            is_primitive = true;
            break;
        }
        case ValueTag::NumberPrimitive: {
            u32 bits[2] = {};
            bits[0] = m_serialized[m_position++];
            bits[1] = m_serialized[m_position++];
            auto double_value = *bit_cast<double*>(&bits);
            value = JS::Value(double_value);
            is_primitive = true;
            break;
        }
        case ValueTag::BigIntPrimitive: {
            auto big_int = TRY(deserialize_big_int_primitive(m_vm, m_serialized, m_position));
            value = JS::Value { big_int };
            is_primitive = true;
            break;
        }
        case ValueTag::StringPrimitive: {
            auto string = TRY(deserialize_string_primitive(m_vm, m_serialized, m_position));
            value = JS::Value { string };
            is_primitive = true;
            break;
        }
        // 6. Otherwise, if serialized.[[Type]] is "Boolean", then set value to a new Boolean object in targetRealm whose [[BooleanData]] internal slot value is serialized.[[BooleanData]].
        case BooleanObject: {
            auto* realm = m_vm.current_realm();
            auto bool_value = static_cast<bool>(m_serialized[m_position++]);
            value = JS::BooleanObject::create(*realm, bool_value);
            break;
        }
        // 7. Otherwise, if serialized.[[Type]] is "Number", then set value to a new Number object in targetRealm whose [[NumberData]] internal slot value is serialized.[[NumberData]].
        case ValueTag::NumberObject: {
            auto* realm = m_vm.current_realm();
            u32 bits[2];
            bits[0] = m_serialized[m_position++];
            bits[1] = m_serialized[m_position++];
            auto double_value = *bit_cast<double*>(&bits);
            value = JS::NumberObject::create(*realm, double_value);
            break;
        }
        // 8. Otherwise, if serialized.[[Type]] is "BigInt", then set value to a new BigInt object in targetRealm whose [[BigIntData]] internal slot value is serialized.[[BigIntData]].
        case ValueTag::BigIntObject: {
            auto* realm = m_vm.current_realm();
            auto big_int = TRY(deserialize_big_int_primitive(m_vm, m_serialized, m_position));
            value = JS::BigIntObject::create(*realm, big_int);
            break;
        }
        // 9. Otherwise, if serialized.[[Type]] is "String", then set value to a new String object in targetRealm whose [[StringData]] internal slot value is serialized.[[StringData]].
        case ValueTag::StringObject: {
            auto* realm = m_vm.current_realm();
            auto string = TRY(deserialize_string_primitive(m_vm, m_serialized, m_position));
            value = JS::StringObject::create(*realm, string, realm->intrinsics().string_prototype());
            break;
        }
        // 10. Otherwise, if serialized.[[Type]] is "Date", then set value to a new Date object in targetRealm whose [[DateValue]] internal slot value is serialized.[[DateValue]].
        case ValueTag::DateObject: {
            auto* realm = m_vm.current_realm();
            u32 bits[2];
            bits[0] = m_serialized[m_position++];
            bits[1] = m_serialized[m_position++];
            auto double_value = *bit_cast<double*>(&bits);
            value = JS::Date::create(*realm, double_value);
            break;
        }
        // 11. Otherwise, if serialized.[[Type]] is "RegExp", then set value to a new RegExp object in targetRealm whose [[RegExpMatcher]] internal slot value is serialized.[[RegExpMatcher]],
        //     whose [[OriginalSource]] internal slot value is serialized.[[OriginalSource]], and whose [[OriginalFlags]] internal slot value is serialized.[[OriginalFlags]].
        case ValueTag::RegExpObject: {
            auto pattern = TRY(deserialize_string_primitive(m_vm, m_serialized, m_position));
            auto flags = TRY(deserialize_string_primitive(m_vm, m_serialized, m_position));
            value = TRY(JS::regexp_create(m_vm, move(pattern), move(flags)));
            break;
        }
        // FIXME: 12. Otherwise, if serialized.[[Type]] is "SharedArrayBuffer", then:
        // FIXME: 13. Otherwise, if serialized.[[Type]] is "GrowableSharedArrayBuffer", then:
        // 14. Otherwise, if serialized.[[Type]] is "ArrayBuffer", then set value to a new ArrayBuffer object in targetRealm whose [[ArrayBufferData]] internal slot value is serialized.[[ArrayBufferData]], and whose [[ArrayBufferByteLength]] internal slot value is serialized.[[ArrayBufferByteLength]].
        case ValueTag::ArrayBuffer: {
            auto* realm = m_vm.current_realm();
            // If this throws an exception, catch it, and then throw a "DataCloneError" DOMException.
            auto bytes_or_error = deserialize_bytes(m_vm, m_serialized, m_position);
            if (bytes_or_error.is_error())
                return WebIDL::DataCloneError::create(*m_vm.current_realm(), "out of memory"_fly_string);
            value = JS::ArrayBuffer::create(*realm, bytes_or_error.release_value());
            break;
        }
        // FIXME: 15. Otherwise, if serialized.[[Type]] is "ResizableArrayBuffer", then set value to a new ArrayBuffer object in targetRealm whose [[ArrayBufferData]] internal slot value is serialized.[[ArrayBufferData]], whose [[ArrayBufferByteLength]] internal slot value is serialized.[[ArrayBufferByteLength]], and whose [[ArrayBufferMaxByteLength]] internal slot value is a serialized.[[ArrayBufferMaxByteLength]].
        // 16. Otherwise, if serialized.[[Type]] is "ArrayBufferView", then:
        case ValueTag::ArrayBufferView: {
            auto* realm = m_vm.current_realm();
            auto array_buffer_value = TRY(deserialize());
            auto& array_buffer = verify_cast<JS::ArrayBuffer>(array_buffer_value.as_object());
            auto constructor_name = TRY(deserialize_string(m_vm, m_serialized, m_position));
            u32 byte_length = m_serialized[m_position++];
            u32 byte_offset = m_serialized[m_position++];

            if (constructor_name == "DataView"sv) {
                value = JS::DataView::create(*realm, &array_buffer, byte_length, byte_offset);
            } else {
                u32 array_length = m_serialized[m_position++];
                JS::GCPtr<JS::TypedArrayBase> typed_array_ptr;
#define CREATE_TYPED_ARRAY(ClassName)       \
    if (constructor_name == #ClassName##sv) \
        typed_array_ptr = JS::ClassName::create(*realm, array_length, array_buffer);
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    CREATE_TYPED_ARRAY(ClassName)
                JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE
#undef CREATE_TYPED_ARRAY
                VERIFY(typed_array_ptr != nullptr); // FIXME: Handle errors better here? Can a fuzzer put weird stuff in the buffer?
                typed_array_ptr->set_byte_length(byte_length);
                typed_array_ptr->set_byte_offset(byte_offset);
                value = typed_array_ptr;
            }
            break;
        }
        // 17. Otherwise, if serialized.[[Type]] is "Map", then:
        case ValueTag::MapObject: {
            auto& realm = *m_vm.current_realm();
            // 1. Set value to a new Map object in targetRealm whose [[MapData]] internal slot value is a new empty List.
            value = JS::Map::create(realm);
            // 2. Set deep to true.
            deep = true;
            break;
        }
        // 18. Otherwise, if serialized.[[Type]] is "Set", then:
        case ValueTag::SetObject: {
            auto& realm = *m_vm.current_realm();
            // 1. Set value to a new Set object in targetRealm whose [[SetData]] internal slot value is a new empty List.
            value = JS::Set::create(realm);
            // 2. Set deep to true.
            deep = true;
            break;
        }
        // 19. Otherwise, if serialized.[[Type]] is "Array", then:
        case ValueTag::ArrayObject: {
            auto& realm = *m_vm.current_realm();
            // 1. Let outputProto be targetRealm.[[Intrinsics]].[[%Array.prototype%]].
            // 2. Set value to ! ArrayCreate(serialized.[[Length]], outputProto).
            auto length = read_u64();
            value = MUST(JS::Array::create(realm, length));
            // 3. Set deep to true.
            deep = true;
            break;
        }
        // 20. Otherwise, if serialized.[[Type]] is "Object", then:
        case ValueTag::Object: {
            auto& realm = *m_vm.current_realm();
            // 1. Set value to a new Object in targetRealm.
            value = JS::Object::create(realm, realm.intrinsics().object_prototype());
            // 2. Set deep to true.
            deep = true;
            break;
        }
        // 21. Otherwise, if serialized.[[Type]] is "Error", then:
        case ValueTag::ErrorObject: {
            auto& realm = *m_vm.current_realm();
            auto type = static_cast<ErrorType>(m_serialized[m_position++]);
            auto has_message = static_cast<bool>(m_serialized[m_position++]);
            if (has_message) {
                auto message = TRY(deserialize_string(m_vm, m_serialized, m_position));
                switch (type) {
                case ErrorType::Error:
                    value = JS::Error::create(realm, message);
                    break;
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    case ErrorType::ClassName:                                                           \
        value = JS::ClassName::create(realm, message);                                   \
        break;
                    JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
                }
            } else {
                switch (type) {
                case ErrorType::Error:
                    value = JS::Error::create(realm);
                    break;
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    case ErrorType::ClassName:                                                           \
        value = JS::ClassName::create(realm);                                            \
        break;
                    JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
                }
            }
            break;
        }
        // 22. Otherwise:
        default:
            return WebIDL::DataCloneError::create(*m_vm.current_realm(), "Unsupported type"_fly_string);
        }

        // 23. Set memory[serialized] to value.
        // IMPLEMENTATION DEFINED: We don't add primitive values to the memory to match the serialization indices (which also doesn't add them)
        if (!is_primitive)
            m_memory.append(value);

        // 24. If deep is true, then:
        if (deep) {
            // 1. If serialized.[[Type]] is "Map", then:
            if (tag == ValueTag::MapObject) {
                auto& map = static_cast<JS::Map&>(value.as_object());
                auto length = read_u64();
                // 1. For each Record { [[Key]], [[Value]] } entry of serialized.[[MapData]]:
                for (u64 i = 0u; i < length; ++i) {
                    // 1. Let deserializedKey be ? StructuredDeserialize(entry.[[Key]], targetRealm, memory).
                    auto deserialized_key = TRY(deserialize());

                    // 2. Let deserializedValue be ? StructuredDeserialize(entry.[[Value]], targetRealm, memory).
                    auto deserialized_value = TRY(deserialize());

                    // 3. Append { [[Key]]: deserializedKey, [[Value]]: deserializedValue } to value.[[MapData]].
                    map.map_set(deserialized_key, deserialized_value);
                }
            }

            // 2. Otherwise, if serialized.[[Type]] is "Set", then:
            else if (tag == ValueTag::SetObject) {
                auto& set = static_cast<JS::Set&>(value.as_object());
                auto length = read_u64();
                // 1. For each entry of serialized.[[SetData]]:
                for (u64 i = 0u; i < length; ++i) {
                    // 1. Let deserializedEntry be ? StructuredDeserialize(entry, targetRealm, memory).
                    auto deserialized_entry = TRY(deserialize());

                    // 2. Append deserializedEntry to value.[[SetData]].
                    set.set_add(deserialized_entry);
                }
            }

            // 3. Otherwise, if serialized.[[Type]] is "Array" or "Object", then:
            else if (tag == ValueTag::ArrayObject || tag == ValueTag::Object) {
                auto& object = value.as_object();
                auto length = read_u64();
                // 1. For each Record { [[Key]], [[Value]] } entry of serialized.[[Properties]]:
                for (u64 i = 0u; i < length; ++i) {
                    auto key = TRY(deserialize_string(m_vm, m_serialized, m_position));

                    // 1. Let deserializedValue be ? StructuredDeserialize(entry.[[Value]], targetRealm, memory).
                    auto deserialized_value = TRY(deserialize());

                    // 2. Let result be ! CreateDataProperty(value, entry.[[Key]], deserializedValue).
                    auto result = MUST(object.create_data_property(key.to_deprecated_string(), deserialized_value));

                    // 3. Assert: result is true.
                    VERIFY(result);
                }
            }

            // 4. Otherwise:
            else {
                // FIXME: 1. Perform the appropriate deserialization steps for the interface identified by serialized.[[Type]], given serialized, value, and targetRealm.
                VERIFY_NOT_REACHED();
            }
        }

        // 25. Return value.
        return value;
    }

private:
    JS::VM& m_vm;
    ReadonlySpan<u32> m_serialized;
    size_t m_position { 0 };
    JS::MarkedVector<JS::Value> m_memory; // Index -> JS value

    u64 read_u64()
    {
        u64 value;
        memcpy(&value, m_serialized.offset_pointer(m_position), sizeof(value));
        m_position += 2;
        return value;
    }

    static WebIDL::ExceptionOr<ByteBuffer> deserialize_bytes(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position)
    {
        u32 size_bits[2];
        size_bits[0] = vector[position++];
        size_bits[1] = vector[position++];
        u64 const size = *bit_cast<u64*>(&size_bits);

        auto bytes = TRY_OR_THROW_OOM(vm, ByteBuffer::create_uninitialized(size));
        u64 byte_position = 0;
        while (position < vector.size() && byte_position < size) {
            for (u8 i = 0; i < 4; ++i) {
                bytes[byte_position++] = (vector[position] >> (i * 8) & 0xFF);
                if (byte_position == size)
                    break;
            }
            position++;
        }
        return bytes;
    }

    static WebIDL::ExceptionOr<String> deserialize_string(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position)
    {
        auto bytes = TRY(deserialize_bytes(vm, vector, position));
        return TRY_OR_THROW_OOM(vm, String::from_utf8(StringView { bytes }));
    }

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::PrimitiveString>> deserialize_string_primitive(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position)
    {
        auto bytes = TRY(deserialize_bytes(vm, vector, position));

        return TRY(Bindings::throw_dom_exception_if_needed(vm, [&vm, &bytes]() {
            return JS::PrimitiveString::create(vm, StringView { bytes });
        }));
    }

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::BigInt>> deserialize_big_int_primitive(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position)
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
    SerializationMemory memory = {};
    return structured_serialize_internal(vm, value, false, memory);
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeforstorage
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_for_storage(JS::VM& vm, JS::Value value)
{
    // 1. Return ? StructuredSerializeInternal(value, true).
    SerializationMemory memory = {};
    return structured_serialize_internal(vm, value, true, memory);
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structuredserializeinternal
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_internal(JS::VM& vm, JS::Value value, bool for_storage, SerializationMemory& memory)
{
    // 1. If memory was not supplied, let memory be an empty map.
    // IMPLEMENTATION DEFINED: We move this requirement up to the callers to make recursion easier

    Serializer serializer(vm, memory, for_storage);
    return serializer.serialize(value);
}

// https://html.spec.whatwg.org/multipage/structured-data.html#structureddeserialize
WebIDL::ExceptionOr<JS::Value> structured_deserialize(JS::VM& vm, SerializationRecord const& serialized, JS::Realm& target_realm, Optional<DeserializationMemory> memory)
{
    if (!memory.has_value())
        memory = DeserializationMemory { vm.heap() };

    Deserializer deserializer(vm, target_realm, serialized.span(), *memory);
    return deserializer.deserialize();
}

}
