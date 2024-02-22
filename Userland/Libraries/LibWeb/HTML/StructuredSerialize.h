/*
 * Copyright (c) 2022, Daniel Ehrenberg <dan@littledan.dev>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibIPC/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

// Structured serialize is an entirely different format from IPC because:
// - It contains representation of type information
// - It may contain circularities
// - It is restricted to JS values

namespace Web::HTML {

using SerializationRecord = Vector<u32>;
using SerializationMemory = HashMap<JS::Handle<JS::Value>, u32>;
using DeserializationMemory = JS::MarkedVector<JS::Value>;

struct TransferDataHolder {
    Vector<u8> data;
    Vector<IPC::File> fds;
};

struct SerializedTransferRecord {
    SerializationRecord serialized;
    Vector<TransferDataHolder> transfer_data_holders;
};

struct DeserializedTransferRecord {
    JS::Value deserialized;
    Vector<JS::Handle<JS::Object>> transferred_values;
};

enum class TransferType : u8 {
    MessagePort,
};

WebIDL::ExceptionOr<SerializationRecord> structured_serialize(JS::VM& vm, JS::Value);
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_for_storage(JS::VM& vm, JS::Value);
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_internal(JS::VM& vm, JS::Value, bool for_storage, SerializationMemory&);

WebIDL::ExceptionOr<JS::Value> structured_deserialize(JS::VM& vm, SerializationRecord const& serialized, JS::Realm& target_realm, Optional<DeserializationMemory>);

WebIDL::ExceptionOr<ByteBuffer> deserialize_bytes(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position);
WebIDL::ExceptionOr<String> deserialize_string(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position);
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::PrimitiveString>> deserialize_string_primitive(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position);
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::BigInt>> deserialize_big_int_primitive(JS::VM& vm, ReadonlySpan<u32> vector, size_t& position);

WebIDL::ExceptionOr<SerializedTransferRecord> structured_serialize_with_transfer(JS::VM& vm, JS::Value value, Vector<JS::Handle<JS::Object>> const& transfer_list);
WebIDL::ExceptionOr<DeserializedTransferRecord> structured_deserialize_with_transfer(JS::VM& vm, SerializedTransferRecord&);

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, ::Web::HTML::SerializedTransferRecord const&);

template<>
ErrorOr<void> encode(Encoder&, ::Web::HTML::TransferDataHolder const&);

template<>
ErrorOr<::Web::HTML::SerializedTransferRecord> decode(Decoder&);

template<>
ErrorOr<::Web::HTML::TransferDataHolder> decode(Decoder&);

}
