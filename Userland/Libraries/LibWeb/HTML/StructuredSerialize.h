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
    JS::MarkedVector<JS::Value> transferred_values;
};

enum class TransferType : u8 {
    MessagePort,
};

WebIDL::ExceptionOr<SerializationRecord> structured_serialize(JS::VM& vm, JS::Value);
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_for_storage(JS::VM& vm, JS::Value);
WebIDL::ExceptionOr<SerializationRecord> structured_serialize_internal(JS::VM& vm, JS::Value, bool for_storage, SerializationMemory&);

WebIDL::ExceptionOr<JS::Value> structured_deserialize(JS::VM& vm, SerializationRecord const& serialized, JS::Realm& target_realm, Optional<DeserializationMemory>);

// TODO: structured_[de]serialize_with_transfer

}
