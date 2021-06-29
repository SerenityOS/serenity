/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Tuple.h>
#include <AK/Variant.h>
#include <AK/Vector.h>

namespace Torrent::Bencode {
class BencodeEncoder {
public:
    void append(i64 value);
    void append(StringView const& value);
    void append(ReadonlyBytes const& value);
    void start_list();
    void start_dict();
    void end_list();
    void end_dict();
    NonnullOwnPtr<ByteBuffer> to_buffer();
    void clear();

private:
    StringBuilder m_builder;
};

enum class BencodeNodeType {
    Integer,
    String,
    List,
    Dictionary,
    KeyValuePair
};

class BencodeNode;

struct KeyValuePair {
    KeyValuePair(String const& key, NonnullOwnPtr<BencodeNode> value)
        : key(key)
        , value(move(value))
    {
    }

    String key;
    NonnullOwnPtr<BencodeNode> value;
};

class BencodeNode {
public:
    BencodeNode() = delete;
    BencodeNode(BencodeNodeType type, Variant<i64, String, NonnullOwnPtr<KeyValuePair>, NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>>&& data)
        : m_type(type)
        , m_data(move(data))
    {
    }

    BencodeNodeType type() const
    {
        return m_type;
    }

    template<typename T>
    T& get()
    {
        VERIFY(m_data.template has<T>());
        return m_data.template get<T>();
    }

    template<typename T>
    T const& get() const
    {
        VERIFY(m_data.template has<T>());
        return m_data.template get<T>();
    }

    template<typename T>
    bool has() const
    {
        return m_data.template has<T>();
    }
    /*
    * Bencode Path:
    * Each part is separated by /
    * Parts can be:
    *  -dictionary keys, surrounded with '
    *  -item index
    *  Returns nullptr on fail
    *
    *  Example: "info"/"files"/2/"length"
    */
    BencodeNode* get_node_with_path(StringView const& path);
    BencodeNode const* get_node_with_path(StringView const& path) const;

private:
    BencodeNodeType m_type;
    Variant<i64, String, NonnullOwnPtr<KeyValuePair>, NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>> m_data;
};

OwnPtr<BencodeNode> decode(ReadonlyBytes const& buffer);
OwnPtr<BencodeNode> decode(StringView const& view);
}
