/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Vector.h>
#include <LibGUI/ColumnsView.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/ItemView.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <sys/stat.h>

class DirectoryView final : public GUI::StackWidget {
    C_OBJECT(DirectoryView)
public:
    virtual ~DirectoryView() override;

    void open(const StringView& path);
    String path() const { return model().root_path(); }
    void open_parent_directory();
    void open_previous_directory();
    void open_next_directory();
    int path_history_size() const { return m_path_history.size(); }
    int path_history_position() const { return m_path_history_position; }

    void refresh();

    Function<void(const StringView&)> on_path_change;
    Function<void(GUI::AbstractView&)> on_selection_change;
    Function<void(const GUI::AbstractView&, const GUI::ModelIndex&, const GUI::ContextMenuEvent&)> on_context_menu_request;
    Function<void(const GUI::AbstractView&, const GUI::ModelIndex&, const GUI::DropEvent&)> on_drop;
    Function<void(const StringView&)> on_status_message;
    Function<void(int done, int total)> on_thumbnail_progress;

    enum ViewMode {
        Invalid,
        List,
        Columns,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    GUI::AbstractView& current_view()
    {
        switch (m_view_mode) {
        case ViewMode::List:
            return *m_table_view;
        case ViewMode::Columns:
            return *m_columns_view;
        case ViewMode::Icon:
            return *m_item_view;
        default:
            ASSERT_NOT_REACHED();
        }
    }

    template<typename Callback>
    void for_each_view_implementation(Callback callback)
    {
        callback(*m_table_view);
        callback(*m_item_view);
        callback(*m_columns_view);
    }

    GUI::FileSystemModel& model() { return *m_model; }

private:
    DirectoryView();
    const GUI::FileSystemModel& model() const { return *m_model; }

    void handle_activation(const GUI::ModelIndex&);

    void set_status_message(const StringView&);
    void update_statusbar();

    ViewMode m_view_mode { Invalid };

    NonnullRefPtr<GUI::FileSystemModel> m_model;
    int m_path_history_position { 0 };
    Vector<String> m_path_history;
    void add_path_to_history(const StringView& path);

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::ItemView> m_item_view;
    RefPtr<GUI::ColumnsView> m_columns_view;
};
