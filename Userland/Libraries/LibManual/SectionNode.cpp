/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SectionNode.h"
#include "PageNode.h"
#include "Path.h"
#include "SubsectionNode.h"
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibFileSystem/FileSystem.h>

namespace Manual {

ErrorOr<NonnullRefPtr<SectionNode>> SectionNode::try_create_from_number(StringView section)
{
    auto maybe_section_number = section.to_number<u32>();
    if (!maybe_section_number.has_value())
        return Error::from_string_literal("Section is not a number");
    auto section_number = maybe_section_number.release_value();
    if (section_number < 1 || section_number > number_of_sections)
        return Error::from_string_literal("Section number is not valid");
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
    Core::DirIterator dir_iterator { own_path.to_byte_string(), Core::DirIterator::Flags::SkipDots };
    Vector<ByteString> directories;
    HashTable<ByteString> files;
    while (dir_iterator.has_next()) {
        auto entry = dir_iterator.next();
        if (entry->type == Core::DirectoryEntry::Type::Directory)
            TRY(directories.try_append(entry->name));
        else if (entry->type == Core::DirectoryEntry::Type::File && entry->name.ends_with(".md"sv, CaseSensitivity::CaseInsensitive))
            TRY(files.try_set(entry->name));
    }

    struct Child {
        NonnullRefPtr<Node const> node;
        String name_for_sorting;
    };
    Vector<Child> children;

    for (auto const& directory : directories) {
        LexicalPath lexical_path(directory);
        RefPtr<PageNode> associated_page;
        auto matching_page_name = ByteString::formatted("{}.md", directory);
        if (files.remove(matching_page_name))
            associated_page = TRY(try_make_ref_counted<PageNode>(*this, TRY(String::from_utf8(lexical_path.title()))));

        TRY(children.try_append({ .node = TRY(try_make_ref_counted<SubsectionNode>(*this, lexical_path.title(), associated_page)),
            .name_for_sorting = TRY(String::from_utf8(lexical_path.title())) }));
    }

    for (auto const& file : files) {
        LexicalPath lexical_path(file);
        children.append({ .node = TRY(try_make_ref_counted<PageNode>(*this, TRY(String::from_utf8(lexical_path.title())))),
            .name_for_sorting = TRY(String::from_utf8(lexical_path.title())) });
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
