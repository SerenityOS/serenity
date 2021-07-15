/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Bencode.h"
#include <AK/Format.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>

namespace Torrent::Bencode {

void BencodeEncoder::append(i64 value)
{
    m_builder.appendff("i{}e", value);
}
void BencodeEncoder::append(StringView const& value)
{
    m_builder.appendff("{}:{}", value.length(), value);
}
void BencodeEncoder::append(ReadonlyBytes const& value)
{
    m_builder.appendff("{}:", value.size());
    m_builder.append(value);
}
void BencodeEncoder::start_list()
{
    m_builder.append('l');
}
void BencodeEncoder::start_dict()
{
    m_builder.append('d');
}
void BencodeEncoder::end_list()
{
    m_builder.append('e');
}
void BencodeEncoder::end_dict()
{
    m_builder.append('e');
}
NonnullOwnPtr<ByteBuffer> BencodeEncoder::to_buffer()
{
    return make<ByteBuffer>(m_builder.to_byte_buffer());
}
void BencodeEncoder::clear()
{
    m_builder.clear();
}

OwnPtr<BencodeNode> decode_one(ReadonlyBytes const& buffer, size_t& index);
OwnPtr<BencodeNode> decode_dict(ReadonlyBytes const& buffer, size_t& index);
OwnPtr<BencodeNode> decode_list(ReadonlyBytes const& buffer, size_t& index);

OwnPtr<BencodeNode> decode_list(ReadonlyBytes const& buffer, size_t& index)
{
    auto list = adopt_own_if_nonnull(new NonnullOwnPtrVector<BencodeNode>());
    while (buffer[index] != 'e') {

        auto maybe_node = decode_one(buffer, index);
        if (!maybe_node)
            return {};
        list->append(maybe_node.release_nonnull());
    }
    index++;
    return adopt_own_if_nonnull(new BencodeNode { BencodeNodeType::List, list.release_nonnull() });
}

OwnPtr<BencodeNode> decode_dict(ReadonlyBytes const& buffer, size_t& index)
{
    auto dict = make<NonnullOwnPtrVector<BencodeNode>>();
    while (buffer[index] != 'e') {
        auto maybe_key = decode_one(buffer, index);
        if (!maybe_key)
            return {};
        if (maybe_key->type() != BencodeNodeType::String)
            return {};
        auto maybe_value = decode_one(buffer, index);
        if (!maybe_value)
            return {};
        dict->append(make<BencodeNode>(BencodeNodeType::KeyValuePair, make<KeyValuePair>(move(maybe_key->get<String>()), maybe_value.release_nonnull())));
    }
    index++;
    return make<BencodeNode>(BencodeNodeType::Dictionary, move(dict));
}

OwnPtr<BencodeNode> decode_one(ReadonlyBytes const& buffer, size_t& index)
{
    switch (buffer[index++]) {
    case 'i': {
        char* end;
        if (buffer[index] == '0' && buffer[index + 1] != 'e')
            return {};
        i64 value = strtol(reinterpret_cast<char const*>(buffer.slice(index).data()), &end, 10);
        if (*end != 'e')
            return {};

        index = (size_t)end - (size_t)buffer.data() + 1;
        return make<BencodeNode>(BencodeNodeType::Integer, value);
    }
    case 'd':
        return decode_dict(buffer, index);
    case 'l':
        return decode_list(buffer, index);
    case '0':
        if (buffer[index] != ':')
            return {};
        [[fallthrough]];
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9': {
        char* end;
        i64 length = strtol(reinterpret_cast<char const*>(buffer.slice(index - 1).data()), &end, 10);
        if (*end != ':' || length < 0)
            return {};
        index = (size_t)end + 1 + length - (size_t)buffer.data();
        return make<BencodeNode>(BencodeNodeType::String, String(end + 1, length));
    }
    default:
        return {};
    }
}

OwnPtr<BencodeNode> decode(ReadonlyBytes const& buffer)
{
    size_t index = 0;
    switch (buffer[0]) {
    case 'i': {
        char* end;
        i64 value = strtol(reinterpret_cast<char const*>(buffer.offset(1)), &end, 10);
        if (*end != 'e')
            return {};

        return make<BencodeNode>(BencodeNodeType::Integer, value);
    }
    case 'd':
    case 'l':
        return decode_one(buffer, index);
    case '0':
        if (buffer[1] != ':')
            return {};
        [[fallthrough]];
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9': //must be a string
    {
        char* end;
        i64 value = strtol(reinterpret_cast<char const*>(buffer.data()), &end, 10);
        if (*end != ':' || value < 0)
            return {};
        return make<BencodeNode>(BencodeNodeType::String, String(end + 1, value));
    }
    default:
        return {};
    }
}

OwnPtr<BencodeNode> decode(StringView const& view)
{
    return decode(view.bytes());
}

BencodeNode const* BencodeNode::get_node_with_path(StringView const& path) const
{
    VERIFY(!path.is_empty());
    auto node = this;
    auto parts = path.split_view('/');
    if (parts.is_empty())
        return node;

    for (const auto& part : parts) {
        switch (node->type()) {
        case BencodeNodeType::Integer:
        case BencodeNodeType::String:
        case BencodeNodeType::KeyValuePair:
            return nullptr;
        case BencodeNodeType::List: {
            auto maybe_index = part.to_uint();
            if (maybe_index.has_value()) {
                auto& list = node->get<NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>>();
                if (list->size() <= maybe_index.value())
                    return nullptr;
                else
                    node = list->ptr_at(maybe_index.value()).ptr();
            } else
                return nullptr;
        } break;
        case BencodeNodeType::Dictionary: {
            auto maybe_index = part.to_uint();
            auto& list = *node->get<NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>>();
            if (maybe_index.has_value()) {
                if (list.size() <= maybe_index.value())
                    return nullptr;
                else
                    node = list.ptr_at(maybe_index.value()).ptr();
            } else {
                bool found = false;
                for (auto& kvnode : list) {
                    auto& pair = kvnode.get<NonnullOwnPtr<KeyValuePair>>();
                    if (pair->key == part.substring_view(1, part.length() - 2)) {
                        node = pair->value.ptr();
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return nullptr;
            }
        } break;
        }
    }
    return node;
}

BencodeNode* BencodeNode::get_node_with_path(StringView const& path)
{
    return const_cast<BencodeNode*>(const_cast<BencodeNode const*>(this)->get_node_with_path(path));
}
}
