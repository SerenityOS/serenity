/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Variant.h>

namespace BitTorrent {

struct List;
class Dict;

using BEncodingType = Variant<ByteBuffer, i64, List, Dict>;

struct List : public Vector<BEncodingType> {
    using Vector<BEncodingType>::Vector;
};

class Dict : public OrderedHashMap<DeprecatedString, BEncodingType> {
    using OrderedHashMap<DeprecatedString, BEncodingType>::OrderedHashMap;

public:
    template<typename T>
    T get(DeprecatedString key)
    {
        return OrderedHashMap<DeprecatedString, BEncodingType>::get(key).value().get<T>();
    }

    template<typename T>
    bool has(DeprecatedString key)
    {
        return OrderedHashMap<DeprecatedString, BEncodingType>::get(key).value().has<T>();
    }

    bool contains(DeprecatedString key)
    {
        return OrderedHashMap<DeprecatedString, BEncodingType>::contains(key);
    }

    ErrorOr<DeprecatedString> get_string(DeprecatedString key)
    {
        return DeprecatedString::from_utf8(get<ByteBuffer>(key).bytes());
    }
};

}
