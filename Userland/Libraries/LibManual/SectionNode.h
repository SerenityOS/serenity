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

class SectionNode : public Node {
public:
    virtual ~SectionNode() override = default;

    SectionNode(StringView section, StringView name)
        : m_section(MUST(String::from_utf8(section)))
        , m_full_name(MUST(String::formatted("{}. {}", section, name)))
    {
    }

    virtual NonnullRefPtrVector<Node>& children() const override
    {
        MUST(reify_if_needed());
        return m_children;
    }

    virtual Node const* parent() const override { return nullptr; }
    virtual String name() const override { return m_full_name; }
    virtual bool is_open() const override { return m_open; }
    void set_open(bool open);

    String const& section_name() const { return m_section; }
    ErrorOr<String> path() const;

private:
    ErrorOr<void> reify_if_needed() const;

    String m_section;
    String m_full_name;
    mutable NonnullRefPtrVector<Node> m_children;
    mutable bool m_reified { false };
    bool m_open { false };
};

constexpr size_t number_of_sections = 8;

extern Array<NonnullRefPtr<SectionNode>, number_of_sections> const sections;

constexpr Array<StringView, number_of_sections> const section_numbers = {
    "1"sv,
    "2"sv,
    "3"sv,
    "4"sv,
    "5"sv,
    "6"sv,
    "7"sv,
    "8"sv,
};

}
