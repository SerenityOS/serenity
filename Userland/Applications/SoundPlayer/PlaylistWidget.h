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
    ~PlaylistModel() override = default;

    int row_count(const GUI::ModelIndex&) const override { return m_playlist_items.size(); }
    int column_count(const GUI::ModelIndex&) const override { return 6; }
    GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    void update() override;
    String column_name(int column) const override;
    Vector<M3UEntry>& items() { return m_playlist_items; }

private:
    Vector<M3UEntry> m_playlist_items;

    static String format_filesize(u64 size_in_bytes);
    static String format_duration(u32 duration_in_seconds);
};

class PlaylistTableView : public GUI::TableView {
    C_OBJECT(PlaylistTableView)
public:
    void doubleclick_event(GUI::MouseEvent& event) override;

    Function<void(const Gfx::Point<int>&)> on_doubleclick;
};

class PlaylistWidget : public GUI::Widget {
    C_OBJECT(PlaylistWidget)
public:
    PlaylistWidget();
    void set_data_model(RefPtr<PlaylistModel> model)
    {
        m_table_view->set_model(model);
        m_table_view->update();
    }

protected:
private:
    RefPtr<PlaylistTableView> m_table_view;
};
