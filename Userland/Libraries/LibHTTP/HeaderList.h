/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibHTTP/Header.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#concept-header-list
class HeaderList {
public:
    // https://fetch.spec.whatwg.org/#header-list-contains
    bool contains(String const& name) const
    {
        for (auto& header : m_list) {
            if (header.name.equals_ignoring_case(name))
                return true;
        }
        return false;
    }

    Optional<Header> first_header_with_name(String const& name) const
    {
        for (auto& header : m_list) {
            if (header.name.equals_ignoring_case(name))
                return header;
        }
        return {};
    }

    // https://fetch.spec.whatwg.org/#concept-header-list-get
    String get(String const& name) const
    {
        if (!contains(name))
            return {};

        StringBuilder builder;
        bool first_name_match = true;

        for (auto& header : m_list) {
            if (!header.name.equals_ignoring_case(name))
                continue;

            if (!first_name_match)
                builder.append(", ");

            first_name_match = false;
            builder.append(header.value);
        }

        return builder.to_string();
    }

    // https://fetch.spec.whatwg.org/#concept-header-list-append
    void append(String const& name, String const& value)
    {
        String name_to_use = name;
        if (auto first_header = first_header_with_name(name); first_header.has_value()) {
            // This is to keep the same casing across all headers with the same name.
            name_to_use = first_header.value().name;
        }

        m_list.append({ name_to_use, value });
    }

    // https://fetch.spec.whatwg.org/#concept-header-list-delete
    void remove(String const& name)
    {
        m_list.remove_all_matching([&name](auto& header) {
            return header.name.equals_ignoring_case(name);
        });
    }

    // https://fetch.spec.whatwg.org/#concept-header-list-set
    void set(String const& name, String const& value)
    {
        if (auto first_header = first_header_with_name(name); first_header.has_value()) {
            auto& header = first_header.value();
            header.name = name;
            header.value = value;

            m_list.remove_all_matching([&](auto& header_in_list) {
                return &header_in_list != &header && header_in_list.name.equals_ignoring_case(name);
            });

            return;
        }

        append(name, value);
    }

    // https://fetch.spec.whatwg.org/#concept-header-list-combine
    void combine(String const& name, String const& value)
    {
        if (auto first_header = first_header_with_name(name); first_header.has_value()) {
            auto& header = first_header.value();
            header.value = String::formatted("{}, {}", name, value);
            return;
        }

        append(name, value);
    }

    size_t size() const { return m_list.size(); }

    using StorageType = Vector<Header>;
    using Iterator = StorageType::Iterator;
    using ConstIterator = StorageType::ConstIterator;

    ConstIterator begin() const { return m_list.begin(); }
    Iterator begin() { return m_list.begin(); }

    ConstIterator end() const { return m_list.begin(); }
    Iterator end() { return m_list.end(); }

private:
    // FIXME: This should ideally be a Multimap
    StorageType m_list;
};

}
