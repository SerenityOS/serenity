/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DirectoryView.h"
#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/SortingProxyModel.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

namespace FileManager {

void spawn_terminal(String const& directory)
{
    posix_spawn_file_actions_t spawn_actions;
    posix_spawn_file_actions_init(&spawn_actions);
    posix_spawn_file_actions_addchdir(&spawn_actions, directory.characters());

    pid_t pid;
    char const* argv[] = { "Terminal", nullptr };
    if ((errno = posix_spawn(&pid, "/bin/Terminal", &spawn_actions, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
    } else {
        if (disown(pid) < 0)
            perror("disown");
    }
    posix_spawn_file_actions_destroy(&spawn_actions);
}

NonnullRefPtr<GUI::Action> LauncherHandler::create_launch_action(Function<void(LauncherHandler const&)> launch_handler)
{
    auto icon = GUI::FileIconProvider::icon_for_executable(details().executable).bitmap_for_size(16);
    return GUI::Action::create(details().name, move(icon), [this, launch_handler = move(launch_handler)](auto&) {
        launch_handler(*this);
    });
}

RefPtr<LauncherHandler> DirectoryView::get_default_launch_handler(NonnullRefPtrVector<LauncherHandler> const& handlers)
{
    // If this is an application, pick it first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::Application)
            return handlers[i];
    }
    // If there's a handler preferred by the user, pick this first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::UserPreferred)
            return handlers[i];
    }
    // Otherwise, use the user's default, if available
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::UserDefault)
            return handlers[i];
    }
    // If still no match, use the first one we find
    if (!handlers.is_empty()) {
        return handlers[0];
    }

    return {};
}

NonnullRefPtrVector<LauncherHandler> DirectoryView::get_launch_handlers(URL const& url)
{
    NonnullRefPtrVector<LauncherHandler> handlers;
    for (auto& h : Desktop::Launcher::get_handlers_with_details_for_url(url)) {
        handlers.append(adopt_ref(*new LauncherHandler(h)));
    }
    return handlers;
}

NonnullRefPtrVector<LauncherHandler> DirectoryView::get_launch_handlers(String const& path)
{
    return get_launch_handlers(URL::create_with_file_protocol(path));
}

void DirectoryView::handle_activation(GUI::ModelIndex const& index)
{
    if (!index.is_valid())
        return;

    auto& node = this->node(index);
    auto path = node.full_path();

    struct stat st;
    if (stat(path.characters(), &st) < 0) {
        perror("stat");
        return;
    }

    if (S_ISDIR(st.st_mode)) {
        if (is_desktop()) {
            Desktop::Launcher::open(URL::create_with_file_protocol(path));
            return;
        }
        open(path);
        return;
    }

    auto url = URL::create_with_file_protocol(path);
    auto launcher_handlers = get_launch_handlers(url);
    auto default_launcher = get_default_launch_handler(launcher_handlers);

    if (default_launcher) {
        auto launch_origin_rect = current_view().to_widget_rect(current_view().content_rect(index)).translated(current_view().screen_relative_rect().location());
        setenv("__libgui_launch_origin_rect", String::formatted("{},{},{},{}", launch_origin_rect.x(), launch_origin_rect.y(), launch_origin_rect.width(), launch_origin_rect.height()).characters(), 1);
        launch(url, *default_launcher);
        unsetenv("__libgui_launch_origin_rect");
    } else {
        auto error_message = String::formatted("Could not open {}", path);
        GUI::MessageBox::show(window(), error_message, "File Manager", GUI::MessageBox::Type::Error);
    }
}

DirectoryView::DirectoryView(Mode mode)
    : m_mode(mode)
    , m_model(GUI::FileSystemModel::create({}))
    , m_sorting_model(MUST(GUI::SortingProxyModel::create(m_model)))
{
    set_active_widget(nullptr);
    set_grabbable_margins(2);

    setup_actions();

    m_error_label = add<GUI::Label>();
    m_error_label->set_font(m_error_label->font().bold_variant());

    setup_model();

    setup_icon_view();
    if (mode != Mode::Desktop) {
        setup_columns_view();
        setup_table_view();
    }

    set_view_mode(ViewMode::Icon);
}

GUI::FileSystemModel::Node const& DirectoryView::node(GUI::ModelIndex const& index) const
{
    return model().node(m_sorting_model->map_to_source(index));
}

void DirectoryView::setup_model()
{
    m_model->on_directory_change_error = [this](int, char const* error_string) {
        auto failed_path = m_model->root_path();
        auto error_message = String::formatted("Could not read {}:\n{}", failed_path, error_string);
        m_error_label->set_text(error_message);
        set_active_widget(m_error_label);

        m_mkdir_action->set_enabled(false);
        m_touch_action->set_enabled(false);

        add_path_to_history(model().root_path());

        if (on_path_change)
            on_path_change(failed_path, false, false);
    };

    m_model->on_rename_error = [this](int, char const* error_string) {
        GUI::MessageBox::show_error(window(), String::formatted("Unable to rename file: {}", error_string));
    };

    m_model->on_complete = [this] {
        if (m_table_view)
            m_table_view->selection().clear();
        if (m_icon_view)
            m_icon_view->selection().clear();

        add_path_to_history(model().root_path());

        bool can_write_in_path = access(model().root_path().characters(), W_OK) == 0;

        m_mkdir_action->set_enabled(can_write_in_path);
        m_touch_action->set_enabled(can_write_in_path);

        if (on_path_change)
            on_path_change(model().root_path(), true, can_write_in_path);
    };

    m_model->register_client(*this);

    m_model->on_thumbnail_progress = [this](int done, int total) {
        if (on_thumbnail_progress)
            on_thumbnail_progress(done, total);
    };

    if (is_desktop())
        m_model->set_root_path(Core::StandardPaths::desktop_directory());
}

void DirectoryView::setup_icon_view()
{
    m_icon_view = add<GUI::IconView>();
    m_icon_view->set_should_hide_unnecessary_scrollbars(true);
    m_icon_view->set_selection_mode(GUI::AbstractView::SelectionMode::MultiSelection);
    m_icon_view->set_editable(true);
    m_icon_view->set_edit_triggers(GUI::AbstractView::EditTrigger::EditKeyPressed);
    m_icon_view->aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    if (is_desktop()) {
        m_icon_view->set_frame_shape(Gfx::FrameShape::NoFrame);
        m_icon_view->set_frame_thickness(0);
        m_icon_view->set_scrollbars_enabled(false);
        m_icon_view->set_fill_with_background_color(false);
        m_icon_view->set_draw_item_text_with_shadow(true);
        m_icon_view->set_flow_direction(GUI::IconView::FlowDirection::TopToBottom);
        m_icon_view->set_accepts_command_palette(false);
    }

    m_icon_view->set_model(m_sorting_model);
    m_icon_view->set_model_column(GUI::FileSystemModel::Column::Name);
    m_icon_view->on_activation = [&](auto& index) {
        handle_activation(index);
    };
    m_icon_view->on_selection_change = [this] {
        handle_selection_change();
    };
    m_icon_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };
    m_icon_view->on_drop = [this](auto& index, auto& event) {
        handle_drop(index, event);
    };
}

void DirectoryView::setup_columns_view()
{
    m_columns_view = add<GUI::ColumnsView>();
    m_columns_view->set_should_hide_unnecessary_scrollbars(true);
    m_columns_view->set_selection_mode(GUI::AbstractView::SelectionMode::MultiSelection);
    m_columns_view->set_editable(true);
    m_columns_view->set_edit_triggers(GUI::AbstractView::EditTrigger::EditKeyPressed);
    m_columns_view->aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    m_columns_view->set_model(m_sorting_model);
    m_columns_view->set_model_column(GUI::FileSystemModel::Column::Name);

    m_columns_view->on_activation = [&](auto& index) {
        handle_activation(index);
    };

    m_columns_view->on_selection_change = [this] {
        handle_selection_change();
    };

    m_columns_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };

    m_columns_view->on_drop = [this](auto& index, auto& event) {
        handle_drop(index, event);
    };
}

void DirectoryView::setup_table_view()
{
    m_table_view = add<GUI::TableView>();
    m_table_view->set_should_hide_unnecessary_scrollbars(true);
    m_table_view->set_selection_mode(GUI::AbstractView::SelectionMode::MultiSelection);
    m_table_view->set_editable(true);
    m_table_view->set_edit_triggers(GUI::AbstractView::EditTrigger::EditKeyPressed);
    m_table_view->aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    m_table_view->set_model(m_sorting_model);
    m_table_view->set_key_column_and_sort_order(GUI::FileSystemModel::Column::Name, GUI::SortOrder::Ascending);

    m_table_view->set_column_visible(GUI::FileSystemModel::Column::Inode, false);
    m_table_view->set_column_visible(GUI::FileSystemModel::Column::SymlinkTarget, false);

    m_table_view->on_activation = [&](auto& index) {
        handle_activation(index);
    };

    m_table_view->on_selection_change = [this] {
        handle_selection_change();
    };

    m_table_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };

    m_table_view->on_drop = [this](auto& index, auto& event) {
        handle_drop(index, event);
    };
}

DirectoryView::~DirectoryView()
{
    m_model->unregister_client(*this);
}

void DirectoryView::model_did_update(unsigned flags)
{
    if (flags & GUI::Model::UpdateFlag::InvalidateAllIndices) {
        for_each_view_implementation([](auto& view) {
            view.selection().clear();
        });
    }
    update_statusbar();
}

void DirectoryView::set_view_mode_from_string(String const& mode)
{
    if (m_mode == Mode::Desktop)
        return;

    if (mode.contains("Table")) {
        set_view_mode(DirectoryView::ViewMode::Table);
        m_view_as_table_action->set_checked(true);
    } else if (mode.contains("Columns")) {
        set_view_mode(DirectoryView::ViewMode::Columns);
        m_view_as_columns_action->set_checked(true);
    } else {
        set_view_mode(DirectoryView::ViewMode::Icon);
        m_view_as_icons_action->set_checked(true);
    }
}

void DirectoryView::config_string_did_change(String const& domain, String const& group, String const& key, String const& value)
{
    if (domain != "FileManager" || group != "DirectoryView")
        return;

    if (key == "ViewMode") {
        set_view_mode_from_string(value);
        return;
    }
}

void DirectoryView::set_view_mode(ViewMode mode)
{
    if (m_view_mode == mode)
        return;
    m_view_mode = mode;
    update();
    if (mode == ViewMode::Table) {
        set_active_widget(m_table_view);
        return;
    }
    if (mode == ViewMode::Columns) {
        set_active_widget(m_columns_view);
        return;
    }
    if (mode == ViewMode::Icon) {
        set_active_widget(m_icon_view);
        return;
    }
    VERIFY_NOT_REACHED();
}

void DirectoryView::add_path_to_history(String path)
{
    if (m_path_history.size() && m_path_history.at(m_path_history_position) == path)
        return;

    if (m_path_history_position < m_path_history.size())
        m_path_history.resize(m_path_history_position + 1);

    m_path_history.append(move(path));
    m_path_history_position = m_path_history.size() - 1;
}

bool DirectoryView::open(String const& path)
{
    auto real_path = Core::File::real_path_for(path);
    if (real_path.is_null() || !Core::File::is_directory(path))
        return false;

    if (chdir(real_path.characters()) < 0) {
        perror("chdir");
    }
    if (model().root_path() == real_path) {
        refresh();
    } else {
        set_active_widget(&current_view());
        model().set_root_path(real_path);
    }
    return true;
}

void DirectoryView::set_status_message(StringView message)
{
    if (on_status_message)
        on_status_message(message);
}

void DirectoryView::open_parent_directory()
{
    open("..");
}

void DirectoryView::refresh()
{
    model().invalidate();
}

void DirectoryView::open_previous_directory()
{
    if (m_path_history_position > 0)
        open(m_path_history[--m_path_history_position]);
}
void DirectoryView::open_next_directory()
{
    if (m_path_history_position < m_path_history.size() - 1)
        open(m_path_history[++m_path_history_position]);
}

void DirectoryView::update_statusbar()
{
    // If we're triggered during widget construction, just ignore it.
    if (m_view_mode == ViewMode::Invalid)
        return;

    StringBuilder builder;

    if (current_view().selection().is_empty()) {
        int total_item_count = model().row_count();
        size_t total_size = model().node({}).total_size;
        builder.appendff("{} item{} ({})", total_item_count, total_item_count != 1 ? "s" : "", human_readable_size(total_size));
        set_status_message(builder.string_view());
        return;
    }

    int selected_item_count = current_view().selection().size();
    size_t selected_byte_count = 0;

    current_view().selection().for_each_index([&](auto& index) {
        auto const& node = this->node(index);
        selected_byte_count += node.size;
    });

    builder.appendff("{} item{} selected ({})", selected_item_count, selected_item_count != 1 ? "s" : "", human_readable_size(selected_byte_count));

    if (selected_item_count == 1) {
        auto& node = this->node(current_view().selection().first());
        if (!node.symlink_target.is_empty()) {
            builder.append(" -> ");
            builder.append(node.symlink_target);
        }
    }

    set_status_message(builder.string_view());
}

void DirectoryView::set_should_show_dotfiles(bool show_dotfiles)
{
    m_model->set_should_show_dotfiles(show_dotfiles);
}

void DirectoryView::launch(URL const&, LauncherHandler const& launcher_handler) const
{
    pid_t child;
    if (launcher_handler.details().launcher_type == Desktop::Launcher::LauncherType::Application) {
        posix_spawn_file_actions_t spawn_actions;
        posix_spawn_file_actions_init(&spawn_actions);
        posix_spawn_file_actions_addchdir(&spawn_actions, path().characters());

        char const* argv[] = { launcher_handler.details().name.characters(), nullptr };
        posix_spawn(&child, launcher_handler.details().executable.characters(), &spawn_actions, nullptr, const_cast<char**>(argv), environ);
        if (disown(child) < 0)
            perror("disown");

        posix_spawn_file_actions_destroy(&spawn_actions);
    } else {
        for (auto& path : selected_file_paths()) {
            char const* argv[] = { launcher_handler.details().name.characters(), path.characters(), nullptr };
            posix_spawn(&child, launcher_handler.details().executable.characters(), nullptr, nullptr, const_cast<char**>(argv), environ);
            if (disown(child) < 0)
                perror("disown");
        }
    }
}

Vector<String> DirectoryView::selected_file_paths() const
{
    Vector<String> paths;
    auto& view = current_view();
    auto& model = *view.model();
    view.selection().for_each_index([&](GUI::ModelIndex const& index) {
        auto parent_index = model.parent_index(index);
        auto name_index = model.index(index.row(), GUI::FileSystemModel::Column::Name, parent_index);
        auto path = name_index.data(GUI::ModelRole::Custom).to_string();
        paths.append(path);
    });
    return paths;
}

void DirectoryView::do_delete(bool should_confirm)
{
    auto paths = selected_file_paths();
    VERIFY(!paths.is_empty());
    delete_paths(paths, should_confirm, window());
}

bool DirectoryView::can_modify_current_selection()
{
    return !current_view().selection().is_empty() && access(path().characters(), W_OK) == 0;
}

void DirectoryView::handle_selection_change()
{
    update_statusbar();

    bool can_modify = can_modify_current_selection();
    m_delete_action->set_enabled(can_modify);
    m_force_delete_action->set_enabled(can_modify);
    m_rename_action->set_enabled(can_modify);

    if (on_selection_change)
        on_selection_change(current_view());
}

void DirectoryView::setup_actions()
{
    m_mkdir_action = GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/mkdir.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        String value;
        if (GUI::InputBox::show(window(), value, "Enter name:", "New directory") == GUI::InputBox::ExecOK && !value.is_empty()) {
            auto new_dir_path = LexicalPath::canonicalized_path(String::formatted("{}/{}", path(), value));
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), String::formatted("mkdir(\"{}\") failed: {}", new_dir_path, strerror(saved_errno)), "Error", GUI::MessageBox::Type::Error);
            }
        }
    });

    m_touch_action = GUI::Action::create("New &File...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        String value;
        if (GUI::InputBox::show(window(), value, "Enter name:", "New file") == GUI::InputBox::ExecOK && !value.is_empty()) {
            auto new_file_path = LexicalPath::canonicalized_path(String::formatted("{}/{}", path(), value));
            struct stat st;
            int rc = stat(new_file_path.characters(), &st);
            if ((rc < 0 && errno != ENOENT)) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), String::formatted("stat(\"{}\") failed: {}", new_file_path, strerror(saved_errno)), "Error", GUI::MessageBox::Type::Error);
                return;
            }
            if (rc == 0) {
                GUI::MessageBox::show(window(), String::formatted("{}: Already exists", new_file_path), "Error", GUI::MessageBox::Type::Error);
                return;
            }
            int fd = creat(new_file_path.characters(), 0666);
            if (fd < 0) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), String::formatted("creat(\"{}\") failed: {}", new_file_path, strerror(saved_errno)), "Error", GUI::MessageBox::Type::Error);
                return;
            }
            rc = close(fd);
            VERIFY(rc >= 0);
        }
    });

    m_open_terminal_action = GUI::Action::create("Open &Terminal Here", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-terminal.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        spawn_terminal(path());
    });

    m_delete_action = GUI::CommonActions::make_delete_action([this](auto&) { do_delete(true); }, window());
    m_rename_action = GUI::CommonActions::make_rename_action([this](auto&) {
        if (can_modify_current_selection())
            current_view().begin_editing(current_view().cursor_index());
    },
        window());

    m_force_delete_action = GUI::Action::create(
        "Delete Without Confirmation", { Mod_Shift, Key_Delete },
        [this](auto&) { do_delete(false); },
        window());

    m_view_as_icons_action = GUI::Action::create_checkable(
        "View as &Icons", { Mod_Ctrl, KeyCode::Key_1 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/icon-view.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Icon);
            Config::write_string("FileManager", "DirectoryView", "ViewMode", "Icon");
        },
        window());

    m_view_as_table_action = GUI::Action::create_checkable(
        "View as &Table", { Mod_Ctrl, KeyCode::Key_2 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/table-view.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Table);
            Config::write_string("FileManager", "DirectoryView", "ViewMode", "Table");
        },
        window());

    m_view_as_columns_action = GUI::Action::create_checkable(
        "View as &Columns", { Mod_Ctrl, KeyCode::Key_3 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/columns-view.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Columns);
            Config::write_string("FileManager", "DirectoryView", "ViewMode", "Columns");
        },
        window());

    if (m_mode == Mode::Desktop) {
        m_view_as_icons_action->set_enabled(false);
        m_view_as_table_action->set_enabled(false);
        m_view_as_columns_action->set_enabled(false);
    }
}

void DirectoryView::handle_drop(GUI::ModelIndex const& index, GUI::DropEvent const& event)
{
    if (!event.mime_data().has_urls())
        return;
    auto urls = event.mime_data().urls();
    if (urls.is_empty()) {
        dbgln("No files to drop");
        return;
    }

    auto& target_node = node(index);
    if (!target_node.is_directory())
        return;

    bool had_accepted_drop = false;
    Vector<String> paths_to_copy;
    for (auto& url_to_copy : urls) {
        if (!url_to_copy.is_valid() || url_to_copy.path() == target_node.full_path())
            continue;
        auto new_path = String::formatted("{}/{}", target_node.full_path(), LexicalPath::basename(url_to_copy.path()));
        if (url_to_copy.path() == new_path)
            continue;

        paths_to_copy.append(url_to_copy.path());
        had_accepted_drop = true;
    }

    if (!paths_to_copy.is_empty())
        run_file_operation(FileOperation::Copy, paths_to_copy, target_node.full_path(), window());

    if (had_accepted_drop && on_accepted_drop)
        on_accepted_drop();
}

}
