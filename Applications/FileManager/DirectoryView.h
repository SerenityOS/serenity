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

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ColumnsView.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/IconView.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <sys/stat.h>

namespace FileManager {

class LauncherHandler : public RefCounted<LauncherHandler> {
public:
    LauncherHandler(const NonnullRefPtr<Desktop::Launcher::Details>& details)
        : m_details(details)
    {
    }

    NonnullRefPtr<GUI::Action> create_launch_action(Function<void(const LauncherHandler&)>);
    const Desktop::Launcher::Details& details() const { return *m_details; }

private:
    NonnullRefPtr<Desktop::Launcher::Details> m_details;
};

class DirectoryView final
    : public GUI::StackWidget
    , private GUI::ModelClient {
    C_OBJECT(DirectoryView);

public:
    enum class Mode {
        Desktop,
        Normal,
    };

    virtual ~DirectoryView() override;

    void open(const StringView& path);
    String path() const { return model().root_path(); }
    void open_parent_directory();
    void open_previous_directory();
    void open_next_directory();
    int path_history_size() const { return m_path_history.size(); }
    int path_history_position() const { return m_path_history_position; }
    static RefPtr<LauncherHandler> get_default_launch_handler(const NonnullRefPtrVector<LauncherHandler>& handlers);
    NonnullRefPtrVector<LauncherHandler> get_launch_handlers(const URL& url);
    NonnullRefPtrVector<LauncherHandler> get_launch_handlers(const String& path);

    void refresh();

    void launch(const AK::URL&, const LauncherHandler&);

    Function<void(const StringView& path, bool can_write_in_path)> on_path_change;
    Function<void(GUI::AbstractView&)> on_selection_change;
    Function<void(const GUI::ModelIndex&, const GUI::ContextMenuEvent&)> on_context_menu_request;
    Function<void(const StringView&)> on_status_message;
    Function<void(int done, int total)> on_thumbnail_progress;

    enum ViewMode {
        Invalid,
        Table,
        Columns,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    GUI::AbstractView& current_view()
    {
        switch (m_view_mode) {
        case ViewMode::Table:
            return *m_table_view;
        case ViewMode::Columns:
            return *m_columns_view;
        case ViewMode::Icon:
            return *m_icon_view;
        default:
            ASSERT_NOT_REACHED();
        }
    }

    const GUI::AbstractView& current_view() const
    {
        return const_cast<DirectoryView*>(this)->current_view();
    }

    template<typename Callback>
    void for_each_view_implementation(Callback callback)
    {
        if (m_icon_view)
            callback(*m_icon_view);
        if (m_table_view)
            callback(*m_table_view);
        if (m_columns_view)
            callback(*m_columns_view);
    }

    void set_should_show_dotfiles(bool);

    const GUI::FileSystemModel::Node& node(const GUI::ModelIndex&) const;

    bool is_desktop() const { return m_mode == Mode::Desktop; }

    Vector<String> selected_file_paths() const;

    GUI::Action& mkdir_action() { return *m_mkdir_action; }
    GUI::Action& touch_action() { return *m_touch_action; }
    GUI::Action& open_terminal_action() { return *m_open_terminal_action; }
    GUI::Action& delete_action() { return *m_delete_action; }
    GUI::Action& force_delete_action() { return *m_force_delete_action; }

private:
    explicit DirectoryView(Mode);

    const GUI::FileSystemModel& model() const { return *m_model; }
    GUI::FileSystemModel& model() { return *m_model; }

    void handle_selection_change();
    void handle_drop(const GUI::ModelIndex&, const GUI::DropEvent&);
    void do_delete(bool should_confirm);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    void setup_actions();
    void setup_model();
    void setup_icon_view();
    void setup_columns_view();
    void setup_table_view();

    void handle_activation(const GUI::ModelIndex&);

    void set_status_message(const StringView&);
    void update_statusbar();

    Mode m_mode { Mode::Normal };
    ViewMode m_view_mode { Invalid };

    NonnullRefPtr<GUI::FileSystemModel> m_model;
    NonnullRefPtr<GUI::SortingProxyModel> m_sorting_model;
    size_t m_path_history_position { 0 };
    Vector<String> m_path_history;
    void add_path_to_history(const StringView& path);

    RefPtr<GUI::Label> m_error_label;

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::IconView> m_icon_view;
    RefPtr<GUI::ColumnsView> m_columns_view;

    RefPtr<GUI::Action> m_mkdir_action;
    RefPtr<GUI::Action> m_touch_action;
    RefPtr<GUI::Action> m_open_terminal_action;
    RefPtr<GUI::Action> m_delete_action;
    RefPtr<GUI::Action> m_force_delete_action;
};

}
