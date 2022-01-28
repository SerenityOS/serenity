/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
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
#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
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
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace FileManager;

static ErrorOr<int> run_in_desktop_mode();
static ErrorOr<int> run_in_windowed_mode(String const& initial_location, String const& entry_focused_on_init);
static void do_copy(Vector<String> const& selected_file_paths, FileOperation file_operation);
static void do_paste(String const& target_directory, GUI::Window* window);
static void do_create_link(Vector<String> const& selected_file_paths, GUI::Window* window);
static void do_create_archive(Vector<String> const& selected_file_paths, GUI::Window* window);
static void do_unzip_archive(Vector<String> const& selected_file_paths, GUI::Window* window);
static void show_properties(String const& container_dir_path, String const& path, Vector<String> const& selected, GUI::Window* window);
static bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, DirectoryView const& directory_view, String const& full_path, RefPtr<GUI::Action>& default_action, NonnullRefPtrVector<LauncherHandler>& current_file_launch_handlers);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd unix cpath rpath wpath fattr proc exec sigaction"));

    struct sigaction act = {};
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));

    Core::ArgsParser args_parser;
    bool is_desktop_mode { false };
    bool is_selection_mode { false };
    bool ignore_path_resolution { false };
    String initial_location;
    args_parser.add_option(is_desktop_mode, "Run in desktop mode", "desktop", 'd');
    args_parser.add_option(is_selection_mode, "Show entry in parent folder", "select", 's');
    args_parser.add_option(ignore_path_resolution, "Use raw path, do not resolve real path", "raw", 'r');
    args_parser.add_positional_argument(initial_location, "Path to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio thread recvfd sendfd cpath rpath wpath fattr proc exec unix"));

    Config::pledge_domains({ "FileManager", "WindowManager" });
    Config::monitor_domain("FileManager");
    Config::monitor_domain("WindowManager");

    if (is_desktop_mode)
        return run_in_desktop_mode();

    // our initial location is defined as, in order of precedence:
    // 1. the command-line path argument (e.g. FileManager /bin)
    // 2. the current directory
    // 3. the user's home directory
    // 4. the root directory

    if (!initial_location.is_empty()) {
        if (!ignore_path_resolution)
            initial_location = Core::File::real_path_for(initial_location);

        if (!Core::File::is_directory(initial_location))
            is_selection_mode = true;
    }

    if (initial_location.is_empty())
        initial_location = Core::File::current_working_directory();

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

    return run_in_windowed_mode(initial_location, focused_entry);
}

void do_copy(Vector<String> const& selected_file_paths, FileOperation file_operation)
{
    if (selected_file_paths.is_empty())
        VERIFY_NOT_REACHED();

    StringBuilder copy_text;
    if (file_operation == FileOperation::Move) {
        copy_text.append("#cut\n"); // This exploits the comment lines in the text/uri-list specification, which might be a bit hackish
    }
    for (auto& path : selected_file_paths) {
        auto url = URL::create_with_file_protocol(path);
        copy_text.appendff("{}\n", url);
    }
    GUI::Clipboard::the().set_data(copy_text.build().bytes(), "text/uri-list");
}

void do_paste(String const& target_directory, GUI::Window* window)
{
    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    if (data_and_type.mime_type != "text/uri-list") {
        dbgln("Cannot paste clipboard type {}", data_and_type.mime_type);
        return;
    }
    auto copied_lines = String::copy(data_and_type.data).split('\n');
    if (copied_lines.is_empty()) {
        dbgln("No files to paste");
        return;
    }

    FileOperation file_operation = FileOperation::Copy;
    if (copied_lines[0] == "#cut") { // cut operation encoded as a text/uri-list comment
        file_operation = FileOperation::Move;
        copied_lines.remove(0);
    }

    Vector<String> source_paths;
    for (auto& uri_as_string : copied_lines) {
        if (uri_as_string.is_empty())
            continue;
        URL url = uri_as_string;
        if (!url.is_valid() || url.protocol() != "file") {
            dbgln("Cannot paste URI {}", uri_as_string);
            continue;
        }
        source_paths.append(url.path());
    }

    if (!source_paths.is_empty())
        run_file_operation(file_operation, source_paths, target_directory, window);
}

void do_create_link(Vector<String> const& selected_file_paths, GUI::Window* window)
{
    auto path = selected_file_paths.first();
    auto destination = String::formatted("{}/{}", Core::StandardPaths::desktop_directory(), LexicalPath::basename(path));
    if (auto result = Core::File::link_file(destination, path); result.is_error()) {
        GUI::MessageBox::show(window, String::formatted("Could not create desktop shortcut:\n{}", result.error()), "File Manager",
            GUI::MessageBox::Type::Error);
    }
}

void do_create_archive(Vector<String> const& selected_file_paths, GUI::Window* window)
{
    String archive_name;
    if (GUI::InputBox::show(window, archive_name, "Enter name:", "Create Archive") != GUI::InputBox::ExecOK)
        return;

    auto output_directory_path = LexicalPath(selected_file_paths.first());

    StringBuilder path_builder;
    path_builder.append(output_directory_path.dirname());
    path_builder.append("/");
    if (archive_name.is_empty()) {
        path_builder.append(output_directory_path.parent().basename());
        path_builder.append(".zip");
    } else {
        path_builder.append(archive_name);
        if (!archive_name.ends_with(".zip"))
            path_builder.append(".zip");
    }
    auto output_path = path_builder.build();

    pid_t zip_pid = fork();
    if (zip_pid < 0) {
        perror("fork");
        VERIFY_NOT_REACHED();
    }

    if (!zip_pid) {
        Vector<String> relative_paths;
        Vector<char const*> arg_list;
        arg_list.append("/bin/zip");
        arg_list.append("-r");
        arg_list.append("-f");
        arg_list.append(output_path.characters());
        for (auto const& path : selected_file_paths) {
            relative_paths.append(LexicalPath::relative_path(path, output_directory_path.dirname()));
            arg_list.append(relative_paths.last().characters());
        }
        arg_list.append(nullptr);
        int rc = execvp("/bin/zip", const_cast<char* const*>(arg_list.data()));
        if (rc < 0) {
            perror("execvp");
            _exit(1);
        }
    } else {
        int status;
        int rc = waitpid(zip_pid, &status, 0);
        if (rc < 0 || !WIFEXITED(status) || WEXITSTATUS(status) != 0)
            GUI::MessageBox::show(window, "Could not create archive", "Archive Error", GUI::MessageBox::Type::Error);
    }
}

void do_unzip_archive(Vector<String> const& selected_file_paths, GUI::Window* window)
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

void show_properties(String const& container_dir_path, String const& path, Vector<String> const& selected, GUI::Window* window)
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

bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, DirectoryView const& directory_view, String const& full_path, RefPtr<GUI::Action>& default_action, NonnullRefPtrVector<LauncherHandler>& current_file_launch_handlers)
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

ErrorOr<int> run_in_desktop_mode()
{
    static constexpr char const* process_name = "FileManager (Desktop)";
    set_process_name(process_name, strlen(process_name));
    pthread_setname_np(pthread_self(), process_name);

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Desktop Manager");
    window->set_window_type(GUI::WindowType::Desktop);
    window->set_has_alpha_channel(true);

    auto desktop_widget = TRY(window->try_set_main_widget<FileManager::DesktopWidget>());
    (void)TRY(desktop_widget->try_set_layout<GUI::VerticalBoxLayout>());

    auto directory_view = TRY(desktop_widget->try_add<DirectoryView>(DirectoryView::Mode::Desktop));
    directory_view->set_name("directory_view");

    auto cut_action = GUI::CommonActions::make_cut_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileOperation::Move);
        },
        window);
    cut_action->set_enabled(false);

    auto copy_action = GUI::CommonActions::make_copy_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileOperation::Copy);
        },
        window);
    copy_action->set_enabled(false);

    auto create_archive_action
        = GUI::Action::create(
            "Create &Archive",
            Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-archive.png").release_value_but_fixme_should_propagate_errors(),
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_create_archive(paths, directory_view->window());
            },
            window);

    auto unzip_archive_action
        = GUI::Action::create(
            "E&xtract Here",
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_unzip_archive(paths, directory_view->window());
            },
            window);

    directory_view->on_selection_change = [&](GUI::AbstractView const& view) {
        cut_action->set_enabled(!view.selection().is_empty());
        copy_action->set_enabled(!view.selection().is_empty());
    };

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto&) {
            String path = directory_view->path();
            Vector<String> selected = directory_view->selected_file_paths();

            show_properties(path, path, selected, directory_view->window());
        },
        window);

    auto paste_action = GUI::CommonActions::make_paste_action(
        [&](GUI::Action const&) {
            do_paste(directory_view->path(), directory_view->window());
        },
        window);
    paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "text/uri-list" && access(directory_view->path().characters(), W_OK) == 0);

    GUI::Clipboard::the().on_change = [&](String const& data_type) {
        paste_action->set_enabled(data_type == "text/uri-list" && access(directory_view->path().characters(), W_OK) == 0);
    };

    auto desktop_view_context_menu = TRY(GUI::Menu::try_create("Directory View"));

    auto file_manager_action = GUI::Action::create("Open in File &Manager", {}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-file-manager.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto paths = directory_view->selected_file_paths();
        if (paths.is_empty()) {
            Desktop::Launcher::open(URL::create_with_file_protocol(directory_view->path()));
            return;
        }

        for (auto& path : paths) {
            if (Core::File::is_directory(path))
                Desktop::Launcher::open(URL::create_with_file_protocol(path));
        }
    });

    auto open_terminal_action = GUI::Action::create("Open in &Terminal", {}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-terminal.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto paths = directory_view->selected_file_paths();
        if (paths.is_empty()) {
            spawn_terminal(directory_view->path());
            return;
        }

        for (auto& path : paths) {
            if (Core::File::is_directory(path)) {
                spawn_terminal(path);
            }
        }
    });

    auto display_properties_action = GUI::Action::create("&Display Settings", {}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-display-settings.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/bin/DisplaySettings"));
    });

    TRY(desktop_view_context_menu->try_add_action(directory_view->mkdir_action()));
    TRY(desktop_view_context_menu->try_add_action(directory_view->touch_action()));
    TRY(desktop_view_context_menu->try_add_action(paste_action));
    TRY(desktop_view_context_menu->try_add_separator());
    TRY(desktop_view_context_menu->try_add_action(file_manager_action));
    TRY(desktop_view_context_menu->try_add_action(open_terminal_action));
    TRY(desktop_view_context_menu->try_add_separator());
    TRY(desktop_view_context_menu->try_add_action(display_properties_action));

    auto desktop_context_menu = TRY(GUI::Menu::try_create("Directory View Directory"));

    TRY(desktop_context_menu->try_add_action(file_manager_action));
    TRY(desktop_context_menu->try_add_action(open_terminal_action));
    TRY(desktop_context_menu->try_add_separator());
    TRY(desktop_context_menu->try_add_action(cut_action));
    TRY(desktop_context_menu->try_add_action(copy_action));
    TRY(desktop_context_menu->try_add_action(paste_action));
    TRY(desktop_context_menu->try_add_action(directory_view->delete_action()));
    TRY(desktop_context_menu->try_add_action(directory_view->rename_action()));
    TRY(desktop_context_menu->try_add_separator());
    TRY(desktop_context_menu->try_add_action(properties_action));

    RefPtr<GUI::Menu> file_context_menu;
    NonnullRefPtrVector<LauncherHandler> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            auto& node = directory_view->node(index);
            if (node.is_directory()) {
                desktop_context_menu->popup(event.screen_position(), file_manager_action);
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File");

                bool added_open_menu_items = add_launch_handler_actions_to_menu(file_context_menu, directory_view, node.full_path(), file_context_menu_action_default_action, current_file_handlers);
                if (added_open_menu_items)
                    file_context_menu->add_separator();

                file_context_menu->add_action(cut_action);
                file_context_menu->add_action(copy_action);
                file_context_menu->add_action(paste_action);
                file_context_menu->add_action(directory_view->delete_action());
                file_context_menu->add_action(directory_view->rename_action());
                file_context_menu->add_action(create_archive_action);
                file_context_menu->add_separator();

                if (node.full_path().ends_with(".zip", AK::CaseSensitivity::CaseInsensitive)) {
                    file_context_menu->add_action(unzip_archive_action);
                    file_context_menu->add_separator();
                }

                file_context_menu->add_action(properties_action);
                file_context_menu->popup(event.screen_position(), file_context_menu_action_default_action);
            }
        } else {
            desktop_view_context_menu->popup(event.screen_position());
        }
    };

    struct BackgroundWallpaperListener : Config::Listener {
        virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override
        {
            if (domain == "WindowManager" && group == "Background" && key == "Wallpaper")
                GUI::Desktop::the().set_wallpaper(value, false);
        }
    } wallpaper_listener;

    auto selected_wallpaper = Config::read_string("WindowManager", "Background", "Wallpaper", "");
    if (!selected_wallpaper.is_empty()) {
        GUI::Desktop::the().set_wallpaper(selected_wallpaper, false);
    }

    window->show();
    return GUI::Application::the()->exec();
}

ErrorOr<int> run_in_windowed_mode(String const& initial_location, String const& entry_focused_on_init)
{
    auto window = TRY(GUI::Window::try_create());
    window->set_title("File Manager");

    auto left = Config::read_i32("FileManager", "Window", "Left", 150);
    auto top = Config::read_i32("FileManager", "Window", "Top", 75);
    auto width = Config::read_i32("FileManager", "Window", "Width", 640);
    auto height = Config::read_i32("FileManager", "Window", "Height", 480);
    auto was_maximized = Config::read_bool("FileManager", "Window", "Maximized", false);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());

    widget->load_from_gml(file_manager_window_gml);

    auto& toolbar_container = *widget->find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    auto& main_toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("main_toolbar");
    auto& location_toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("location_toolbar");
    location_toolbar.layout()->set_margins({ 3, 6 });

    auto& location_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("location_textbox");

    auto& breadcrumb_toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("breadcrumb_toolbar");
    breadcrumb_toolbar.layout()->set_margins({ 0, 6 });
    auto& breadcrumbbar = *widget->find_descendant_of_type_named<GUI::Breadcrumbbar>("breadcrumbbar");

    auto& splitter = *widget->find_descendant_of_type_named<GUI::HorizontalSplitter>("splitter");
    auto& tree_view = *widget->find_descendant_of_type_named<GUI::TreeView>("tree_view");

    auto directories_model = GUI::FileSystemModel::create({}, GUI::FileSystemModel::Mode::DirectoriesOnly);
    tree_view.set_model(directories_model);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Icon, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Size, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::User, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Group, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Permissions, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::ModificationTime, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::Inode, false);
    tree_view.set_column_visible(GUI::FileSystemModel::Column::SymlinkTarget, false);
    bool is_reacting_to_tree_view_selection_change = false;

    auto directory_view = TRY(splitter.try_add<DirectoryView>(DirectoryView::Mode::Normal));
    directory_view->set_name("directory_view");

    location_textbox.on_escape_pressed = [&] {
        directory_view->set_focus(true);
    };

    // Open the root directory. FIXME: This is awkward.
    tree_view.toggle_index(directories_model->index(0, 0, {}));

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    auto& progressbar = *widget->find_descendant_of_type_named<GUI::Progressbar>("progressbar");
    progressbar.set_format(GUI::Progressbar::Format::ValueSlashMax);
    progressbar.set_frame_shape(Gfx::FrameShape::Panel);
    progressbar.set_frame_shadow(Gfx::FrameShadow::Sunken);
    progressbar.set_frame_thickness(1);

    location_textbox.on_return_pressed = [&] {
        if (directory_view->open(location_textbox.text()))
            location_textbox.set_focus(false);
    };

    auto refresh_tree_view = [&] {
        directories_model->invalidate();

        auto current_path = directory_view->path();

        struct stat st;
        // If the directory no longer exists, we find a parent that does.
        while (stat(current_path.characters(), &st) != 0) {
            directory_view->open_parent_directory();
            current_path = directory_view->path();
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

        directory_view->refresh();
    };

    auto directory_context_menu = TRY(GUI::Menu::try_create("Directory View Directory"));
    auto directory_view_context_menu = TRY(GUI::Menu::try_create("Directory View"));
    auto tree_view_directory_context_menu = TRY(GUI::Menu::try_create("Tree View Directory"));

    auto open_parent_directory_action = GUI::Action::create("Open &Parent Directory", { Mod_Alt, Key_Up }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open-parent-directory.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        directory_view->open_parent_directory();
    });

    RefPtr<GUI::Action> layout_toolbar_action;
    RefPtr<GUI::Action> layout_location_action;
    RefPtr<GUI::Action> layout_statusbar_action;
    RefPtr<GUI::Action> layout_folderpane_action;

    auto show_toolbar = Config::read_bool("FileManager", "Layout", "ShowToolbar", true);
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
        Config::write_bool("FileManager", "Layout", "ShowToolbar", action.is_checked());
    });
    layout_toolbar_action->set_checked(show_toolbar);
    main_toolbar.set_visible(show_toolbar);

    auto show_location = Config::read_bool("FileManager", "Layout", "ShowLocationBar", true);
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
        Config::write_bool("FileManager", "Layout", "ShowLocationBar", action.is_checked());
    });
    layout_location_action->set_checked(show_location);
    breadcrumb_toolbar.set_visible(show_location);

    toolbar_container.set_visible(show_location | show_toolbar);

    layout_statusbar_action = GUI::Action::create_checkable("&Status Bar", [&](auto& action) {
        action.is_checked() ? statusbar.set_visible(true) : statusbar.set_visible(false);
        Config::write_bool("FileManager", "Layout", "ShowStatusbar", action.is_checked());
    });

    auto show_statusbar = Config::read_bool("FileManager", "Layout", "ShowStatusbar", true);
    layout_statusbar_action->set_checked(show_statusbar);
    statusbar.set_visible(show_statusbar);

    layout_folderpane_action = GUI::Action::create_checkable("&Folder Pane", { Mod_Ctrl, Key_P }, [&](auto& action) {
        action.is_checked() ? tree_view.set_visible(true) : tree_view.set_visible(false);
        Config::write_bool("FileManager", "Layout", "ShowFolderPane", action.is_checked());
    });

    auto show_folderpane = Config::read_bool("FileManager", "Layout", "ShowFolderPane", true);
    layout_folderpane_action->set_checked(show_folderpane);
    tree_view.set_visible(show_folderpane);

    location_textbox.on_focusout = [&] {
        if (show_location)
            breadcrumb_toolbar.set_visible(true);
        if (!(show_location | show_toolbar))
            toolbar_container.set_visible(false);

        location_toolbar.set_visible(false);
    };

    auto view_type_action_group = make<GUI::ActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(directory_view->view_as_icons_action());
    view_type_action_group->add_action(directory_view->view_as_table_action());
    view_type_action_group->add_action(directory_view->view_as_columns_action());

    auto tree_view_selected_file_paths = [&] {
        Vector<String> paths;
        auto& view = tree_view;
        view.selection().for_each_index([&](GUI::ModelIndex const& index) {
            paths.append(directories_model->full_path(index));
        });
        return paths;
    };

    auto select_all_action = GUI::CommonActions::make_select_all_action([&](auto&) {
        directory_view->current_view().select_all();
    });

    auto cut_action = GUI::CommonActions::make_cut_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();

            if (paths.is_empty())
                paths = tree_view_selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileOperation::Move);
            refresh_tree_view();
        },
        window);
    cut_action->set_enabled(false);

    auto copy_action = GUI::CommonActions::make_copy_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();

            if (paths.is_empty())
                paths = tree_view_selected_file_paths();

            if (paths.is_empty())
                VERIFY_NOT_REACHED();

            do_copy(paths, FileOperation::Copy);
            refresh_tree_view();
        },
        window);
    copy_action->set_enabled(false);

    auto open_in_new_window_action
        = GUI::Action::create(
            "Open in New &Window",
            {},
            Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-file-manager.png").release_value_but_fixme_should_propagate_errors(),
            [&](GUI::Action const& action) {
                Vector<String> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view->selected_file_paths();

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
            Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-terminal.png").release_value_but_fixme_should_propagate_errors(),
            [&](GUI::Action const& action) {
                Vector<String> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view->selected_file_paths();

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
            Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-symlink.png").release_value_but_fixme_should_propagate_errors(),
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty()) {
                    return;
                }
                do_create_link(paths, directory_view->window());
            },
            window);

    auto create_archive_action
        = GUI::Action::create(
            "Create &Archive",
            Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-archive.png").release_value_but_fixme_should_propagate_errors(),
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_create_archive(paths, directory_view->window());
                refresh_tree_view();
            },
            window);

    auto unzip_archive_action
        = GUI::Action::create(
            "E&xtract Here",
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_unzip_archive(paths, directory_view->window());
                refresh_tree_view();
            },
            window);

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto& action) {
            String container_dir_path;
            String path;
            Vector<String> selected;
            if (action.activator() == directory_context_menu || directory_view->active_widget()->is_focused()) {
                path = directory_view->path();
                container_dir_path = path;
                selected = directory_view->selected_file_paths();
            } else {
                path = directories_model->full_path(tree_view.selection().first());
                container_dir_path = LexicalPath::basename(path);
                selected = tree_view_selected_file_paths();
            }

            show_properties(container_dir_path, path, selected, directory_view->window());
        },
        window);

    auto paste_action = GUI::CommonActions::make_paste_action(
        [&](GUI::Action const& action) {
            String target_directory;
            if (action.activator() == directory_context_menu)
                target_directory = directory_view->selected_file_paths()[0];
            else
                target_directory = directory_view->path();
            do_paste(target_directory, directory_view->window());
            refresh_tree_view();
        },
        window);

    auto folder_specific_paste_action = GUI::CommonActions::make_paste_action(
        [&](GUI::Action const& action) {
            String target_directory;
            if (action.activator() == directory_context_menu)
                target_directory = directory_view->selected_file_paths()[0];
            else
                target_directory = directory_view->path();
            do_paste(target_directory, directory_view->window());
            refresh_tree_view();
        },
        window);

    auto go_back_action = GUI::CommonActions::make_go_back_action(
        [&](auto&) {
            directory_view->open_previous_directory();
        },
        window);

    auto go_forward_action = GUI::CommonActions::make_go_forward_action(
        [&](auto&) {
            directory_view->open_next_directory();
        },
        window);

    auto go_home_action = GUI::CommonActions::make_go_home_action(
        [&](auto&) {
            directory_view->open(Core::StandardPaths::home_directory());
        },
        window);

    GUI::Clipboard::the().on_change = [&](String const& data_type) {
        auto current_location = directory_view->path();
        paste_action->set_enabled(data_type == "text/uri-list" && access(current_location.characters(), W_OK) == 0);
    };

    auto tree_view_delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            delete_paths(tree_view_selected_file_paths(), true, window);
            refresh_tree_view();
        },
        &tree_view);

    // This is a little awkward. The menu action does something different depending on which view has focus.
    // It would be nice to find a good abstraction for this instead of creating a branching action like this.
    auto focus_dependent_delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        if (tree_view.is_focused())
            tree_view_delete_action->activate();
        else
            directory_view->delete_action().activate();
        refresh_tree_view();
    });
    focus_dependent_delete_action->set_enabled(false);

    auto mkdir_action = GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/mkdir.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        directory_view->mkdir_action().activate();
        refresh_tree_view();
    });

    auto touch_action = GUI::Action::create("New &File...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png").release_value_but_fixme_should_propagate_errors(), [&](GUI::Action const&) {
        directory_view->touch_action().activate();
        refresh_tree_view();
    });

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(mkdir_action));
    TRY(file_menu->try_add_action(touch_action));
    TRY(file_menu->try_add_action(focus_dependent_delete_action));
    TRY(file_menu->try_add_action(directory_view->rename_action()));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(properties_action));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto edit_menu = TRY(window->try_add_menu("&Edit"));
    TRY(edit_menu->try_add_action(cut_action));
    TRY(edit_menu->try_add_action(copy_action));
    TRY(edit_menu->try_add_action(paste_action));
    TRY(edit_menu->try_add_separator());
    TRY(edit_menu->try_add_action(select_all_action));

    auto action_show_dotfiles = GUI::Action::create_checkable("&Show Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& action) {
        directory_view->set_should_show_dotfiles(action.is_checked());
        directories_model->set_should_show_dotfiles(action.is_checked());
        refresh_tree_view();
        Config::write_bool("FileManager", "DirectoryView", "ShowDotFiles", action.is_checked());
    });

    auto show_dotfiles = Config::read_bool("FileManager", "DirectoryView", "ShowDotFiles", false);
    directory_view->set_should_show_dotfiles(show_dotfiles);
    action_show_dotfiles->set_checked(show_dotfiles);

    auto const initial_location_contains_dotfile = initial_location.contains("/."sv);
    action_show_dotfiles->set_checked(initial_location_contains_dotfile);
    action_show_dotfiles->on_activation(action_show_dotfiles);

    auto view_menu = TRY(window->try_add_menu("&View"));
    auto layout_menu = TRY(view_menu->try_add_submenu("&Layout"));
    TRY(layout_menu->try_add_action(*layout_toolbar_action));
    TRY(layout_menu->try_add_action(*layout_location_action));
    TRY(layout_menu->try_add_action(*layout_statusbar_action));
    TRY(layout_menu->try_add_action(*layout_folderpane_action));

    TRY(view_menu->try_add_separator());

    TRY(view_menu->try_add_action(directory_view->view_as_icons_action()));
    TRY(view_menu->try_add_action(directory_view->view_as_table_action()));
    TRY(view_menu->try_add_action(directory_view->view_as_columns_action()));
    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(action_show_dotfiles));

    auto go_to_location_action = GUI::Action::create("Go to &Location...", { Mod_Ctrl, Key_L }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-to.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        toolbar_container.set_visible(true);
        location_toolbar.set_visible(true);
        breadcrumb_toolbar.set_visible(false);
        location_textbox.select_all();
        location_textbox.set_focus(true);
    });

    auto go_menu = TRY(window->try_add_menu("&Go"));
    TRY(go_menu->try_add_action(go_back_action));
    TRY(go_menu->try_add_action(go_forward_action));
    TRY(go_menu->try_add_action(open_parent_directory_action));
    TRY(go_menu->try_add_action(go_home_action));
    TRY(go_menu->try_add_action(go_to_location_action));
    TRY(go_menu->try_add_separator());
    TRY(go_menu->try_add_action(directory_view->open_terminal_action()));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("File Manager", GUI::Icon::default_icon("app-file-manager"), window)));

    (void)TRY(main_toolbar.try_add_action(go_back_action));
    (void)TRY(main_toolbar.try_add_action(go_forward_action));
    (void)TRY(main_toolbar.try_add_action(open_parent_directory_action));
    (void)TRY(main_toolbar.try_add_action(go_home_action));

    TRY(main_toolbar.try_add_separator());
    (void)TRY(main_toolbar.try_add_action(directory_view->open_terminal_action()));

    TRY(main_toolbar.try_add_separator());
    (void)TRY(main_toolbar.try_add_action(mkdir_action));
    (void)TRY(main_toolbar.try_add_action(touch_action));
    TRY(main_toolbar.try_add_separator());

    (void)TRY(main_toolbar.try_add_action(focus_dependent_delete_action));
    (void)TRY(main_toolbar.try_add_action(directory_view->rename_action()));

    TRY(main_toolbar.try_add_separator());
    (void)TRY(main_toolbar.try_add_action(cut_action));
    (void)TRY(main_toolbar.try_add_action(copy_action));
    (void)TRY(main_toolbar.try_add_action(paste_action));

    TRY(main_toolbar.try_add_separator());
    (void)TRY(main_toolbar.try_add_action(directory_view->view_as_icons_action()));
    (void)TRY(main_toolbar.try_add_action(directory_view->view_as_table_action()));
    (void)TRY(main_toolbar.try_add_action(directory_view->view_as_columns_action()));

    directory_view->on_path_change = [&](String const& new_path, bool can_read_in_path, bool can_write_in_path) {
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
                        directory_view->open(selected_path);
                    } else {
                        dbgln("Breadcrumb path '{}' doesn't exist", selected_path);
                        breadcrumbbar.remove_end_segments(segment_index);
                        auto existing_path_segment = breadcrumbbar.find_segment_with_data(directory_view->path());
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

        mkdir_action->set_enabled(can_write_in_path);
        touch_action->set_enabled(can_write_in_path);
        paste_action->set_enabled(can_write_in_path && GUI::Clipboard::the().fetch_mime_type() == "text/uri-list");
        go_forward_action->set_enabled(directory_view->path_history_position() < directory_view->path_history_size() - 1);
        go_back_action->set_enabled(directory_view->path_history_position() > 0);
        open_parent_directory_action->set_enabled(new_path != "/");
        directory_view->view_as_table_action().set_enabled(can_read_in_path);
        directory_view->view_as_icons_action().set_enabled(can_read_in_path);
        directory_view->view_as_columns_action().set_enabled(can_read_in_path);
    };

    directory_view->on_accepted_drop = [&] {
        refresh_tree_view();
    };

    directory_view->on_status_message = [&](StringView message) {
        statusbar.set_text(message);
    };

    directory_view->on_thumbnail_progress = [&](int done, int total) {
        if (done == total) {
            progressbar.set_visible(false);
            return;
        }
        progressbar.set_range(0, total);
        progressbar.set_value(done);
        progressbar.set_visible(true);
    };

    directory_view->on_selection_change = [&](GUI::AbstractView& view) {
        auto& selection = view.selection();
        cut_action->set_enabled(!selection.is_empty());
        copy_action->set_enabled(!selection.is_empty());
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && tree_view.is_focused())
            || !directory_view->current_view().selection().is_empty());
    };

    auto directory_open_action = GUI::Action::create("Open", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        directory_view->open(directory_view->selected_file_paths().first());
    });

    TRY(directory_context_menu->try_add_action(directory_open_action));
    TRY(directory_context_menu->try_add_action(open_in_new_window_action));
    TRY(directory_context_menu->try_add_action(open_in_new_terminal_action));
    TRY(directory_context_menu->try_add_separator());
    TRY(directory_context_menu->try_add_action(cut_action));
    TRY(directory_context_menu->try_add_action(copy_action));
    TRY(directory_context_menu->try_add_action(folder_specific_paste_action));
    TRY(directory_context_menu->try_add_action(directory_view->delete_action()));
    TRY(directory_context_menu->try_add_action(directory_view->rename_action()));
    TRY(directory_context_menu->try_add_action(shortcut_action));
    TRY(directory_context_menu->try_add_action(create_archive_action));
    TRY(directory_context_menu->try_add_separator());
    TRY(directory_context_menu->try_add_action(properties_action));

    TRY(directory_view_context_menu->try_add_action(mkdir_action));
    TRY(directory_view_context_menu->try_add_action(touch_action));
    TRY(directory_view_context_menu->try_add_action(paste_action));
    TRY(directory_view_context_menu->try_add_action(directory_view->open_terminal_action()));
    TRY(directory_view_context_menu->try_add_separator());
    TRY(directory_view_context_menu->try_add_action(action_show_dotfiles));
    TRY(directory_view_context_menu->try_add_separator());
    TRY(directory_view_context_menu->try_add_action(properties_action));

    TRY(tree_view_directory_context_menu->try_add_action(open_in_new_window_action));
    TRY(tree_view_directory_context_menu->try_add_action(open_in_new_terminal_action));
    TRY(tree_view_directory_context_menu->try_add_separator());
    TRY(tree_view_directory_context_menu->try_add_action(mkdir_action));
    TRY(tree_view_directory_context_menu->try_add_action(touch_action));
    TRY(tree_view_directory_context_menu->try_add_action(cut_action));
    TRY(tree_view_directory_context_menu->try_add_action(copy_action));
    TRY(tree_view_directory_context_menu->try_add_action(paste_action));
    TRY(tree_view_directory_context_menu->try_add_action(tree_view_delete_action));
    TRY(tree_view_directory_context_menu->try_add_separator());
    TRY(tree_view_directory_context_menu->try_add_action(properties_action));

    RefPtr<GUI::Menu> file_context_menu;
    NonnullRefPtrVector<LauncherHandler> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            auto& node = directory_view->node(index);

            if (node.is_directory()) {
                auto should_get_enabled = access(node.full_path().characters(), W_OK) == 0 && GUI::Clipboard::the().fetch_mime_type() == "text/uri-list";
                folder_specific_paste_action->set_enabled(should_get_enabled);
                directory_context_menu->popup(event.screen_position(), directory_open_action);
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File");

                bool added_launch_file_handlers = add_launch_handler_actions_to_menu(file_context_menu, directory_view, node.full_path(), file_context_menu_action_default_action, current_file_handlers);
                if (added_launch_file_handlers)
                    file_context_menu->add_separator();

                file_context_menu->add_action(cut_action);
                file_context_menu->add_action(copy_action);
                file_context_menu->add_action(paste_action);
                file_context_menu->add_action(directory_view->delete_action());
                file_context_menu->add_action(directory_view->rename_action());
                file_context_menu->add_action(shortcut_action);
                file_context_menu->add_action(create_archive_action);
                file_context_menu->add_separator();

                if (node.full_path().ends_with(".zip", AK::CaseSensitivity::CaseInsensitive)) {
                    file_context_menu->add_action(unzip_archive_action);
                    file_context_menu->add_separator();
                }

                file_context_menu->add_action(properties_action);
                file_context_menu->popup(event.screen_position(), file_context_menu_action_default_action);
            }
        } else {
            directory_view_context_menu->popup(event.screen_position());
        }
    };

    tree_view.on_selection_change = [&] {
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && tree_view.is_focused())
            || !directory_view->current_view().selection().is_empty());

        if (tree_view.selection().is_empty())
            return;

        if (directories_model->m_previously_selected_index.is_valid())
            directories_model->update_node_on_selection(directories_model->m_previously_selected_index, false);

        auto const& index = tree_view.selection().first();
        directories_model->update_node_on_selection(index, true);
        directories_model->m_previously_selected_index = index;

        auto path = directories_model->full_path(index);
        if (directory_view->path() == path)
            return;
        TemporaryChange change(is_reacting_to_tree_view_selection_change, true);
        directory_view->open(path);
        cut_action->set_enabled(!tree_view.selection().is_empty());
        copy_action->set_enabled(!tree_view.selection().is_empty());
        directory_view->delete_action().set_enabled(!tree_view.selection().is_empty());
    };

    tree_view.on_focus_change = [&](bool has_focus, [[maybe_unused]] GUI::FocusSource const source) {
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && has_focus)
            || !directory_view->current_view().selection().is_empty());
    };

    tree_view.on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            tree_view_directory_context_menu->popup(event.screen_position());
        }
    };

    auto copy_urls_to_directory = [&](Vector<URL> const& urls, String const& directory) {
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
                auto error_message = String::formatted("Could not copy {} into {}:\n {}", url_to_copy.to_string(), new_path, static_cast<Error const&>(result.error()));
                GUI::MessageBox::show(window, error_message, "File Manager", GUI::MessageBox::Type::Error);
            } else {
                had_accepted_copy = true;
            }
        }
        if (had_accepted_copy)
            refresh_tree_view();
    };

    breadcrumbbar.on_segment_drop = [&](size_t segment_index, GUI::DropEvent const& event) {
        if (!event.mime_data().has_urls())
            return;
        copy_urls_to_directory(event.mime_data().urls(), breadcrumbbar.segment_data(segment_index));
    };

    breadcrumbbar.on_segment_drag_enter = [&](size_t, GUI::DragEvent& event) {
        if (event.mime_types().contains_slow("text/uri-list"))
            event.accept();
    };

    breadcrumbbar.on_doubleclick = [&](GUI::MouseEvent const&) {
        go_to_location_action->activate();
    };

    tree_view.on_drop = [&](GUI::ModelIndex const& index, GUI::DropEvent const& event) {
        if (!event.mime_data().has_urls())
            return;
        auto& target_node = directories_model->node(index);
        if (!target_node.is_directory())
            return;
        copy_urls_to_directory(event.mime_data().urls(), target_node.full_path());
        const_cast<GUI::DropEvent&>(event).accept();
    };

    directory_view->open(initial_location);
    directory_view->set_focus(true);

    paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "text/uri-list" && access(initial_location.characters(), W_OK) == 0);

    window->set_rect({ left, top, width, height });
    if (was_maximized)
        window->set_maximized(true);

    window->show();

    directory_view->set_view_mode_from_string(Config::read_string("FileManager", "DirectoryView", "ViewMode", "Icon"));

    if (!entry_focused_on_init.is_empty()) {
        auto matches = directory_view->current_view().model()->matches(entry_focused_on_init, GUI::Model::MatchesFlag::MatchFull | GUI::Model::MatchesFlag::FirstMatchOnly);
        if (!matches.is_empty())
            directory_view->current_view().set_cursor(matches.first(), GUI::AbstractView::SelectionUpdate::Set);
    }

    // Write window position to config file on close request.
    window->on_close_request = [&] {
        Config::write_bool("FileManager", "Window", "Maximized", window->is_maximized());
        if (!window->is_maximized()) {
            Config::write_i32("FileManager", "Window", "Left", window->x());
            Config::write_i32("FileManager", "Window", "Top", window->y());
            Config::write_i32("FileManager", "Window", "Width", window->width());
            Config::write_i32("FileManager", "Window", "Height", window->height());
        }
        return GUI::Window::CloseRequestDecision::Close;
    };

    return GUI::Application::the()->exec();
}
