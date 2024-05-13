/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemeWidget.h"

#include <AK/LexicalPath.h>
#include <LibCore/Directory.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

namespace MouseSettings {
ErrorOr<String> MouseCursorModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::Bitmap:
        return String {};
    case Column::Name:
        return "Name"_string;
    }
    VERIFY_NOT_REACHED();
}

GUI::Variant MouseCursorModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& cursor = m_cursors[index.row()];

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Bitmap:
            if (!cursor.bitmap)
                return {};
            return *cursor.bitmap;
        case Column::Name:
            return cursor.name;
        }
        VERIFY_NOT_REACHED();
    }
    return {};
}

void MouseCursorModel::invalidate()
{
    if (m_theme_name.is_empty())
        return;

    m_cursors.clear();
    // FIXME: Propagate errors.
    (void)Core::Directory::for_each_entry(ByteString::formatted("/res/cursor-themes/{}", m_theme_name), Core::DirIterator::Flags::SkipDots, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
        auto path = LexicalPath::join(directory.path().string(), entry.name);
        if (path.has_extension(".ini"sv))
            return IterationDecision::Continue;
        if (path.title().contains("2x"sv))
            return IterationDecision::Continue;

        Cursor cursor;
        cursor.path = path.string();
        cursor.name = path.basename();

        // FIXME: Animated cursor bitmaps
        auto cursor_bitmap = Gfx::Bitmap::load_from_file(cursor.path).release_value_but_fixme_should_propagate_errors();
        auto cursor_bitmap_rect = cursor_bitmap->rect();
        cursor.params = Gfx::CursorParams::parse_from_filename(cursor.name, cursor_bitmap_rect.center()).constrained(*cursor_bitmap);
        cursor.bitmap = cursor_bitmap->cropped(Gfx::IntRect(Gfx::FloatRect(cursor_bitmap_rect).scaled(1.0 / cursor.params.frames(), 1.0))).release_value_but_fixme_should_propagate_errors();

        m_cursors.append(move(cursor));
        return IterationDecision::Continue;
    });

    Model::invalidate();
}

GUI::Variant ThemeModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::Display) {
        return m_themes[index.row()];
    }
    return {};
}

void ThemeModel::invalidate()
{
    m_themes.clear();

    // FIXME: Propagate errors.
    (void)Core::Directory::for_each_entry("/res/cursor-themes"sv, Core::DirIterator::Flags::SkipDots, [&](auto const& entry, auto&) -> ErrorOr<IterationDecision> {
        if (access(ByteString::formatted("/res/cursor-themes/{}/Config.ini", entry.name).characters(), R_OK) == 0)
            m_themes.append(entry.name);
        return IterationDecision::Continue;
    });

    Model::invalidate();
}

Vector<GUI::ModelIndex> ThemeModel::matches(StringView needle, unsigned flags, const GUI::ModelIndex& parent)
{
    Vector<GUI::ModelIndex> found = {};

    for (size_t i = 0; i < m_themes.size(); ++i) {
        auto theme = m_themes[i];
        if (!string_matches(theme, needle, flags))
            continue;
        found.append(index(i, 0, parent));
        if (flags & GUI::Model::MatchesFlag::FirstMatchOnly)
            break;
    }

    return found;
}

ErrorOr<void> ThemeWidget::initialize()
{
    m_cursors_tableview = find_descendant_of_type_named<GUI::TableView>("cursors_tableview");
    m_cursors_tableview->set_highlight_selected_rows(true);
    m_cursors_tableview->set_alternating_row_colors(false);
    m_cursors_tableview->set_vertical_padding(16);
    m_cursors_tableview->set_column_headers_visible(false);
    m_cursors_tableview->set_highlight_key_column(false);

    m_mouse_cursor_model = MouseCursorModel::create();
    auto sorting_proxy_model = TRY(GUI::SortingProxyModel::create(*m_mouse_cursor_model));
    sorting_proxy_model->set_sort_role(GUI::ModelRole::Display);

    m_cursors_tableview->set_model(sorting_proxy_model);
    m_cursors_tableview->set_key_column_and_sort_order(MouseCursorModel::Column::Name, GUI::SortOrder::Ascending);
    m_cursors_tableview->set_column_width(0, 25);
    m_cursors_tableview->model()->invalidate();

    auto theme_name = GUI::ConnectionToWindowServer::the().get_cursor_theme();
    m_mouse_cursor_model->change_theme(theme_name);

    m_theme_name_box = find_descendant_of_type_named<GUI::ComboBox>("theme_name_box");
    m_theme_name_box->set_only_allow_values_from_model(true);
    m_theme_name_box->on_change = [this](ByteString const& value, GUI::ModelIndex const&) {
        m_mouse_cursor_model->change_theme(value);
        set_modified(true);
    };
    m_theme_name_box->set_model(ThemeModel::create());
    m_theme_name_box->model()->invalidate();
    m_theme_name_box->set_text(theme_name, GUI::AllowCallback::No);
    return {};
}

void ThemeWidget::apply_settings()
{
    GUI::ConnectionToWindowServer::the().async_apply_cursor_theme(m_theme_name_box->text());
}

void ThemeWidget::reset_default_values()
{
    m_theme_name_box->set_text("Default");
}
}
