/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopWidget.h"
#include "DirectoryView.h"
#include "FileUtils.h"
#include "PropertiesWindow.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <Applications/FileManager/FileManagerWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <pthread.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace FileManager;

static int run_in_desktop_mode(RefPtr<Core::ConfigFile>);
static int run_in_windowed_mode(RefPtr<Core::ConfigFile>, String initial_location, String entry_focused_on_init);
static void do_copy(const Vector<String>& selected_file_paths, FileUtils::FileOperation file_operation);
static void do_paste(const String& target_directory, GUI::Window* window);
static void do_create_link(const Vector<String>& selected_file_paths, GUI::Window* window);
static void do_unzip_archive(const Vector<String>& selected_file_paths, GUI::Window* window);
static void show_properties(const String& container_dir_path, const String& path, const Vector<String>& selected, GUI::Window* window);
static bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, const DirectoryView& directory_view, const String& full_path, RefPtr<GUI::Action>& default_action, NonnullRefPtrVector<LauncherHandler>& current_file_launch_handlers);

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd unix cpath rpath wpath fattr proc exec sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("FileManager");

    Core::ArgsParser args_parser;
    bool is_desktop_mode { false }, is_selection_mode { false }, ignore_path_resolution { false };
    String initial_location;
    args_parser.add_option(is_desktop_mode, "Run in desktop mode", "desktop", 'd');
    args_parser.add_option(is_selection_mode, "Show entry in parent folder", "select", 's');
    args_parser.add_option(ignore_path_resolution, "Use raw path, do not resolve real path", "raw", 'r');
    args_parser.add_positional_argument(initial_location, "Path to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd cpath rpath wpath fattr proc exec unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (is_desktop_mode)
        return run_in_desktop_mode(move(config));

    // our initial location is defined as, in order of precedence:
    // 1. the command-line path argument (e.g. FileManager /bin)
    // 2. the user's home directory
    // 3. the root directory

    if (!initial_location.is_empty()) {
        if (!ignore_path_resolution)
            initial_location = Core::File::real_path_for(initial_location);

        if (!Core::File::is_directory(initial_location))
            is_selection_mode = true;
    }

    if (initial_location.is_empty())
        initial_location = Core::StandardPaths::home_directory();

    if (initial_location.is_empty())
        initial_location = "/";

    String focused_entry;
    if (is_selection_mode) {
        LexicalPath path(initial_location);
        initial_location = path.dirname();
        focused_entry = path.basename();
    }

    return run_in_windowed_mode(move(config), initial_location, focused_entry);
}

void do_copy(const Vector<String>& selected_file_paths, FileUtils::FileOperation file_operation)
{
    if (selected_file_paths.is_empty())
        VERIFY_NOT_REACHED();

    StringBuilder copy_text;
    if (file_operation == FileUtils::FileOperation::Cut) {
        copy_text.append("#cut\n"); // This exploits the comment lines in the text/uri-list specification, which might be a bit hackish
    }
    for (auto& path : selected_file_paths) {
        auto url = URL::create_with_file_protocol(path);
        copy_text.appendff("{}\n", url);
    }
    GUI::Clipboard::the().set_data(copy_text.build().bytes(), "text/uri-list");
}

void do_paste(const String& target_directory, GUI::Window* window)
{
    auto data_and_type = GUI::Clipboard::the().data_and_type();
    if (data_and_type.mime_type != "text/uri-list") {
        dbgln("Cannot paste clipboard type {}", data_and_type.mime_type);
        return;
    }
    auto copied_lines = String::copy(data_and_type.data).split('\n');
    if (copied_lines.is_empty()) {
        dbgln("No files to paste");
        return;
    }

    bool should_delete_src = false;
    if (copied_lines[0] == "#cut") { // cut operation encoded as a text/uri-list commen
        should_delete_src = true;
        copied_lines.remove(0);
    }

    for (auto& uri_as_string : copied_lines) {
        if (uri_as_string.is_empty())
            continue;
        URL url = uri_as_string;
        if (!url.is_valid() || url.protocol() != "file") {
            dbgln("Cannot paste URI {}", uri_as_string);
            continue;
        }

        auto new_path = String::formatted("{}/{}", target_directory, url.basename());
        if (auto result = Core::File::copy_file_or_directory(new_path, url.path()); result.is_error()) {
            auto error_message = String::formatted("Could not paste '{}': {}", url.path(), result.error().error_code);
            GUI::MessageBox::show(window, error_message, "File Manager", GUI::MessageBox::Type::Error);
        } else if (should_delete_src) {
            FileUtils::delete_path(url.path(), window);
        }
    }
}

void do_create_link(const Vector<String>& selected_file_paths, GUI::Window* window)
{
    auto path = selected_file_paths.first();
    auto destination = String::formatted("{}/{}", Core::StandardPaths::desktop_directory(), LexicalPath::basename(path));
    if (auto result = Core::File::link_file(destination, path); result.is_error()) {
        GUI::MessageBox::show(window, String::formatted("Could not create desktop shortcut:\n{}", result.error()), "File Manager",
            GUI::MessageBox::Type::Error);
    }
}

void do_unzip_archive(const Vector<String>& selected_file_paths, GUI::Window* window)
{
    String archive_file_path = selected_file_paths.first();
    String output_directory_path = archive_file_path.substring(0, archive_file_path.length() - 4);

    pid_t unzip_pid = fork();
    if (unzip_pid < 0) {
        perror("fork");
        VERIFY_NOT_REACHED();
    }

    if (!unzip_pid) {
        int rc = execlp("/bin/unzip", "/bin/unzip", "-d", output_directory_path.characters(), archive_file_path.characters(), nullptr);
        if (rc < 0) {
            perror("execlp");
            _exit(1);
        }
    } else {
        // FIXME: this could probably be tied in with the new file operation progress tracking
        int status;
        int rc = waitpid(unzip_pid, &status, 0);
        if (rc < 0 || !WIFEXITED(status) || WEXITSTATUS(status) != 0)
            GUI::MessageBox::show(window, "Could not extract archive", "Extract Archive Error", GUI::MessageBox::Type::Error);
    }
}

void show_properties(const String& container_dir_path, const String& path, const Vector<String>& selected, GUI::Window* window)
{
    RefPtr<PropertiesWindow> properties;
    if (selected.is_empty()) {
        properties = window->add<PropertiesWindow>(path, true);
    } else {
        properties = window->add<PropertiesWindow>(selected.first(), access(container_dir_path.characters(), W_OK) != 0);
    }
    properties->on_close = [properties = properties.ptr()] {
        properties->remove_from_parent();
    };
    properties->center_on_screen();
    properties->show();
}

bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, const DirectoryView& directory_view, const String& full_path, RefPtr<GUI::Action>& default_action, NonnullRefPtrVector<LauncherHandler>& current_file_launch_handlers)
{
    current_file_launch_handlers = directory_view.get_launch_handlers(full_path);

    bool added_open_menu_items = false;
    auto default_file_handler = directory_view.get_default_launch_handler(current_file_launch_handlers);
    if (default_file_handler) {
        auto file_open_action = default_file_handler->create_launch_action([&, full_path = move(full_path)](auto& launcher_handler) {
            directory_view.launch(URL::create_with_file_protocol(full_path), launcher_handler);
        });
        if (default_file_handler->details().launcher_type == Desktop::Launcher::LauncherType::Application)
            file_open_action->set_text(String::formatted("Run {}", file_open_action->text()));
        else
            file_open_action->set_text(String::formatted("Open in {}", file_open_action->text()));

        default_action = file_open_action;

        menu->add_action(move(file_open_action));
        added_open_menu_items = true;
    } else {
        default_action.clear();
    }

    if (current_file_launch_handlers.size() > 1) {
        added_open_menu_items = true;
        auto& file_open_with_menu = menu->add_submenu("Open with");
        for (auto& handler : current_file_launch_handlers) {
            if (&handler == default_file_handler.ptr())
                continue;
            file_open_with_menu.add_action(handler.create_launch_action([&, full_path = move(full_path)](auto& launcher_handler) {
                directory_view.launch(URL::create_with_file_protocol(full_path), launcher_handler);
            }));
        }
    }

    return added_open_menu_items;
}

int run_in_desktop_mode([[maybe_unused]] RefPtr<Core::ConfigFile> config)
{
    static constexpr const char* process_name = "FileManager (Desktop)";
    set_process_name(process_name, strlen(process_name));
    pthread_setname_np(pthread_self(), process_name);

    auto window = GUI::Window::construct();
    window->set_title("Desktop Manager");
    window->set_window_type(GUI::WindowType::Desktop);
    window->set_has_alpha_channel(true);

    auto& desktop_widget = window->set_main_widget<FileManager::DesktopWidget>();
    desktop_widget.set_layout<GUI::VerticalBoxLayout>();

    [[maybe_unused]] auto& directory_view = desktop_widget.add<DirectoryView>(DirectoryView::Mode::Desktop);

    auto copy_action = GUI::CommonActions::make_copy_action(
        [&](auto&) {
            auto paths = directory_view.selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileUtils::FileOperation::Copy);
        },
        window);
    copy_action->set_enabled(false);

    auto cut_action = GUI::CommonActions::make_cut_action(
        [&](auto&) {
            auto paths = directory_view.selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileUtils::FileOperation::Cut);
        },
        window);
    cut_action->set_enabled(false);

    auto unzip_archive_action
        = GUI::Action::create(
            "E&xtract Here",
            [&](const GUI::Action&) {
                auto paths = directory_view.selected_file_paths();
                if (paths.is_empty())
                    return;

                do_unzip_archive(paths, directory_view.window());
            },
            window);

    directory_view.on_selection_change = [&](const GUI::AbstractView& view) {
        copy_action->set_enabled(!view.selection().is_empty());
        cut_action->set_enabled(!view.selection().is_empty());
    };

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto&) {
            String path = directory_view.path();
            Vector<String> selected = directory_view.selected_file_paths();

            show_properties(path, path, selected, directory_view.window());
        },
        window);

    auto paste_action = GUI::CommonActions::make_paste_action(
        [&](const GUI::Action&) {
            do_paste(directory_view.path(), directory_view.window());
        },
        window);
    paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "text/uri-list" && access(directory_view.path().characters(), W_OK) == 0);

    GUI::Clipboard::the().on_change = [&](const String& data_type) {
        paste_action->set_enabled(data_type == "text/uri-list" && access(directory_view.path().characters(), W_OK) == 0);
    };

    auto desktop_view_context_menu = GUI::Menu::construct("Directory View");

    auto file_manager_action = GUI::Action::create("Show in File &Manager", {}, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"), [&](const GUI::Action&) {
        Desktop::Launcher::open(URL::create_with_file_protocol(directory_view.path()));
    });

    auto display_properties_action = GUI::Action::create("&Display Settings", {}, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-settings.png"), [&](const GUI::Action&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/bin/DisplaySettings"));
    });

    desktop_view_context_menu->add_action(directory_view.mkdir_action());
    desktop_view_context_menu->add_action(directory_view.touch_action());
    desktop_view_context_menu->add_action(paste_action);
    desktop_view_context_menu->add_separator();
    desktop_view_context_menu->add_action(file_manager_action);
    desktop_view_context_menu->add_action(directory_view.open_terminal_action());
    desktop_view_context_menu->add_separator();
    desktop_view_context_menu->add_action(display_properties_action);

    auto desktop_context_menu = GUI::Menu::construct("Directory View Directory");
    desktop_context_menu->add_action(copy_action);
    desktop_context_menu->add_action(cut_action);
    desktop_context_menu->add_action(paste_action);
    desktop_context_menu->add_action(directory_view.delete_action());
    desktop_context_menu->add_action(directory_view.rename_action());
    desktop_context_menu->add_separator();
    desktop_context_menu->add_action(properties_action);

    RefPtr<GUI::Menu> file_context_menu;
    NonnullRefPtrVector<LauncherHandler> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            auto& node = directory_view.node(index);
            if (node.is_directory()) {
                desktop_context_menu->popup(event.screen_position());
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File");
                file_context_menu->add_action(copy_action);
                file_context_menu->add_action(cut_action);
                file_context_menu->add_action(paste_action);
                file_context_menu->add_action(directory_view.delete_action());
                file_context_menu->add_action(directory_view.rename_action());
                file_context_menu->add_separator();

                if (node.full_path().ends_with(".zip", AK::CaseSensitivity::CaseInsensitive)) {
                    file_context_menu->add_action(unzip_archive_action);
                    file_context_menu->add_separator();
                }

                bool added_open_menu_items = add_launch_handler_actions_to_menu(file_context_menu, directory_view, node.full_path(), file_context_menu_action_default_action, current_file_handlers);
                if (added_open_menu_items)
                    file_context_menu->add_separator();

                file_context_menu->add_action(properties_action);
                file_context_menu->popup(event.screen_position(), file_context_menu_action_default_action);
            }
        } else {
            desktop_view_context_menu->popup(event.screen_position());
        }
    };

    auto wm_config = Core::ConfigFile::get_for_app("WindowManager");
    auto selected_wallpaper = wm_config->read_entry("Background", "Wallpaper", "");
    if (!selected_wallpaper.is_empty()) {
        GUI::Desktop::the().set_wallpaper(selected_wallpaper, false);
    }

    window->show();
    return GUI::Application::the()->exec();
}

int run_in_windowed_mode(RefPtr<Core::ConfigFile> config, String initial_location, String entry_focused_on_init)
{
    auto window = GUI::Window::construct();
    window->set_title("File Manager");

    auto left = config->read_num_entry("Window", "Left", 150);
    auto top = config->read_num_entry("Window", "Top", 75);
    auto width = config->read_num_entry("Window", "Width", 640);
    auto height = config->read_num_entry("Window", "Height", 480);
    auto was_maximized = config->read_bool_entry("Window", "Maximized", false);

    auto& widget = window->set_main_widget<GUI::Widget>();

    widget.load_from_gml(file_manager_window_gml);

    auto& toolbar_container = *widget.find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    auto& main_toolbar = *widget.find_descendant_of_type_named<GUI::Toolbar>("main_toolbar");
    auto& location_toolbar = *widget.find_descendant_of_type_named<GUI::Toolbar>("location_toolbar");
    location_toolbar.layout()->set_margins({ 6, 3, 6, 3 });

    auto& location_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("location_textbox");

    auto& breadcrumb_toolbar = *widget.find_descendant_of_type_named<GUI::Toolbar>("breadcrumb_toolbar");
    breadcrumb_toolbar.layout()->set_margins({ 6, 0, 6, 0 });
    auto& breadcrumbbar = *widget.find_descendant_of_type_named<GUI::Breadcrumbbar>("breadcrumbbar");

    auto& splitter = *widget.find_descendant_of_type_named<GUI::HorizontalSplitter>("splitter");
    auto& tree_view = *widget.find_descendant_of_type_named<GUI::TreeView>("tree_view");

    auto directories_model = GUI::FileSystemModel::create({}, GUI::FileSystemModel::Mode::DirectoriesOnly);
    tree_view.set_model(directories_model);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Icon, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Size, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Owner, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Group, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Permissions, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::ModificationTime, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Inode, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::SymlinkTarget, false);
    bool is_reacting_to_tree_view_selection_change = false;

    auto& directory_view = splitter.add<DirectoryView>(DirectoryView::Mode::Normal);

    location_textbox.on_escape_pressed = [&] {
        directory_view.set_focus(true);
    };

    // Open the root directory. FIXME: This is awkward.
    tree_view.toggle_index(directories_model->index(0, 0, {}));

    auto& statusbar = *widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    auto& progressbar = *widget.find_descendant_of_type_named<GUI::Progressbar>("progressbar");
    progressbar.set_format(GUI::Progressbar::Format::ValueSlashMax);
    progressbar.set_frame_shape(Gfx::FrameShape::Panel);
    progressbar.set_frame_shadow(Gfx::FrameShadow::Sunken);
    progressbar.set_frame_thickness(1);

    location_textbox.on_return_pressed = [&] {
        directory_view.open(location_textbox.text());
    };

    auto refresh_tree_view = [&] {
        directories_model->update();

        auto current_path = directory_view.path();

        struct stat st;
        // If the directory no longer exists, we find a parent that does.
        while (stat(current_path.characters(), &st) != 0) {
            directory_view.open_parent_directory();
            current_path = directory_view.path();
            if (current_path == directories_model->root_path()) {
                break;
            }
        }

        // Reselect the existing folder in the tree.
        auto new_index = directories_model->index(current_path, GUI::FileSystemModel::Column::Name);
        if (new_index.is_valid()) {
            tree_view.expand_all_parents_of(new_index);
            tree_view.set_cursor(new_index, GUI::AbstractView::SelectionUpdate::Set, true);
        }

        directory_view.refresh();
    };

    auto directory_context_menu = GUI::Menu::construct("Directory View Directory");
    auto directory_view_context_menu = GUI::Menu::construct("Directory View");
    auto tree_view_directory_context_menu = GUI::Menu::construct("Tree View Directory");
    auto tree_view_context_menu = GUI::Menu::construct("Tree View");

    auto open_parent_directory_action = GUI::Action::create("Open &Parent Directory", { Mod_Alt, Key_Up }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [&](const GUI::Action&) {
        directory_view.open_parent_directory();
    });

    RefPtr<GUI::Action> layout_toolbar_action;
    RefPtr<GUI::Action> layout_location_action;
    RefPtr<GUI::Action> layout_statusbar_action;
    RefPtr<GUI::Action> layout_folderpane_action;

    auto show_toolbar = config->read_bool_entry("Layout", "ShowToolbar", true);
    layout_toolbar_action = GUI::Action::create_checkable("&Toolbar", [&](auto& action) {
        if (action.is_checked()) {
            main_toolbar.set_visible(true);
            toolbar_container.set_visible(true);
        } else {
            main_toolbar.set_visible(false);
            if (!location_toolbar.is_visible() && !breadcrumb_toolbar.is_visible())
                toolbar_container.set_visible(false);
        }
        show_toolbar = action.is_checked();
        config->write_bool_entry("Layout", "ShowToolbar", action.is_checked());
        config->sync();
    });
    layout_toolbar_action->set_checked(show_toolbar);
    main_toolbar.set_visible(show_toolbar);

    auto show_location = config->read_bool_entry("Layout", "ShowLocationBar", true);
    layout_location_action = GUI::Action::create_checkable("&Location Bar", [&](auto& action) {
        if (action.is_checked()) {
            breadcrumb_toolbar.set_visible(true);
            location_toolbar.set_visible(false);
            toolbar_container.set_visible(true);
        } else {
            breadcrumb_toolbar.set_visible(false);
            location_toolbar.set_visible(false);
            if (!main_toolbar.is_visible())
                toolbar_container.set_visible(false);
        }
        show_location = action.is_checked();
        config->write_bool_entry("Layout", "ShowLocationBar", action.is_checked());
        config->sync();
    });
    layout_location_action->set_checked(show_location);
    breadcrumb_toolbar.set_visible(show_location);

    toolbar_container.set_visible(show_location | show_toolbar);

    layout_statusbar_action = GUI::Action::create_checkable("&Status Bar", [&](auto& action) {
        action.is_checked() ? statusbar.set_visible(true) : statusbar.set_visible(false);
        config->write_bool_entry("Layout", "ShowStatusbar", action.is_checked());
        config->sync();
    });

    auto show_statusbar = config->read_bool_entry("Layout", "ShowStatusbar", true);
    layout_statusbar_action->set_checked(show_statusbar);
    statusbar.set_visible(show_statusbar);

    layout_folderpane_action = GUI::Action::create_checkable("&Folder Pane", { Mod_Ctrl, Key_P }, [&](auto& action) {
        action.is_checked() ? tree_view.set_visible(true) : tree_view.set_visible(false);
        config->write_bool_entry("Layout", "ShowFolderPane", action.is_checked());
        config->sync();
    });

    auto show_folderpane = config->read_bool_entry("Layout", "ShowFolderPane", true);
    layout_folderpane_action->set_checked(show_folderpane);
    tree_view.set_visible(show_folderpane);

    location_textbox.on_focusout = [&] {
        if (show_location)
            breadcrumb_toolbar.set_visible(true);
        if (!(show_location | show_toolbar))
            toolbar_container.set_visible(false);

        location_toolbar.set_visible(false);
    };

    RefPtr<GUI::Action> view_as_table_action;
    RefPtr<GUI::Action> view_as_icons_action;
    RefPtr<GUI::Action> view_as_columns_action;

    view_as_icons_action = GUI::Action::create_checkable(
        "View as &Icons", { Mod_Ctrl, KeyCode::Key_1 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/icon-view.png"), [&](const GUI::Action&) {
            directory_view.set_view_mode(DirectoryView::ViewMode::Icon);
            config->write_entry("DirectoryView", "ViewMode", "Icon");
            config->sync();
        },
        window);

    view_as_table_action = GUI::Action::create_checkable(
        "View as &Table", { Mod_Ctrl, KeyCode::Key_2 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/table-view.png"), [&](const GUI::Action&) {
            directory_view.set_view_mode(DirectoryView::ViewMode::Table);
            config->write_entry("DirectoryView", "ViewMode", "Table");
            config->sync();
        },
        window);

    view_as_columns_action = GUI::Action::create_checkable(
        "View as &Columns", { Mod_Ctrl, KeyCode::Key_3 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/columns-view.png"), [&](const GUI::Action&) {
            directory_view.set_view_mode(DirectoryView::ViewMode::Columns);
            config->write_entry("DirectoryView", "ViewMode", "Columns");
            config->sync();
        },
        window);

    auto view_type_action_group = make<GUI::ActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(*view_as_icons_action);
    view_type_action_group->add_action(*view_as_table_action);
    view_type_action_group->add_action(*view_as_columns_action);

    auto tree_view_selected_file_paths = [&] {
        Vector<String> paths;
        auto& view = tree_view;
        view.selection().for_each_index([&](const GUI::ModelIndex& index) {
            paths.append(directories_model->full_path(index));
        });
        return paths;
    };

    auto select_all_action = GUI::CommonActions::make_select_all_action([&](auto&) {
        directory_view.current_view().select_all();
    });

    auto copy_action = GUI::CommonActions::make_copy_action(
        [&](auto&) {
            auto paths = directory_view.selected_file_paths();

            if (paths.is_empty())
                paths = tree_view_selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileUtils::FileOperation::Copy);
            refresh_tree_view();
        },
        window);
    copy_action->set_enabled(false);

    auto cut_action = GUI::CommonActions::make_cut_action(
        [&](auto&) {
            auto paths = directory_view.selected_file_paths();

            if (paths.is_empty())
                paths = tree_view_selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileUtils::FileOperation::Cut);
            refresh_tree_view();
        },
        window);
    cut_action->set_enabled(false);

    auto open_in_new_window_action
        = GUI::Action::create(
            "Open in New &Window",
            {},
            Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"),
            [&](GUI::Action const& action) {
                Vector<String> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view.selected_file_paths();

                for (auto& path : paths) {
                    if (Core::File::is_directory(path))
                        Desktop::Launcher::open(URL::create_with_file_protocol(path));
                }
            },
            window);

    auto open_in_new_terminal_action
        = GUI::Action::create(
            "Open in &Terminal",
            {},
            Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"),
            [&](GUI::Action const& action) {
                Vector<String> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view.selected_file_paths();

                for (auto& path : paths) {
                    if (Core::File::is_directory(path)) {
                        spawn_terminal(path);
                    }
                }
            },
            window);

    auto shortcut_action
        = GUI::Action::create(
            "Create Desktop &Shortcut",
            {},
            Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-symlink.png"),
            [&](const GUI::Action&) {
                auto paths = directory_view.selected_file_paths();
                if (paths.is_empty()) {
                    return;
                }
                do_create_link(paths, directory_view.window());
            },
            window);

    auto unzip_archive_action
        = GUI::Action::create(
            "E&xtract Here",
            [&](const GUI::Action&) {
                auto paths = directory_view.selected_file_paths();
                if (paths.is_empty())
                    return;

                do_unzip_archive(paths, directory_view.window());
                refresh_tree_view();
            },
            window);

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto& action) {
            String container_dir_path;
            String path;
            Vector<String> selected;
            if (action.activator() == directory_context_menu || directory_view.active_widget()->is_focused()) {
                path = directory_view.path();
                container_dir_path = path;
                selected = directory_view.selected_file_paths();
            } else {
                path = directories_model->full_path(tree_view.selection().first());
                container_dir_path = LexicalPath::basename(path);
                selected = tree_view_selected_file_paths();
            }

            show_properties(container_dir_path, path, selected, directory_view.window());
        },
        window);

    auto paste_action = GUI::CommonActions::make_paste_action(
        [&](const GUI::Action& action) {
            String target_directory;
            if (action.activator() == directory_context_menu)
                target_directory = directory_view.selected_file_paths()[0];
            else
                target_directory = directory_view.path();
            do_paste(target_directory, directory_view.window());
            refresh_tree_view();
        },
        window);

    auto folder_specific_paste_action = GUI::CommonActions::make_paste_action(
        [&](const GUI::Action& action) {
            String target_directory;
            if (action.activator() == directory_context_menu)
                target_directory = directory_view.selected_file_paths()[0];
            else
                target_directory = directory_view.path();
            do_paste(target_directory, directory_view.window());
            refresh_tree_view();
        },
        window);

    auto go_back_action = GUI::CommonActions::make_go_back_action(
        [&](auto&) {
            directory_view.open_previous_directory();
        },
        window);

    auto go_forward_action = GUI::CommonActions::make_go_forward_action(
        [&](auto&) {
            directory_view.open_next_directory();
        },
        window);

    auto go_home_action = GUI::CommonActions::make_go_home_action(
        [&](auto&) {
            directory_view.open(Core::StandardPaths::home_directory());
        },
        window);

    GUI::Clipboard::the().on_change = [&](const String& data_type) {
        auto current_location = directory_view.path();
        paste_action->set_enabled(data_type == "text/uri-list" && access(current_location.characters(), W_OK) == 0);
    };

    auto tree_view_delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            FileUtils::delete_paths(tree_view_selected_file_paths(), true, window);
            refresh_tree_view();
        },
        &tree_view);

    // This is a little awkward. The menu action does something different depending on which view has focus.
    // It would be nice to find a good abstraction for this instead of creating a branching action like this.
    auto focus_dependent_delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        if (tree_view.is_focused())
            tree_view_delete_action->activate();
        else
            directory_view.delete_action().activate();
        refresh_tree_view();
    });
    focus_dependent_delete_action->set_enabled(false);

    auto mkdir_action = GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"), [&](const GUI::Action&) {
        directory_view.mkdir_action().activate();
        refresh_tree_view();
    });

    auto touch_action = GUI::Action::create("New &File...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [&](const GUI::Action&) {
        directory_view.touch_action().activate();
        refresh_tree_view();
    });

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(mkdir_action);
    file_menu.add_action(touch_action);
    file_menu.add_action(focus_dependent_delete_action);
    file_menu.add_action(directory_view.rename_action());
    file_menu.add_separator();
    file_menu.add_action(properties_action);
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = menubar->add_menu("&Edit");
    edit_menu.add_action(copy_action);
    edit_menu.add_action(cut_action);
    edit_menu.add_action(paste_action);
    edit_menu.add_separator();
    edit_menu.add_action(select_all_action);

    auto action_show_dotfiles = GUI::Action::create_checkable("&Show Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& action) {
        directory_view.set_should_show_dotfiles(action.is_checked());
        refresh_tree_view();
        config->write_bool_entry("DirectoryView", "ShowDotFiles", action.is_checked());
        config->sync();
    });

    auto show_dotfiles = config->read_bool_entry("DirectoryView", "ShowDotFiles", false);
    directory_view.set_should_show_dotfiles(show_dotfiles);
    action_show_dotfiles->set_checked(show_dotfiles);

    auto& view_menu = menubar->add_menu("&View");
    auto& layout_menu = view_menu.add_submenu("&Layout");
    layout_menu.add_action(*layout_toolbar_action);
    layout_menu.add_action(*layout_location_action);
    layout_menu.add_action(*layout_statusbar_action);
    layout_menu.add_action(*layout_folderpane_action);

    view_menu.add_separator();

    view_menu.add_action(*view_as_icons_action);
    view_menu.add_action(*view_as_table_action);
    view_menu.add_action(*view_as_columns_action);
    view_menu.add_separator();
    view_menu.add_action(action_show_dotfiles);

    auto go_to_location_action = GUI::Action::create("Go to &Location...", { Mod_Ctrl, Key_L }, [&](auto&) {
        toolbar_container.set_visible(true);
        location_toolbar.set_visible(true);
        breadcrumb_toolbar.set_visible(false);
        location_textbox.select_all();
        location_textbox.set_focus(true);
    });

    auto& go_menu = menubar->add_menu("&Go");
    go_menu.add_action(go_back_action);
    go_menu.add_action(go_forward_action);
    go_menu.add_action(open_parent_directory_action);
    go_menu.add_action(go_home_action);
    go_menu.add_action(go_to_location_action);
    go_menu.add_separator();
    go_menu.add_action(directory_view.open_terminal_action());

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("File Manager", GUI::Icon::default_icon("app-file-manager"), window));

    window->set_menubar(menubar);

    main_toolbar.add_action(go_back_action);
    main_toolbar.add_action(go_forward_action);
    main_toolbar.add_action(open_parent_directory_action);
    main_toolbar.add_action(go_home_action);

    main_toolbar.add_separator();
    main_toolbar.add_action(mkdir_action);
    main_toolbar.add_action(touch_action);
    main_toolbar.add_action(focus_dependent_delete_action);

    main_toolbar.add_separator();
    main_toolbar.add_action(copy_action);
    main_toolbar.add_action(cut_action);
    main_toolbar.add_action(paste_action);

    main_toolbar.add_separator();
    main_toolbar.add_action(directory_view.open_terminal_action());

    main_toolbar.add_separator();
    main_toolbar.add_action(*view_as_icons_action);
    main_toolbar.add_action(*view_as_table_action);
    main_toolbar.add_action(*view_as_columns_action);

    directory_view.on_path_change = [&](const String& new_path, bool can_read_in_path, bool can_write_in_path) {
        auto icon = GUI::FileIconProvider::icon_for_path(new_path);
        auto* bitmap = icon.bitmap_for_size(16);
        window->set_icon(bitmap);
        location_textbox.set_icon(bitmap);

        window->set_title(String::formatted("{} - File Manager", new_path));
        location_textbox.set_text(new_path);

        {
            LexicalPath lexical_path(new_path);

            auto segment_index_of_new_path_in_breadcrumbbar = breadcrumbbar.find_segment_with_data(new_path);

            if (segment_index_of_new_path_in_breadcrumbbar.has_value()) {
                auto new_segment_index = segment_index_of_new_path_in_breadcrumbbar.value();
                breadcrumbbar.set_selected_segment(new_segment_index);

                // If the path change was because the directory we were in was deleted,
                // remove the breadcrumbs for it.
                if ((new_segment_index + 1 < breadcrumbbar.segment_count())
                    && !Core::File::is_directory(breadcrumbbar.segment_data(new_segment_index + 1))) {
                    breadcrumbbar.remove_end_segments(new_segment_index + 1);
                }
            } else {
                breadcrumbbar.clear_segments();

                breadcrumbbar.append_segment("/", GUI::FileIconProvider::icon_for_path("/").bitmap_for_size(16), "/", "/");
                StringBuilder builder;

                for (auto& part : lexical_path.parts()) {
                    // NOTE: We rebuild the path as we go, so we have something to pass to GUI::FileIconProvider.
                    builder.append('/');
                    builder.append(part);

                    breadcrumbbar.append_segment(part, GUI::FileIconProvider::icon_for_path(builder.string_view()).bitmap_for_size(16), builder.string_view(), builder.string_view());
                }

                breadcrumbbar.set_selected_segment(breadcrumbbar.segment_count() - 1);

                breadcrumbbar.on_segment_click = [&](size_t segment_index) {
                    auto selected_path = breadcrumbbar.segment_data(segment_index);
                    if (Core::File::is_directory(selected_path)) {
                        directory_view.open(selected_path);
                    } else {
                        dbgln("Breadcrumb path '{}' doesn't exist", selected_path);
                        breadcrumbbar.remove_end_segments(segment_index);
                        auto existing_path_segment = breadcrumbbar.find_segment_with_data(directory_view.path());
                        breadcrumbbar.set_selected_segment(existing_path_segment.value());
                    }
                };
            }
        }

        if (!is_reacting_to_tree_view_selection_change) {
            auto new_index = directories_model->index(new_path, GUI::FileSystemModel::Column::Name);
            if (new_index.is_valid()) {
                tree_view.expand_all_parents_of(new_index);
                tree_view.set_cursor(new_index, GUI::AbstractView::SelectionUpdate::Set);
            }
        }

        struct stat st;
        if (lstat(new_path.characters(), &st)) {
            perror("stat");
            return;
        }

        paste_action->set_enabled(can_write_in_path && GUI::Clipboard::the().mime_type() == "text/uri-list");
        go_forward_action->set_enabled(directory_view.path_history_position() < directory_view.path_history_size() - 1);
        go_back_action->set_enabled(directory_view.path_history_position() > 0);
        open_parent_directory_action->set_enabled(new_path != "/");
        view_as_table_action->set_enabled(can_read_in_path);
        view_as_icons_action->set_enabled(can_read_in_path);
        view_as_columns_action->set_enabled(can_read_in_path);
    };

    directory_view.on_accepted_drop = [&] {
        refresh_tree_view();
    };

    directory_view.on_status_message = [&](const StringView& message) {
        statusbar.set_text(message);
    };

    directory_view.on_thumbnail_progress = [&](int done, int total) {
        if (done == total) {
            progressbar.set_visible(false);
            return;
        }
        progressbar.set_range(0, total);
        progressbar.set_value(done);
        progressbar.set_visible(true);
    };

    directory_view.on_selection_change = [&](GUI::AbstractView& view) {
        auto& selection = view.selection();
        copy_action->set_enabled(!selection.is_empty());
        cut_action->set_enabled(!selection.is_empty());
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && tree_view.is_focused())
            || !directory_view.current_view().selection().is_empty());
    };

    directory_context_menu->add_action(open_in_new_window_action);
    directory_context_menu->add_action(open_in_new_terminal_action);
    directory_context_menu->add_action(copy_action);
    directory_context_menu->add_action(cut_action);
    directory_context_menu->add_action(folder_specific_paste_action);
    directory_context_menu->add_action(directory_view.delete_action());
    directory_context_menu->add_action(directory_view.rename_action());
    directory_context_menu->add_action(shortcut_action);
    directory_context_menu->add_separator();
    directory_context_menu->add_action(properties_action);

    directory_view_context_menu->add_action(mkdir_action);
    directory_view_context_menu->add_action(touch_action);
    directory_view_context_menu->add_action(paste_action);
    directory_view_context_menu->add_action(directory_view.open_terminal_action());
    directory_view_context_menu->add_separator();
    directory_view_context_menu->add_action(action_show_dotfiles);
    directory_view_context_menu->add_separator();
    directory_view_context_menu->add_action(properties_action);

    tree_view_directory_context_menu->add_action(open_in_new_window_action);
    tree_view_directory_context_menu->add_action(open_in_new_terminal_action);
    tree_view_directory_context_menu->add_action(copy_action);
    tree_view_directory_context_menu->add_action(cut_action);
    tree_view_directory_context_menu->add_action(paste_action);
    tree_view_directory_context_menu->add_action(tree_view_delete_action);
    tree_view_directory_context_menu->add_action(directory_view.rename_action());
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(properties_action);
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(mkdir_action);
    tree_view_directory_context_menu->add_action(touch_action);

    RefPtr<GUI::Menu> file_context_menu;
    NonnullRefPtrVector<LauncherHandler> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            auto& node = directory_view.node(index);

            if (node.is_directory()) {
                auto should_get_enabled = access(node.full_path().characters(), W_OK) == 0 && GUI::Clipboard::the().mime_type() == "text/uri-list";
                folder_specific_paste_action->set_enabled(should_get_enabled);
                directory_context_menu->popup(event.screen_position());
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File");
                file_context_menu->add_action(copy_action);
                file_context_menu->add_action(cut_action);
                file_context_menu->add_action(paste_action);
                file_context_menu->add_action(directory_view.delete_action());
                file_context_menu->add_action(directory_view.rename_action());
                file_context_menu->add_action(shortcut_action);
                file_context_menu->add_separator();

                if (node.full_path().ends_with(".zip", AK::CaseSensitivity::CaseInsensitive)) {
                    file_context_menu->add_action(unzip_archive_action);
                    file_context_menu->add_separator();
                }

                bool added_launch_file_handlers = add_launch_handler_actions_to_menu(file_context_menu, directory_view, node.full_path(), file_context_menu_action_default_action, current_file_handlers);
                if (added_launch_file_handlers)
                    file_context_menu->add_separator();

                file_context_menu->add_action(properties_action);
                file_context_menu->popup(event.screen_position(), file_context_menu_action_default_action);
            }
        } else {
            directory_view_context_menu->popup(event.screen_position());
        }
    };

    tree_view.on_selection_change = [&] {
        const auto& index = tree_view.selection().first();
        if (directories_model->m_previously_selected_index.is_valid())
            directories_model->update_node_on_selection(directories_model->m_previously_selected_index, false);

        directories_model->update_node_on_selection(index, true);
        directories_model->m_previously_selected_index = index;
    };

    tree_view.on_selection_change = [&] {
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && tree_view.is_focused())
            || !directory_view.current_view().selection().is_empty());

        if (tree_view.selection().is_empty())
            return;
        auto path = directories_model->full_path(tree_view.selection().first());
        if (directory_view.path() == path)
            return;
        TemporaryChange change(is_reacting_to_tree_view_selection_change, true);
        directory_view.open(path);
        copy_action->set_enabled(!tree_view.selection().is_empty());
        cut_action->set_enabled(!tree_view.selection().is_empty());
        directory_view.delete_action().set_enabled(!tree_view.selection().is_empty());
    };

    tree_view.on_focus_change = [&]([[maybe_unused]] const bool has_focus, [[maybe_unused]] const GUI::FocusSource source) {
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && has_focus)
            || !directory_view.current_view().selection().is_empty());
    };

    tree_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            tree_view_directory_context_menu->popup(event.screen_position());
        }
    };

    auto copy_urls_to_directory = [&](const Vector<URL>& urls, const String& directory) {
        if (urls.is_empty()) {
            dbgln("No files to copy");
            return;
        }
        bool had_accepted_copy = false;
        for (auto& url_to_copy : urls) {
            if (!url_to_copy.is_valid() || url_to_copy.path() == directory)
                continue;
            auto new_path = String::formatted("{}/{}", directory, LexicalPath::basename(url_to_copy.path()));
            if (url_to_copy.path() == new_path)
                continue;

            if (auto result = Core::File::copy_file_or_directory(url_to_copy.path(), new_path); result.is_error()) {
                auto error_message = String::formatted("Could not copy {} into {}:\n {}", url_to_copy.to_string(), new_path, result.error().error_code);
                GUI::MessageBox::show(window, error_message, "File Manager", GUI::MessageBox::Type::Error);
            } else {
                had_accepted_copy = true;
            }
        }
        if (had_accepted_copy)
            refresh_tree_view();
    };

    breadcrumbbar.on_segment_drop = [&](size_t segment_index, const GUI::DropEvent& event) {
        if (!event.mime_data().has_urls())
            return;
        copy_urls_to_directory(event.mime_data().urls(), breadcrumbbar.segment_data(segment_index));
    };

    breadcrumbbar.on_segment_drag_enter = [&](size_t, GUI::DragEvent& event) {
        if (event.mime_types().contains_slow("text/uri-list"))
            event.accept();
    };

    breadcrumbbar.on_doubleclick = [&](const GUI::MouseEvent&) {
        go_to_location_action->activate();
    };

    tree_view.on_drop = [&](const GUI::ModelIndex& index, const GUI::DropEvent& event) {
        if (!event.mime_data().has_urls())
            return;
        auto& target_node = directories_model->node(index);
        if (!target_node.is_directory())
            return;
        copy_urls_to_directory(event.mime_data().urls(), target_node.full_path());
        const_cast<GUI::DropEvent&>(event).accept();
    };

    directory_view.open(initial_location);
    directory_view.set_focus(true);

    paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "text/uri-list" && access(initial_location.characters(), W_OK) == 0);

    window->show();

    window->set_rect({ left, top, width, height });
    if (was_maximized)
        window->set_maximized(true);

    // Read directory read mode from config.
    auto dir_view_mode = config->read_entry("DirectoryView", "ViewMode", "Icon");

    if (dir_view_mode.contains("Table")) {
        directory_view.set_view_mode(DirectoryView::ViewMode::Table);
        view_as_table_action->set_checked(true);
    } else if (dir_view_mode.contains("Columns")) {
        directory_view.set_view_mode(DirectoryView::ViewMode::Columns);
        view_as_columns_action->set_checked(true);
    } else {
        directory_view.set_view_mode(DirectoryView::ViewMode::Icon);
        view_as_icons_action->set_checked(true);
    }

    if (!entry_focused_on_init.is_empty()) {
        auto matches = directory_view.current_view().model()->matches(entry_focused_on_init, GUI::Model::MatchesFlag::MatchFull | GUI::Model::MatchesFlag::FirstMatchOnly);
        if (!matches.is_empty())
            directory_view.current_view().set_cursor(matches.first(), GUI::AbstractView::SelectionUpdate::Set);
    }

    // Write window position to config file on close request.
    window->on_close_request = [&] {
        config->write_bool_entry("Window", "Maximized", window->is_maximized());
        if (!window->is_maximized()) {
            config->write_num_entry("Window", "Left", window->x());
            config->write_num_entry("Window", "Top", window->y());
            config->write_num_entry("Window", "Width", window->width());
            config->write_num_entry("Window", "Height", window->height());
        }
        config->sync();

        return GUI::Window::CloseRequestDecision::Close;
    };

    return GUI::Application::the()->exec();
}
