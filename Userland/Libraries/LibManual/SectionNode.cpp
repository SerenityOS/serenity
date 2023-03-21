/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SectionNode.h"
#include "PageNode.h"
#include "Path.h"
#include "SubsectionNode.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibFileSystem/FileSystem.h>

namespace Manual {

ErrorOr<NonnullRefPtr<SectionNode>> SectionNode::try_create_from_number(StringView section)
{
    auto maybe_section_number = section.to_uint<u32>();
    if (!maybe_section_number.has_value())
        return Error::from_string_literal("Section is not a number");
    auto section_number = maybe_section_number.release_value();
    if (section_number > number_of_sections)
        return Error::from_string_literal("Section number too large");
    return sections[section_number - 1];
}

ErrorOr<String> SectionNode::path() const
{
    return String::formatted("{}/{}{}", manual_base_path, top_level_section_prefix, m_section);
}

ErrorOr<String> SectionNode::name() const
{
    return String::formatted("{}. {}", m_section, m_name);
}

ErrorOr<void> SectionNode::reify_if_needed() const
{
    if (m_reified)
        return {};
    m_reified = true;

    auto own_path = TRY(path());
    Core::DirIterator dir_iter { own_path.to_deprecated_string(), Core::DirIterator::Flags::SkipDots };

    struct Child {
        NonnullRefPtr<Node const> node;
        String name_for_sorting;
    };
    Vector<Child> children;

    while (dir_iter.has_next()) {
        LexicalPath lexical_path(dir_iter.next_path());
        if (lexical_path.extension() != "md") {
            if (FileSystem::is_directory(LexicalPath::absolute_path(own_path.to_deprecated_string(), lexical_path.string()))) {
                dbgln("Found subsection {}", lexical_path);
                children.append({ .node = TRY(try_make_ref_counted<SubsectionNode>(*this, lexical_path.title())),
                    .name_for_sorting = TRY(String::from_utf8(lexical_path.title())) });
            }
        } else {
            children.append({ .node = TRY(try_make_ref_counted<PageNode>(*this, TRY(String::from_utf8(lexical_path.title())))),
                .name_for_sorting = TRY(String::from_utf8(lexical_path.title())) });
        }
    }

    quick_sort(children, [](auto const& a, auto const& b) { return a.name_for_sorting < b.name_for_sorting; });

    m_children.ensure_capacity(children.size());
    for (auto child : children)
        m_children.unchecked_append(move(child.node));

    return {};
}

void SectionNode::set_open(bool open)
{
    if (m_open == open)
        return;
    m_open = open;
}

Array<NonnullRefPtr<SectionNode>, number_of_sections> const sections = { {
    make_ref_counted<SectionNode>("1"sv, "User Programs"sv),
    make_ref_counted<SectionNode>("2"sv, "System Calls"sv),
    make_ref_counted<SectionNode>("3"sv, "Library Functions"sv),
    make_ref_counted<SectionNode>("4"sv, "Special Files"sv),
    make_ref_counted<SectionNode>("5"sv, "File Formats"sv),
    make_ref_counted<SectionNode>("6"sv, "Games"sv),
    make_ref_counted<SectionNode>("7"sv, "Miscellanea"sv),
    make_ref_counted<SectionNode>("8"sv, "Sysadmin Tools"sv),
} };

}
