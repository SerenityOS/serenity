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
        , m_name(MUST(String::from_utf8(name)))
    {
    }

    virtual ErrorOr<Span<NonnullRefPtr<Node const>>> children() const override
    {
        TRY(reify_if_needed());
        return m_children.span();
    }

    virtual Node const* parent() const override { return nullptr; }
    virtual ErrorOr<String> name() const override;
    String const& section_name() const { return m_section; }
    virtual unsigned section_number() const override { return m_section.to_number<unsigned>().value_or(0); }
    virtual ErrorOr<String> path() const override;
    virtual PageNode const* document() const override { return nullptr; }

    virtual bool is_open() const override { return m_open; }
    void set_open(bool open);

    static ErrorOr<NonnullRefPtr<SectionNode>> try_create_from_number(StringView section_number);

protected:
    // In this class, the section is a number, but in lower sections it might be the same as the name.
    String m_section;
    String m_name;

private:
    ErrorOr<void> reify_if_needed() const;

    mutable Vector<NonnullRefPtr<Node const>> m_children;
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
