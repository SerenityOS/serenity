/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "M3UParser.h"
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Variant.h>
#include <LibGUI/Widget.h>

enum class PlaylistModelCustomRole {
    _DONOTUSE = (int)GUI::ModelRole::Custom,
    FilePath
};

class PlaylistModel : public GUI::Model {
public:
    enum Column {
        Title,
        Duration,
        Group,
        Album,
        Artist,
        Filesize,
        __Count
    };

    ~PlaylistModel() override = default;

    int row_count(GUI::ModelIndex const&) const override { return m_playlist_items.size(); }
    int column_count(GUI::ModelIndex const&) const override { return 6; }
    GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    ErrorOr<String> column_name(int column) const override;
    Vector<M3UEntry>& items() { return m_playlist_items; }

private:
    Vector<M3UEntry> m_playlist_items;
};

class PlaylistTableView : public GUI::TableView {
    C_OBJECT(PlaylistTableView)
public:
    void doubleclick_event(GUI::MouseEvent& event) override;

    Function<void(Gfx::Point<int> const&)> on_doubleclick;

private:
    PlaylistTableView();
};

class PlaylistWidget : public GUI::Widget {
    C_OBJECT(PlaylistWidget)
public:
    void set_data_model(RefPtr<PlaylistModel> model)
    {
        m_table_view->set_model(model);
        m_table_view->update();
    }

private:
    PlaylistWidget();

    RefPtr<PlaylistTableView> m_table_view;
};
