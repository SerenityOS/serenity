/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibConfig/Listener.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ColumnsView.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/IconView.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <LibURL/URL.h>
#include <fcntl.h>
#include <sys/stat.h>

namespace FileManager {

void spawn_terminal(GUI::Window* window, StringView directory);

class LauncherHandler : public RefCounted<LauncherHandler> {
public:
    LauncherHandler(NonnullRefPtr<Desktop::Launcher::Details> const& details)
        : m_details(details)
    {
    }

    NonnullRefPtr<GUI::Action> create_launch_action(Function<void(LauncherHandler const&)>);
    Desktop::Launcher::Details const& details() const { return *m_details; }

private:
    NonnullRefPtr<Desktop::Launcher::Details> m_details;
};

class DirectoryView final
    : public GUI::StackWidget
    , private GUI::ModelClient
    , public Config::Listener {
    C_OBJECT(DirectoryView);

public:
    enum class Mode {
        Desktop,
        Normal,
    };

    virtual ~DirectoryView() override;

    bool open(ByteString const& path);
    ByteString path() const { return model().root_path(); }
    void open_parent_directory();
    void open_previous_directory();
    void open_next_directory();
    int path_history_size() const { return m_path_history.size(); }
    int path_history_position() const { return m_path_history_position; }
    static RefPtr<LauncherHandler> get_default_launch_handler(Vector<NonnullRefPtr<LauncherHandler>> const& handlers);
    static Vector<NonnullRefPtr<LauncherHandler>> get_launch_handlers(URL::URL const& url);
    static Vector<NonnullRefPtr<LauncherHandler>> get_launch_handlers(ByteString const& path);

    void refresh();

    void launch(URL::URL const&, LauncherHandler const&) const;

    Function<void(StringView path, bool can_read_in_path, bool can_write_in_path)> on_path_change;
    Function<void(GUI::AbstractView&)> on_selection_change;
    Function<void(GUI::ModelIndex const&, GUI::ContextMenuEvent const&)> on_context_menu_request;
    Function<void(StringView)> on_status_message;
    Function<void(int done, int total)> on_thumbnail_progress;
    Function<void()> on_accepted_drop;

    enum ViewMode {
        Invalid,
        Table,
        Columns,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    void set_view_mode_from_string(ByteString const&);

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
            VERIFY_NOT_REACHED();
        }
    }

    GUI::AbstractView const& current_view() const
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

    GUI::FileSystemModel::Node const& node(GUI::ModelIndex const&) const;

    bool is_desktop() const { return m_mode == Mode::Desktop; }

    Vector<ByteString> selected_file_paths() const;

    GUI::Action& mkdir_action() { return *m_mkdir_action; }
    GUI::Action& touch_action() { return *m_touch_action; }
    GUI::Action& open_terminal_action() { return *m_open_terminal_action; }
    GUI::Action& delete_action() { return *m_delete_action; }
    GUI::Action& force_delete_action() { return *m_force_delete_action; }
    GUI::Action& rename_action() { return *m_rename_action; }
    GUI::Action& view_as_icons_action() { return *m_view_as_icons_action; }
    GUI::Action& view_as_table_action() { return *m_view_as_table_action; }
    GUI::Action& view_as_columns_action() { return *m_view_as_columns_action; }

    // ^Config::Listener
    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;

private:
    explicit DirectoryView(Mode);

    GUI::FileSystemModel const& model() const { return *m_model; }
    GUI::FileSystemModel& model() { return *m_model; }

    void handle_selection_change();
    void handle_drop(GUI::ModelIndex const&, GUI::DropEvent const&);
    void do_delete(bool should_confirm);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    void setup_actions();
    void setup_model();
    void setup_icon_view();
    void setup_columns_view();
    void setup_table_view();

    void handle_activation(GUI::ModelIndex const&);

    void set_status_message(StringView);
    void update_statusbar();
    bool can_modify_current_selection();

    Mode m_mode { Mode::Normal };
    ViewMode m_view_mode { Invalid };

    NonnullRefPtr<GUI::FileSystemModel> m_model;
    NonnullRefPtr<GUI::SortingProxyModel> m_sorting_model;
    size_t m_path_history_position { 0 };
    Vector<ByteString> m_path_history;
    void add_path_to_history(ByteString);

    RefPtr<GUI::Label> m_error_label;

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::IconView> m_icon_view;
    RefPtr<GUI::ColumnsView> m_columns_view;

    RefPtr<GUI::Action> m_mkdir_action;
    RefPtr<GUI::Action> m_touch_action;
    RefPtr<GUI::Action> m_open_terminal_action;
    RefPtr<GUI::Action> m_delete_action;
    RefPtr<GUI::Action> m_force_delete_action;
    RefPtr<GUI::Action> m_rename_action;

    RefPtr<GUI::Action> m_view_as_table_action;
    RefPtr<GUI::Action> m_view_as_icons_action;
    RefPtr<GUI::Action> m_view_as_columns_action;
};

}
