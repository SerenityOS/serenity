/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemeWidget.h"

#include <AK/LexicalPath.h>
#include <Applications/MouseSettings/ThemeWidgetGML.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>
#include <LibGUI/WindowServerConnection.h>

String MouseCursorModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::Bitmap:
        return {};
    case Column::Name:
        return "Name";
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
    Core::DirIterator iterator(String::formatted("/res/cursor-themes/{}", m_theme_name), Core::DirIterator::Flags::SkipDots);

    while (iterator.has_next()) {
        auto path = iterator.next_full_path();
        if (path.ends_with(".ini"))
            continue;
        if (path.contains("2x"))
            continue;

        Cursor cursor;
        cursor.path = move(path);
        cursor.name = LexicalPath::basename(cursor.path);

        // FIXME: Animated cursor bitmaps
        auto cursor_bitmap = Gfx::Bitmap::try_load_from_file(cursor.path).release_value_but_fixme_should_propagate_errors();
        auto cursor_bitmap_rect = cursor_bitmap->rect();
        cursor.params = Gfx::CursorParams::parse_from_filename(cursor.name, cursor_bitmap_rect.center()).constrained(*cursor_bitmap);
        cursor.bitmap = cursor_bitmap->cropped(Gfx::IntRect(Gfx::FloatRect(cursor_bitmap_rect).scaled(1.0 / cursor.params.frames(), 1.0))).release_value_but_fixme_should_propagate_errors();

        m_cursors.append(move(cursor));
    }
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

    Core::DirIterator iterator("/res/cursor-themes", Core::DirIterator::Flags::SkipDots);

    while (iterator.has_next()) {
        auto path = iterator.next_path();
        if (access(String::formatted("/res/cursor-themes/{}/Config.ini", path).characters(), R_OK) == 0)
            m_themes.append(path);
    }
    Model::invalidate();
}

ThemeWidget::ThemeWidget()
{
    load_from_gml(theme_widget_gml);
    m_cursors_tableview = find_descendant_of_type_named<GUI::TableView>("cursors_tableview");
    m_cursors_tableview->set_highlight_selected_rows(true);
    m_cursors_tableview->set_alternating_row_colors(false);
    m_cursors_tableview->set_vertical_padding(16);
    m_cursors_tableview->set_column_headers_visible(false);
    m_cursors_tableview->set_highlight_key_column(false);

    auto mouse_cursor_model = MouseCursorModel::create();
    auto sorting_proxy_model = GUI::SortingProxyModel::create(mouse_cursor_model);
    sorting_proxy_model->set_sort_role(GUI::ModelRole::Display);

    m_cursors_tableview->set_model(sorting_proxy_model);
    m_cursors_tableview->set_key_column_and_sort_order(MouseCursorModel::Column::Name, GUI::SortOrder::Ascending);
    m_cursors_tableview->set_column_width(0, 25);
    m_cursors_tableview->model()->invalidate();

    m_theme_name = GUI::WindowServerConnection::the().get_cursor_theme();
    mouse_cursor_model->change_theme(m_theme_name);

    m_theme_name_box = find_descendant_of_type_named<GUI::ComboBox>("theme_name_box");
    m_theme_name_box->on_change = [this, mouse_cursor_model](String const& value, GUI::ModelIndex const&) mutable {
        m_theme_name = value;
        mouse_cursor_model->change_theme(m_theme_name);
    };
    m_theme_name_box->set_model(ThemeModel::create());
    m_theme_name_box->model()->invalidate();
    m_theme_name_box->set_text(m_theme_name);
}

void ThemeWidget::apply_settings()
{
    GUI::WindowServerConnection::the().async_apply_cursor_theme(m_theme_name_box->text());
}

void ThemeWidget::reset_default_values()
{
    m_theme_name_box->set_text("Default");
}

ThemeWidget::~ThemeWidget()
{
}
