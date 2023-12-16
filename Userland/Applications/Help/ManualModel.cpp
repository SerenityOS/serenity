/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ManualModel.h"
#include <AK/FuzzyMatch.h>
#include <AK/Try.h>
#include <AK/Utf8View.h>
#include <LibManual/Node.h>
#include <LibManual/PageNode.h>
#include <LibManual/Path.h>
#include <LibManual/SectionNode.h>

ManualModel::ManualModel()
{
    m_section_open_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/book-open.png"sv).release_value_but_fixme_should_propagate_errors());
    m_section_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/book.png"sv).release_value_but_fixme_should_propagate_errors());
    m_page_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"sv).release_value_but_fixme_should_propagate_errors());
}

Optional<GUI::ModelIndex> ManualModel::index_from_path(StringView path) const
{
    // The first slice removes the man pages base path plus the `/man` from the main section subdirectory.
    // The second slice removes the trailing `.md`.
    auto path_without_base = path.substring_view(Manual::manual_base_path.string().length() + 4);
    auto url = URL::create_with_help_scheme(path_without_base.substring_view(0, path_without_base.length() - 3), {}, "man");

    auto maybe_page = Manual::Node::try_find_from_help_url(url);
    if (maybe_page.is_error())
        return {};

    auto page = maybe_page.release_value();
    // Main section
    if (page->parent() == nullptr) {
        for (size_t section = 0; section < Manual::number_of_sections; ++section) {
            auto main_section_index = index(static_cast<int>(section), 0);
            if (main_section_index.internal_data() == page.ptr())
                return main_section_index;
        }
        return {};
    }
    auto maybe_siblings = page->parent()->children();
    if (maybe_siblings.is_error())
        return {};
    auto siblings = maybe_siblings.release_value();
    for (size_t row = 0; row < siblings.size(); ++row) {
        if (siblings[row] == page)
            return create_index(static_cast<int>(row), 0, page.ptr());
    }

    return {};
}

Optional<String> ManualModel::page_name(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto const* node = static_cast<Manual::Node const*>(index.internal_data());
    auto const* page = node->document();
    if (!page)
        return {};
    auto path = page->name();
    if (path.is_error())
        return {};
    return path.release_value();
}

Optional<String> ManualModel::page_path(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* node = static_cast<Manual::Node const*>(index.internal_data());
    auto page = node->document();
    if (!page)
        return {};
    auto path = page->path();
    if (path.is_error())
        return {};
    return path.release_value();
}

ErrorOr<StringView> ManualModel::page_view(String const& path) const
{
    if (path.is_empty())
        return StringView {};

    {
        // Check if we've got it cached already.
        auto mapped_file = m_mapped_files.get(path);
        if (mapped_file.has_value())
            return StringView { mapped_file.value()->bytes() };
    }

    auto file = TRY(Core::MappedFile::map(path));

    StringView view { file->bytes() };
    m_mapped_files.set(path, move(file));
    return view;
}

Optional<String> ManualModel::page_and_section(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto const* node = static_cast<Manual::Node const*>(index.internal_data());
    auto const* page = node->document();
    if (!page)
        return {};

    auto const* section = static_cast<Manual::SectionNode const*>(page->parent());
    auto page_name = page->name();
    if (page_name.is_error())
        return {};
    auto name = String::formatted("{}({})", page_name.release_value(), section->section_name());
    if (name.is_error())
        return {};
    return name.release_value();
}

GUI::ModelIndex ManualModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, Manual::sections[row].ptr());
    auto* parent = static_cast<Manual::Node const*>(parent_index.internal_data());
    auto const children = parent->children();
    if (children.is_error())
        return {};
    auto child = children.value()[row];
    return create_index(row, column, child.ptr());
}

GUI::ModelIndex ManualModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<Manual::Node const*>(index.internal_data());
    auto* parent = child->parent();
    if (parent == nullptr)
        return {};

    if (parent->parent() == nullptr) {
        for (size_t row = 0; row < Manual::sections.size(); row++)
            if (Manual::sections[row].ptr() == parent)
                return create_index(row, 0, parent);
        VERIFY_NOT_REACHED();
    }
    auto maybe_children = parent->parent()->children();
    if (maybe_children.is_error())
        return {};
    auto children = maybe_children.release_value();
    for (size_t row = 0; row < children.size(); row++) {
        Manual::Node const* child_at_row = children[row];
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

int ManualModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return static_cast<int>(Manual::sections.size());
    auto* node = static_cast<Manual::Node const*>(index.internal_data());
    auto maybe_children = node->children();
    if (maybe_children.is_error())
        return 0;
    return static_cast<int>(maybe_children.value().size());
}

int ManualModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

GUI::Variant ManualModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* node = static_cast<Manual::Node const*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Search:
        if (!node->is_page())
            return {};
        if (auto path = page_path(index); path.has_value())
            if (auto page = page_view(path.release_value()); !page.is_error())
                // FIXME: We already provide String, but GUI::Variant still needs ByteString.
                return ByteString(page.release_value());
        return {};
    case GUI::ModelRole::Display:
        if (auto name = node->name(); !name.is_error())
            return name.release_value();
        return {};
    case GUI::ModelRole::Icon:
        if (node->is_page())
            return m_page_icon;
        if (node->is_open())
            return m_section_open_icon;
        return m_section_icon;
    default:
        return {};
    }
}

void ManualModel::update_section_node_on_toggle(const GUI::ModelIndex& index, bool const open)
{
    auto* node = static_cast<Manual::Node*>(index.internal_data());
    if (is<Manual::SectionNode>(*node))
        static_cast<Manual::SectionNode*>(node)->set_open(open);
}

GUI::Model::MatchResult ManualModel::data_matches(const GUI::ModelIndex& index, const GUI::Variant& term) const
{
    auto name = page_name(index);
    if (!name.has_value())
        return { TriState::False };

    auto match_result = fuzzy_match(term.as_string(), name.value());
    if (match_result.score > 0)
        return { TriState::True, match_result.score };

    auto path = page_path(index);
    // NOTE: This is slightly inaccurate, as page_path can also fail due to OOM. We consider it acceptable to have a data mismatch in that case.
    if (!path.has_value())
        return { TriState::False };
    auto view_result = page_view(path.release_value());
    if (view_result.is_error() || view_result.value().is_empty())
        return { TriState::False };

    if (view_result.value().contains(term.as_string(), CaseSensitivity::CaseInsensitive))
        return { TriState::True, 0 };

    return { TriState::False };
}
