/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "PlaylistWidget.h"
#include "Player.h"
#include "SoundPlayerWidgetAdvancedView.h"
#include <AK/LexicalPath.h>
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
    m_table_view->on_doubleclick = [&](const Gfx::Point<int>& point) {
        auto player = dynamic_cast<Player*>(window()->main_widget());
        auto index = m_table_view->index_at_event_position(point);
        if (!index.is_valid())
            return;
        player->open_file(m_table_view->model()->data(index, static_cast<GUI::ModelRole>(PlaylistModelCustomRole::FilePath)).as_string());
    };
}

GUI::Variant PlaylistModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return "CenterLeft";
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case 0:
            return m_playlist_items[index.row()].extended_info->track_display_title.value_or(LexicalPath(m_playlist_items[index.row()].path).title());
        case 1:
            return format_duration(m_playlist_items[index.row()].extended_info->track_length_in_seconds.value_or(0));
        case 2:
            return m_playlist_items[index.row()].extended_info->group_name.value_or("");
        case 3:
            return m_playlist_items[index.row()].extended_info->album_title.value_or("");
        case 4:
            return m_playlist_items[index.row()].extended_info->album_artist.value_or("");
        case 5:
            return format_filesize(m_playlist_items[index.row()].extended_info->file_size_in_bytes.value_or(0));
        }
    }
    if (role == GUI::ModelRole::Sort)
        return data(index, GUI::ModelRole::Display);
    if (role == static_cast<GUI::ModelRole>(PlaylistModelCustomRole::FilePath)) //path
        return m_playlist_items[index.row()].path;

    return {};
}

String PlaylistModel::format_filesize(u64 size_in_bytes)
{
    if (size_in_bytes > GiB)
        return String::formatted("{:.2f} GiB", (double)size_in_bytes / GiB);
    else if (size_in_bytes > MiB)
        return String::formatted("{:.2f} MiB", (double)size_in_bytes / MiB);
    else if (size_in_bytes > KiB)
        return String::formatted("{:.2f} KiB", (double)size_in_bytes / KiB);
    else
        return String::formatted("{} B", size_in_bytes);
}

String PlaylistModel::format_duration(u32 duration_in_seconds)
{
    return String::formatted("{:02}:{:02}:{:02}", duration_in_seconds / 3600, duration_in_seconds / 60, duration_in_seconds % 60);
}

String PlaylistModel::column_name(int column) const
{
    switch (column) {
    case 0:
        return "Title";
    case 1:
        return "Duration";
    case 2:
        return "Group";
    case 3:
        return "Album";
    case 4:
        return "Artist";
    case 5:
        return "Filesize";
    }
    VERIFY_NOT_REACHED();
}

void PlaylistModel::update()
{
}

void PlaylistTableView::doubleclick_event(GUI::MouseEvent& event)
{
    AbstractView::doubleclick_event(event);
    if (event.button() == GUI::Left) {
        if (on_doubleclick)
            on_doubleclick(event.position());
    }
}
