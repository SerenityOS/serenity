/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <LibManual/Node.h>

namespace Manual {

class SectionNode;

class SectionNode : public Node {
public:
    virtual ~SectionNode() override = default;

    SectionNode(String section, String name)
        : m_section(section)
        , m_full_name(String::formatted("{}. {}", section, name))
    {
    }

    virtual NonnullRefPtrVector<Node>& children() const override
    {
        reify_if_needed();
        return m_children;
    }

    virtual Node const* parent() const override { return nullptr; }
    virtual String name() const override { return m_full_name; }
    virtual bool is_open() const override { return m_open; }
    void set_open(bool open);

    String const& section_name() const { return m_section; }
    String path() const;

private:
    void reify_if_needed() const;

    String m_section;
    String m_full_name;
    mutable NonnullRefPtrVector<Node> m_children;
    mutable bool m_reified { false };
    bool m_open { false };
};

constexpr size_t number_of_sections = 8;

Array<NonnullRefPtr<SectionNode>, number_of_sections> const sections = { {
    make_ref_counted<SectionNode>("1", "User Programs"),
    make_ref_counted<SectionNode>("2", "System Calls"),
    make_ref_counted<SectionNode>("3", "Library Functions"),
    make_ref_counted<SectionNode>("4", "Special Files"),
    make_ref_counted<SectionNode>("5", "File Formats"),
    make_ref_counted<SectionNode>("6", "Games"),
    make_ref_counted<SectionNode>("7", "Miscellanea"),
    make_ref_counted<SectionNode>("8", "Sysadmin Tools"),
} };

constexpr Array<StringView, number_of_sections> const section_numbers = { {
    "1"sv,
    "2"sv,
    "3"sv,
    "4"sv,
    "5"sv,
    "6"sv,
    "7"sv,
    "8"sv,
} };

}
