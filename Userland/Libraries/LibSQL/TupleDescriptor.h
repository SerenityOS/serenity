/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibSQL/Serializer.h>
#include <LibSQL/Type.h>

namespace SQL {

struct TupleElementDescriptor {
    ByteString schema { "" };
    ByteString table { "" };
    ByteString name { "" };
    SQLType type { SQLType::Text };
    Order order { Order::Ascending };

    bool operator==(TupleElementDescriptor const&) const = default;

    void serialize(Serializer& serializer) const
    {
        serializer.serialize(name);
        serializer.serialize<u8>((u8)type);
        serializer.serialize<u8>((u8)order);
    }
    void deserialize(Serializer& serializer)
    {
        name = serializer.deserialize<ByteString>();
        type = (SQLType)serializer.deserialize<u8>();
        order = (Order)serializer.deserialize<u8>();
    }

    size_t length() const
    {
        return sizeof(u32) + name.length() + 2 * sizeof(u8);
    }

    ByteString to_byte_string() const
    {
        return ByteString::formatted("  name: {} type: {} order: {}", name, SQLType_name(type), Order_name(order));
    }
};

class TupleDescriptor
    : public Vector<TupleElementDescriptor>
    , public RefCounted<TupleDescriptor> {
public:
    TupleDescriptor() = default;
    ~TupleDescriptor() = default;

    [[nodiscard]] int compare_ignoring_names(TupleDescriptor const& other) const
    {
        if (size() != other.size())
            return (int)size() - (int)other.size();
        for (auto ix = 0u; ix < size(); ++ix) {
            auto elem = (*this)[ix];
            auto other_elem = other[ix];
            if ((elem.type != other_elem.type) || (elem.order != other_elem.order)) {
                return 1;
            }
        }
        return 0;
    }

    void serialize(Serializer& serializer) const
    {
        serializer.serialize<u32>(size());
        for (auto& element : *this) {
            serializer.serialize<TupleElementDescriptor>(element);
        }
    }

    void deserialize(Serializer& serializer)
    {
        auto sz = serializer.deserialize<u32>();
        for (auto ix = 0u; ix < sz; ix++) {
            append(serializer.deserialize<TupleElementDescriptor>());
        }
    }

    size_t length() const
    {
        size_t len = sizeof(u32);
        for (auto& element : *this)
            len += element.length();
        return len;
    }

    ByteString to_byte_string() const
    {
        Vector<ByteString> elements;
        for (auto& element : *this)
            elements.append(element.to_byte_string());
        return ByteString::formatted("[\n{}\n]", ByteString::join('\n', elements));
    }

    using Vector<TupleElementDescriptor>::operator==;
};

}
