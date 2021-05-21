/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibHTTP/Header.h>
#include <LibCore/MimeType.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#concept-header-list
class HeaderList {
public:
    bool contains(String const& name) const;
    String get(String const& name) const;
    void append(String const& name, String const& value);
    void remove(String const& name);
    void set(String const& name, String const& value);
    void combine(String const& name, String const& value);

    size_t size() const { return m_list.size(); }

    // This is here for convenience (so you don't have to do .list())
    using StorageType = Vector<Header>;
    using Iterator = StorageType::Iterator;
    using ConstIterator = StorageType::ConstIterator;

    ConstIterator begin() const { return m_list.begin(); }
    Iterator begin() { return m_list.begin(); }

    ConstIterator end() const { return m_list.begin(); }
    Iterator end() { return m_list.end(); }

    Vector<String> get_decode_and_split(String const& name) const;

    Optional<Core::MimeType> extract_mime_type() const;

    [[nodiscard]] bool determine_nosniff() const;

    Vector<String> extract_header_list_values(String const& name) const;

    StorageType& list() { return m_list; }
    const StorageType& list() const { return m_list; }

    void clear() { return m_list.clear(); }
    bool is_empty() const { return m_list.is_empty(); }

private:
    StorageType m_list;

    Optional<Header> first_header_with_name(String const& name) const;
};

}
