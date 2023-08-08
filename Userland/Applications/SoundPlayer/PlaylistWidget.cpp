/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaylistWidget.h"
#include "Player.h"
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

PlaylistWidget::PlaylistWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_table_view = add<PlaylistTableView>();
    m_table_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);
    m_table_view->set_selection_behavior(GUI::AbstractView::SelectionBehavior::SelectRows);
    m_table_view->set_highlight_selected_rows(true);
    m_table_view->on_doubleclick = [&](Gfx::Point<int> const& point) {
        auto player = dynamic_cast<Player*>(window()->main_widget());
        auto index = m_table_view->index_at_event_position(point);
        if (!index.is_valid())
            return;
        player->play_file_path(m_table_view->model()->data(index, static_cast<GUI::ModelRole>(PlaylistModelCustomRole::FilePath)).as_string());
    };
}

GUI::Variant PlaylistModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return "CenterLeft";
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Title:
            return m_playlist_items[index.row()].extended_info->track_display_title.value_or(LexicalPath::title(m_playlist_items[index.row()].path));
        case Column::Duration:
            return human_readable_digital_time(m_playlist_items[index.row()].extended_info->track_length_in_seconds.value_or(0));
        case Column::Group:
            return m_playlist_items[index.row()].extended_info->group_name.value_or("");
        case Column::Album:
            return m_playlist_items[index.row()].extended_info->album_title.value_or("");
        case Column::Artist:
            return m_playlist_items[index.row()].extended_info->album_artist.value_or("");
        case Column::Filesize:
            return human_readable_size(m_playlist_items[index.row()].extended_info->file_size_in_bytes.value_or(0));
        }
    }
    if (role == GUI::ModelRole::Sort)
        return data(index, GUI::ModelRole::Display);
    if (role == static_cast<GUI::ModelRole>(PlaylistModelCustomRole::FilePath)) // path
        return m_playlist_items[index.row()].path;

    return {};
}

ErrorOr<String> PlaylistModel::column_name(int column) const
{
    switch (column) {
    case Column::Title:
        return "Title"_string;
    case Column::Duration:
        return "Duration"_string;
    case Column::Group:
        return "Group"_string;
    case Column::Album:
        return "Album"_string;
    case Column::Artist:
        return "Artist"_string;
    case Column::Filesize:
        return "Filesize"_string;
    }
    VERIFY_NOT_REACHED();
}

PlaylistTableView::PlaylistTableView() = default;

void PlaylistTableView::doubleclick_event(GUI::MouseEvent& event)
{
    AbstractView::doubleclick_event(event);
    if (event.button() == GUI::Primary) {
        if (on_doubleclick)
            on_doubleclick(event.position());
    }
}
