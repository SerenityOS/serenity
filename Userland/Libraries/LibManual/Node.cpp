/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Node.h"
#include "PageNode.h"
#include "SectionNode.h"
#include <AK/Assertions.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <LibFileSystem/FileSystem.h>
#include <LibManual/Path.h>

namespace Manual {

ErrorOr<NonnullRefPtr<PageNode const>> Node::try_create_from_query(Vector<StringView, 2> const& query_parameters)
{
    if (query_parameters.size() > 2)
        return Error::from_string_literal("Queries longer than 2 strings are not supported yet");

    auto query_parameter_iterator = query_parameters.begin();

    if (query_parameter_iterator.is_end())
        return PageNode::help_index_page();

    auto first_query_parameter = *query_parameter_iterator;
    ++query_parameter_iterator;
    if (query_parameter_iterator.is_end()) {
        // [/path/to/docs.md]
        auto path_from_query = LexicalPath { first_query_parameter };
        if (path_from_query.is_absolute()
            && path_from_query.is_child_of(manual_base_path)
            && path_from_query.extension() == "md"sv) {
            auto section_directory = path_from_query.parent();
            auto man_string_location = section_directory.basename().find("man"sv);
            if (!man_string_location.has_value())
                return Error::from_string_literal("Page is inside invalid section");
            auto section_name = section_directory.basename().substring_view(man_string_location.value() + 3);
            auto section = TRY(SectionNode::try_create_from_number(section_name));
            return try_make_ref_counted<PageNode>(section, TRY(String::from_utf8(path_from_query.title())));
        }

        // [page] (in any section)
        Optional<NonnullRefPtr<PageNode>> maybe_page;
        for (auto const& section : sections) {
            auto const page = TRY(try_make_ref_counted<PageNode>(section, TRY(String::from_utf8(first_query_parameter))));
            if (FileSystem::exists(TRY(page->path()))) {
                maybe_page = page;
                break;
            }
        }
        if (maybe_page.has_value())
            return maybe_page.release_value();
        return Error::from_string_literal("Page not found");
    }
    // [section] [name]
    auto second_query_parameter = *query_parameter_iterator;
    auto section = TRY(SectionNode::try_create_from_number(first_query_parameter));
    auto const page = TRY(try_make_ref_counted<PageNode>(section, TRY(String::from_utf8(second_query_parameter))));
    if (FileSystem::exists(TRY(page->path())))
        return page;
    return Error::from_string_literal("Page doesn't exist in section");
}

ErrorOr<NonnullRefPtr<Node const>> Node::try_find_from_help_url(URL const& url)
{
    if (url.host() != "man")
        return Error::from_string_view("Bad help operation"sv);
    if (url.path_segment_count() < 2)
        return Error::from_string_view("Bad help page URL"sv);

    auto const section = url.path_segment_at_index(0);
    auto maybe_section_number = section.to_uint();
    if (!maybe_section_number.has_value())
        return Error::from_string_view("Bad section number"sv);
    auto section_number = maybe_section_number.value();
    if (section_number > number_of_sections)
        return Error::from_string_view("Section number out of bounds"sv);

    NonnullRefPtr<Node const> current_node = sections[section_number - 1];

    for (size_t i = 1; i < url.path_segment_count(); i++) {
        auto children = TRY(current_node->children());
        for (auto const& child : children) {
            if (TRY(child->name()) == url.path_segment_at_index(i).view()) {
                current_node = child;
                break;
            }
        }
    }

    return current_node;
}

}
