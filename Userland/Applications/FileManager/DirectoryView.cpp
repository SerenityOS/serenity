/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DirectoryView.h"
#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/Debounce.h>
#include <LibCore/MimeData.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Process.h>
#include <LibGUI/SortingProxyModel.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

namespace FileManager {

void spawn_terminal(GUI::Window* window, StringView directory)
{
    GUI::Process::spawn_or_show_error(window, "/bin/Terminal"sv, ReadonlySpan<StringView> {}, directory);
}

NonnullRefPtr<GUI::Action> LauncherHandler::create_launch_action(Function<void(LauncherHandler const&)> launch_handler)
{
    auto icon = GUI::FileIconProvider::icon_for_executable(details().executable).bitmap_for_size(16);
    return GUI::Action::create(details().name, move(icon), [this, launch_handler = move(launch_handler)](auto&) {
        launch_handler(*this);
    });
}

RefPtr<LauncherHandler> DirectoryView::get_default_launch_handler(Vector<NonnullRefPtr<LauncherHandler>> const& handlers)
{
    // If this is an application, pick it first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i]->details().launcher_type == Desktop::Launcher::LauncherType::Application)
            return handlers[i];
    }
    // If there's a handler preferred by the user, pick this first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i]->details().launcher_type == Desktop::Launcher::LauncherType::UserPreferred)
            return handlers[i];
    }
    // Otherwise, use the user's default, if available
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i]->details().launcher_type == Desktop::Launcher::LauncherType::UserDefault)
            return handlers[i];
    }
    // If still no match, use the first one we find
    if (!handlers.is_empty()) {
        return handlers[0];
    }

    return {};
}

Vector<NonnullRefPtr<LauncherHandler>> DirectoryView::get_launch_handlers(URL::URL const& url)
{
    Vector<NonnullRefPtr<LauncherHandler>> handlers;
    for (auto& h : Desktop::Launcher::get_handlers_with_details_for_url(url)) {
        handlers.append(adopt_ref(*new LauncherHandler(h)));
    }
    return handlers;
}

Vector<NonnullRefPtr<LauncherHandler>> DirectoryView::get_launch_handlers(ByteString const& path)
{
    return get_launch_handlers(URL::create_with_file_scheme(path));
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
        auto error_message = ByteString::formatted("Could not stat {}: {}", path, strerror(errno));
        GUI::MessageBox::show(window(), error_message, "File Manager"sv, GUI::MessageBox::Type::Error);
        return;
    }

    if (S_ISDIR(st.st_mode)) {
        if (is_desktop()) {
            Desktop::Launcher::open(URL::create_with_file_scheme(path));
            return;
        }
        open(path);
        return;
    }

    auto url = URL::create_with_file_scheme(path);
    auto launcher_handlers = get_launch_handlers(url);
    auto default_launcher = get_default_launch_handler(launcher_handlers);

    if (default_launcher) {
        auto launch_origin_rect = current_view().to_widget_rect(current_view().content_rect(index)).translated(current_view().screen_relative_rect().location());
        setenv("__libgui_launch_origin_rect", ByteString::formatted("{},{},{},{}", launch_origin_rect.x(), launch_origin_rect.y(), launch_origin_rect.width(), launch_origin_rect.height()).characters(), 1);
        launch(url, *default_launcher);
        unsetenv("__libgui_launch_origin_rect");
    } else {
        auto error_message = ByteString::formatted("Could not open {}", path);
        GUI::MessageBox::show(window(), error_message, "File Manager"sv, GUI::MessageBox::Type::Error);
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
        auto error_message = String::formatted("Could not read {}:\n{}", failed_path, error_string).release_value_but_fixme_should_propagate_errors();
        m_error_label->set_text(error_message);
        set_active_widget(m_error_label);

        m_mkdir_action->set_enabled(false);
        m_touch_action->set_enabled(false);

        add_path_to_history(model().root_path());

        if (on_path_change)
            on_path_change(failed_path, false, false);
    };

    m_model->on_rename_error = [this](int, char const* error_string) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("Unable to rename file: {}", error_string));
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

    m_model->on_root_path_removed = [this] {
        // Change model root to the first existing parent directory.
        LexicalPath model_root(model().root_path());

        while (model_root.string() != "/") {
            model_root = model_root.parent();
            if (FileSystem::is_directory(model_root.string()))
                break;
        }

        open(model_root.string());
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
        m_icon_view->set_frame_style(Gfx::FrameStyle::NoFrame);
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

    auto visible_columns = Config::read_string("FileManager"sv, "DirectoryView"sv, "TableColumns"sv, ""sv);
    if (visible_columns.is_empty()) {
        m_table_view->set_column_visible(GUI::FileSystemModel::Column::Inode, false);
        m_table_view->set_column_visible(GUI::FileSystemModel::Column::SymlinkTarget, false);
    } else {
        m_table_view->set_visible_columns(visible_columns);
    }
    m_table_view->on_visible_columns_changed = Core::debounce(100, [this]() {
        auto visible_columns = m_table_view->get_visible_columns().release_value_but_fixme_should_propagate_errors();
        Config::write_string("FileManager"sv, "DirectoryView"sv, "TableColumns"sv, visible_columns);
    });

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

void DirectoryView::set_view_mode_from_string(ByteString const& mode)
{
    if (m_mode == Mode::Desktop)
        return;

    if (mode.contains("Table"sv)) {
        set_view_mode(DirectoryView::ViewMode::Table);
        m_view_as_table_action->set_checked(true);
    } else if (mode.contains("Columns"sv)) {
        set_view_mode(DirectoryView::ViewMode::Columns);
        m_view_as_columns_action->set_checked(true);
    } else {
        set_view_mode(DirectoryView::ViewMode::Icon);
        m_view_as_icons_action->set_checked(true);
    }
}

void DirectoryView::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
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

void DirectoryView::add_path_to_history(ByteString path)
{
    if (m_path_history.size() && m_path_history.at(m_path_history_position) == path)
        return;

    if (m_path_history_position < m_path_history.size())
        m_path_history.resize(m_path_history_position + 1);

    m_path_history.append(move(path));
    m_path_history_position = m_path_history.size() - 1;
}

bool DirectoryView::open(ByteString const& path)
{
    auto error_or_real_path = FileSystem::real_path(path);
    if (error_or_real_path.is_error() || !FileSystem::is_directory(path))
        return false;

    auto real_path = error_or_real_path.release_value();
    if (auto result = Core::System::chdir(real_path); result.is_error()) {
        dbgln("Failed to open '{}': {}", real_path, result.error());
        warnln("Failed to open '{}': {}", real_path, result.error());
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
            builder.append(" → "sv);
            builder.append(node.symlink_target);
        }
    }

    set_status_message(builder.string_view());
}

void DirectoryView::set_should_show_dotfiles(bool show_dotfiles)
{
    m_model->set_should_show_dotfiles(show_dotfiles);
}

void DirectoryView::launch(URL::URL const&, LauncherHandler const& launcher_handler) const
{
    // FIXME: Add posix_spawnattr_t support to Core::Process and use it here.
    pid_t child;

    posix_spawnattr_t spawn_attributes;
    posix_spawnattr_init(&spawn_attributes);

    posix_spawnattr_setpgroup(&spawn_attributes, getsid(0));

    short current_flag;
    posix_spawnattr_getflags(&spawn_attributes, &current_flag);
    posix_spawnattr_setflags(&spawn_attributes, static_cast<short>(current_flag | POSIX_SPAWN_SETPGROUP));

    if (launcher_handler.details().launcher_type == Desktop::Launcher::LauncherType::Application) {
        posix_spawn_file_actions_t spawn_actions;
        posix_spawn_file_actions_init(&spawn_actions);
        posix_spawn_file_actions_addchdir(&spawn_actions, path().characters());

        Vector<char const*, 2> argv;
        argv.append(launcher_handler.details().name.characters());

        for (auto const& argument : launcher_handler.details().arguments)
            argv.append(argument.characters());

        argv.append(nullptr);

        errno = posix_spawn(&child, launcher_handler.details().executable.characters(), &spawn_actions, &spawn_attributes, const_cast<char**>(argv.data()), environ);
        if (errno) {
            perror("posix_spawn");
        } else if (disown(child) < 0) {
            perror("disown");
        }
        posix_spawn_file_actions_destroy(&spawn_actions);
    } else {
        for (auto& path : selected_file_paths()) {
            char const* argv[] = { launcher_handler.details().name.characters(), path.characters(), nullptr };
            if ((errno = posix_spawn(&child, launcher_handler.details().executable.characters(), nullptr, &spawn_attributes, const_cast<char**>(argv), environ)))
                continue;
            if (disown(child) < 0)
                perror("disown");
        }
    }
}

Vector<ByteString> DirectoryView::selected_file_paths() const
{
    Vector<ByteString> paths;
    auto& view = current_view();
    auto& model = *view.model();
    view.selection().for_each_index([&](GUI::ModelIndex const& index) {
        auto parent_index = model.parent_index(index);
        auto name_index = model.index(index.row(), GUI::FileSystemModel::Column::Name, parent_index);
        auto path = name_index.data(GUI::ModelRole::Custom).to_byte_string();
        paths.append(path);
    });
    return paths;
}

void DirectoryView::do_delete(bool should_confirm)
{
    auto paths = selected_file_paths();
    VERIFY(!paths.is_empty());
    delete_paths(paths, should_confirm, window());
    current_view().selection().clear();
}

bool DirectoryView::can_modify_current_selection()
{
    auto selections = current_view().selection().indices();
    // FIXME: remove once Clang formats this properly.
    // clang-format off
    return selections.first_matching([&](auto& index) {
        return node(index).can_delete_or_move();
    }).has_value();
    // clang-format on
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
    m_mkdir_action = GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"sv).release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        String value;
        auto icon = Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-folder.png"sv).release_value_but_fixme_should_propagate_errors();
        if (GUI::InputBox::show(window(), value, "Enter a name:"sv, "New Directory"sv, GUI::InputType::NonemptyText, {}, move(icon)) == GUI::InputBox::ExecResult::OK) {
            auto new_dir_path = LexicalPath::canonicalized_path(ByteString::formatted("{}/{}", path(), value));
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), ByteString::formatted("mkdir(\"{}\") failed: {}", new_dir_path, strerror(saved_errno)), "Error"sv, GUI::MessageBox::Type::Error);
            }
        }
    });

    m_touch_action = GUI::Action::create("New &File...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv).release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        String value;
        auto icon = Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-unknown.png"sv).release_value_but_fixme_should_propagate_errors();
        if (GUI::InputBox::show(window(), value, "Enter a name:"sv, "New File"sv, GUI::InputType::NonemptyText, {}, move(icon)) == GUI::InputBox::ExecResult::OK) {
            auto new_file_path = LexicalPath::canonicalized_path(ByteString::formatted("{}/{}", path(), value));
            struct stat st;
            int rc = stat(new_file_path.characters(), &st);
            if ((rc < 0 && errno != ENOENT)) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), ByteString::formatted("stat(\"{}\") failed: {}", new_file_path, strerror(saved_errno)), "Error"sv, GUI::MessageBox::Type::Error);
                return;
            }
            if (rc == 0) {
                GUI::MessageBox::show(window(), ByteString::formatted("{}: Already exists", new_file_path), "Error"sv, GUI::MessageBox::Type::Error);
                return;
            }
            int fd = creat(new_file_path.characters(), 0666);
            if (fd < 0) {
                auto saved_errno = errno;
                GUI::MessageBox::show(window(), ByteString::formatted("creat(\"{}\") failed: {}", new_file_path, strerror(saved_errno)), "Error"sv, GUI::MessageBox::Type::Error);
                return;
            }
            rc = close(fd);
            VERIFY(rc >= 0);
        }
    });

    m_open_terminal_action = GUI::Action::create("Open &Terminal Here", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        spawn_terminal(window(), path());
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
        "View as &Icons", { Mod_Ctrl, KeyCode::Key_1 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/icon-view.png"sv).release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Icon);
            Config::write_string("FileManager"sv, "DirectoryView"sv, "ViewMode"sv, "Icon"sv);
        },
        window());

    m_view_as_table_action = GUI::Action::create_checkable(
        "View as &Table", { Mod_Ctrl, KeyCode::Key_2 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/table-view.png"sv).release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Table);
            Config::write_string("FileManager"sv, "DirectoryView"sv, "ViewMode"sv, "Table"sv);
        },
        window());

    m_view_as_columns_action = GUI::Action::create_checkable(
        "View as &Columns", { Mod_Ctrl, KeyCode::Key_3 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/columns-view.png"sv).release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
            set_view_mode(DirectoryView::ViewMode::Columns);
            Config::write_string("FileManager"sv, "DirectoryView"sv, "ViewMode"sv, "Columns"sv);
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
    auto const& target_node = node(index);

    bool const has_accepted_drop = ::FileManager::handle_drop(event, target_node.full_path(), window()).release_value_but_fixme_should_propagate_errors();

    if (has_accepted_drop && on_accepted_drop)
        on_accepted_drop();
}

}
