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
#include <LibFileSystem/FileSystem.h>
#include <LibManual/Path.h>
#include <LibURL/URL.h>

namespace Manual {

ErrorOr<NonnullRefPtr<PageNode const>> Node::try_create_from_query(Vector<StringView, 2> const& query_parameters)
{
    if (query_parameters.size() > 2)
        return Error::from_string_literal("Queries longer than 2 strings are not supported yet");

    if (query_parameters.size() == 1 && query_parameters[0].starts_with("help://"sv)) {
        auto help_url = URL::create_with_url_or_path(query_parameters[0].trim("/"sv, TrimMode::Right));
        auto node_from_url = TRY(Manual::Node::try_find_from_help_url(help_url));
        return *node_from_url->document();
    }

    auto query_parameter_iterator = query_parameters.begin();

    if (query_parameter_iterator.is_end())
        return PageNode::help_index_page();

    auto first_query_parameter = *query_parameter_iterator;
    ++query_parameter_iterator;
    if (query_parameter_iterator.is_end()) {
        // [/path/to/docs.md]
        auto path_from_query = LexicalPath { first_query_parameter };
        constexpr auto MARKDOWN_FILE_EXTENSION = "md"sv;
        if (path_from_query.is_absolute()
            && path_from_query.is_child_of(manual_base_path)
            && path_from_query.extension() == MARKDOWN_FILE_EXTENSION) {
            // Parse the section number and page name from a directory string of the form:
            // /usr/share/man/man[section_number]/[page_name].md
            // The page_name includes any subsections.
            auto const& section_directory = path_from_query.string();
            auto section_name_start_index = manual_base_path.string().length() + 4;
            auto section_name_end_index = section_directory.find('/', section_name_start_index);
            if (!section_name_end_index.has_value())
                return Error::from_string_literal("Page is inside invalid section");
            auto section_name = section_directory.substring_view(section_name_start_index, section_name_end_index.value() - section_name_start_index);
            auto section = TRY(SectionNode::try_create_from_number(section_name));
            auto page_name_end_index = section_directory.length() - section_name_end_index.value() - MARKDOWN_FILE_EXTENSION.length() - 1;
            // +1 to trim the leading '/' from the start.
            auto page_name = section_directory.substring_view(section_name_end_index.value() + 1, page_name_end_index - 1);
            return try_make_ref_counted<PageNode>(section, TRY(String::from_utf8(page_name)));
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

ErrorOr<NonnullRefPtr<Node const>> Node::try_find_from_help_url(URL::URL const& url)
{
    if (url.host() != "man"_string)
        return Error::from_string_view("Bad help operation"sv);
    if (url.path_segment_count() < 2)
        return Error::from_string_view("Bad help page URL"sv);

    auto const section = url.path_segment_at_index(0);
    auto maybe_section_number = section.to_number<unsigned>();
    if (!maybe_section_number.has_value())
        return Error::from_string_view("Bad section number"sv);
    auto section_number = maybe_section_number.value();
    if (section_number > number_of_sections)
        return Error::from_string_view("Section number out of bounds"sv);

    NonnullRefPtr<Node const> current_node = sections[section_number - 1];
    bool child_node_found;
    for (size_t i = 1; i < url.path_segment_count(); i++) {
        child_node_found = false;
        auto children = TRY(current_node->children());
        for (auto const& child : children) {
            if (auto path = url.path_segment_at_index(i); TRY(child->name()) == path.view()) {
                child_node_found = true;
                current_node = child;
                break;
            }
        }

        if (!child_node_found)
            break;
    }

    if (!child_node_found)
        return Error::from_string_view("Page not found"sv);

    return current_node;
}

}
