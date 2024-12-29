/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopWidget.h"
#include "DirectoryView.h"
#include "FileUtils.h"
#include "PropertiesWindow.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <Applications/FileManager/FileManagerWindowGML.h>
#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Process.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/PathBreadcrumbbar.h>
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
#include <LibURL/URL.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace FileManager;

static ErrorOr<int> run_in_desktop_mode();
static ErrorOr<int> run_in_windowed_mode(ByteString const& initial_location, ByteString const& entry_focused_on_init);
static void do_copy(Vector<ByteString> const& selected_file_paths, FileOperation file_operation);
static void do_paste(ByteString const& target_directory, GUI::Window* window);
static void do_create_link(Vector<ByteString> const& selected_file_paths, GUI::Window* window);
static void do_create_archive(Vector<ByteString> const& selected_file_paths, GUI::Window* window);
static void do_set_wallpaper(ByteString const& file_path, GUI::Window* window);
static void do_unzip_archive(Vector<ByteString> const& selected_file_paths, GUI::Window* window);
static void show_properties(ByteString const& container_dir_path, ByteString const& path, Vector<ByteString> const& selected, GUI::Window* window);
static bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, DirectoryView const& directory_view, ByteString const& full_path, RefPtr<GUI::Action>& default_action, Vector<NonnullRefPtr<LauncherHandler>>& current_file_launch_handlers);

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
    ByteString initial_location;
    args_parser.add_option(is_desktop_mode, "Run in desktop mode", "desktop", 'd');
    args_parser.add_option(is_selection_mode, "Show entry in parent folder", "select", 's');
    args_parser.add_option(ignore_path_resolution, "Use raw path, do not resolve real path", "raw", 'r');
    args_parser.add_positional_argument(initial_location, "Path to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio thread recvfd sendfd cpath rpath wpath fattr proc exec unix"));

    Config::pledge_domains({ "FileManager", "WindowManager", "Maps" });
    Config::monitor_domain("FileManager");
    Config::monitor_domain("WindowManager");

    if (is_desktop_mode)
        return run_in_desktop_mode();

    // our initial location is defined as, in order of precedence:
    // 1. the command-line path argument (e.g. FileManager /bin)
    // 2. the current directory
    // 3. the user's home directory
    // 4. the root directory

    LexicalPath path(initial_location);
    if (!initial_location.is_empty()) {
        if (auto error_or_path = FileSystem::real_path(initial_location); !ignore_path_resolution && !error_or_path.is_error())
            initial_location = error_or_path.release_value();

        if (!FileSystem::is_directory(initial_location)) {
            // We want to extract zips to a temporary directory when FileManager is launched with a .zip file as its first argument
            if (path.has_extension(".zip"sv)) {
                auto temp_directory = FileSystem::TempFile::create_temp_directory();
                if (temp_directory.is_error()) {
                    dbgln("Failed to create temporary directory during zip extraction: {}", temp_directory.error());

                    GUI::MessageBox::show_error(app->active_window(), "Failed to create temporary directory!"sv);
                    return -1;
                }

                auto temp_directory_path = temp_directory.value()->path();
                auto result = Core::Process::spawn("/bin/unzip"sv, Array { "-d"sv, temp_directory_path, initial_location });

                if (result.is_error()) {
                    dbgln("Failed to extract {} to {}: {}", initial_location, temp_directory_path, result.error());

                    auto message = TRY(String::formatted("Failed to extract {} to {}", initial_location, temp_directory_path));
                    GUI::MessageBox::show_error(app->active_window(), message);

                    return -1;
                }

                return run_in_windowed_mode(temp_directory_path.to_byte_string(), path.basename());
            }

            is_selection_mode = true;
        }
    }

    if (auto error_or_cwd = FileSystem::current_working_directory(); initial_location.is_empty() && !error_or_cwd.is_error())
        initial_location = error_or_cwd.release_value();

    if (initial_location.is_empty())
        initial_location = Core::StandardPaths::home_directory();

    if (initial_location.is_empty())
        initial_location = "/";

    ByteString focused_entry;
    if (is_selection_mode) {
        initial_location = path.dirname();
        focused_entry = path.basename();
    }

    return run_in_windowed_mode(initial_location, focused_entry);
}

void do_copy(Vector<ByteString> const& selected_file_paths, FileOperation file_operation)
{
    VERIFY(!selected_file_paths.is_empty());

    StringBuilder copy_text;
    if (file_operation == FileOperation::Move) {
        copy_text.append("#cut\n"sv); // This exploits the comment lines in the text/uri-list specification, which might be a bit hackish
    }
    for (auto& path : selected_file_paths) {
        auto url = URL::create_with_file_scheme(path);
        copy_text.appendff("{}\n", url);
    }
    GUI::Clipboard::the().set_data(copy_text.string_view().bytes(), "text/uri-list");
}

void do_paste(ByteString const& target_directory, GUI::Window* window)
{
    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    if (data_and_type.mime_type != "text/uri-list") {
        dbgln("Cannot paste clipboard type {}", data_and_type.mime_type);
        return;
    }
    auto copied_lines = ByteString::copy(data_and_type.data).split('\n');
    if (copied_lines.is_empty()) {
        dbgln("No files to paste");
        return;
    }

    FileOperation file_operation = FileOperation::Copy;
    if (copied_lines[0] == "#cut") { // cut operation encoded as a text/uri-list comment
        file_operation = FileOperation::Move;
        copied_lines.remove(0);
    }

    Vector<ByteString> source_paths;
    for (auto& uri_as_string : copied_lines) {
        if (uri_as_string.is_empty())
            continue;
        URL::URL url = uri_as_string;
        if (!url.is_valid() || url.scheme() != "file") {
            dbgln("Cannot paste URI {}", uri_as_string);
            continue;
        }
        source_paths.append(URL::percent_decode(url.serialize_path()));
    }

    if (!source_paths.is_empty()) {
        if (auto result = run_file_operation(file_operation, source_paths, target_directory, window); result.is_error())
            dbgln("Failed to paste files: {}", result.error());
    }
}

void do_create_link(Vector<ByteString> const& selected_file_paths, GUI::Window* window)
{
    auto path = selected_file_paths.first();
    auto destination = ByteString::formatted("{}/{}", Core::StandardPaths::desktop_directory(), LexicalPath::basename(path));
    if (auto result = FileSystem::link_file(destination, path); result.is_error()) {
        GUI::MessageBox::show(window, ByteString::formatted("Could not create desktop shortcut:\n{}", result.error()), "File Manager"sv,
            GUI::MessageBox::Type::Error);
    }
}

void do_create_archive(Vector<ByteString> const& selected_file_paths, GUI::Window* window)
{
    String archive_name;
    if (GUI::InputBox::show(window, archive_name, "Enter name:"sv, "Create Archive"sv) != GUI::InputBox::ExecResult::OK)
        return;

    auto output_directory_path = LexicalPath(selected_file_paths.first());

    StringBuilder path_builder;
    path_builder.append(output_directory_path.dirname());
    path_builder.append('/');
    if (archive_name.is_empty()) {
        path_builder.append(output_directory_path.parent().basename());
        path_builder.append(".zip"sv);
    } else {
        path_builder.append(archive_name);
        if (!AK::StringUtils::ends_with(archive_name, ".zip"sv, CaseSensitivity::CaseSensitive))
            path_builder.append(".zip"sv);
    }
    auto output_path = path_builder.to_byte_string();

    pid_t zip_pid = fork();
    if (zip_pid < 0) {
        perror("fork");
        VERIFY_NOT_REACHED();
    }

    if (!zip_pid) {
        Vector<ByteString> relative_paths;
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
            GUI::MessageBox::show(window, "Could not create archive"sv, "Archive Error"sv, GUI::MessageBox::Type::Error);
    }
}

void do_set_wallpaper(ByteString const& file_path, GUI::Window* window)
{
    auto show_error = [&] {
        GUI::MessageBox::show(window, ByteString::formatted("Failed to set {} as wallpaper.", file_path), "Failed to set wallpaper"sv, GUI::MessageBox::Type::Error);
    };

    auto bitmap_or_error = Gfx::Bitmap::load_from_file(file_path);
    if (bitmap_or_error.is_error()) {
        show_error();
        return;
    }

    if (!GUI::Desktop::the().set_wallpaper(bitmap_or_error.release_value(), file_path))
        show_error();
}

void do_unzip_archive(Vector<ByteString> const& selected_file_paths, GUI::Window* window)
{
    ByteString archive_file_path = selected_file_paths.first();
    ByteString output_directory_path = archive_file_path.substring(0, archive_file_path.length() - 4);

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
            GUI::MessageBox::show(window, "Could not extract archive"sv, "Extract Archive Error"sv, GUI::MessageBox::Type::Error);
    }
}

void show_properties(ByteString const& container_dir_path, ByteString const& path, Vector<ByteString> const& selected, GUI::Window* window)
{
    ErrorOr<RefPtr<PropertiesWindow>> properties_or_error = nullptr;
    if (selected.is_empty()) {
        properties_or_error = window->try_add<PropertiesWindow>(path, true);
    } else {
        properties_or_error = window->try_add<PropertiesWindow>(selected.first(), access(container_dir_path.characters(), W_OK) != 0);
    }

    if (properties_or_error.is_error()) {
        GUI::MessageBox::show(window, "Could not show properties"sv, "Properties Error"sv, GUI::MessageBox::Type::Error);
        return;
    }

    auto properties = properties_or_error.release_value();
    properties->on_close = [properties = properties.ptr()] {
        properties->remove_from_parent();
    };
    properties->center_on_screen();
    properties->show();
}

bool add_launch_handler_actions_to_menu(RefPtr<GUI::Menu>& menu, DirectoryView const& directory_view, ByteString const& full_path, RefPtr<GUI::Action>& default_action, Vector<NonnullRefPtr<LauncherHandler>>& current_file_launch_handlers)
{
    current_file_launch_handlers = directory_view.get_launch_handlers(full_path);

    bool added_open_menu_items = false;
    auto default_file_handler = directory_view.get_default_launch_handler(current_file_launch_handlers);
    if (default_file_handler) {
        auto file_open_action = default_file_handler->create_launch_action([&, full_path = move(full_path)](auto& launcher_handler) {
            directory_view.launch(URL::create_with_file_scheme(full_path), launcher_handler);
        });
        if (default_file_handler->details().launcher_type == Desktop::Launcher::LauncherType::Application)
            file_open_action->set_text(ByteString::formatted("Run {}", file_open_action->text()));
        else
            file_open_action->set_text(ByteString::formatted("Open in {}", file_open_action->text()));

        default_action = file_open_action;

        menu->add_action(move(file_open_action));
        added_open_menu_items = true;
    } else {
        default_action.clear();
    }

    if (current_file_launch_handlers.size() > 1) {
        added_open_menu_items = true;
        auto file_open_with_menu = menu->add_submenu("Open with"_string);
        for (auto& handler : current_file_launch_handlers) {
            if (handler == default_file_handler)
                continue;
            file_open_with_menu->add_action(handler->create_launch_action([&, full_path = move(full_path)](auto& launcher_handler) {
                directory_view.launch(URL::create_with_file_scheme(full_path), launcher_handler);
            }));
        }
    }

    return added_open_menu_items;
}

ErrorOr<int> run_in_desktop_mode()
{
    (void)Core::Process::set_name("FileManager (Desktop)"sv, Core::Process::SetThreadName::Yes);

    auto window = GUI::Window::construct();
    window->set_title("Desktop Manager");
    window->set_window_type(GUI::WindowType::Desktop);
    window->set_has_alpha_channel(true);

    auto desktop_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/desktop.png"sv));
    window->set_icon(desktop_icon);

    auto desktop_widget = window->set_main_widget<FileManager::DesktopWidget>();
    desktop_widget->set_layout<GUI::VerticalBoxLayout>();

    auto directory_view = TRY(desktop_widget->try_add<DirectoryView>(DirectoryView::Mode::Desktop));
    directory_view->set_name("directory_view");

    auto cut_action = GUI::CommonActions::make_cut_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();
            VERIFY(!paths.is_empty());

            do_copy(paths, FileOperation::Move);
        },
        window);
    cut_action->set_enabled(false);

    auto copy_action = GUI::CommonActions::make_copy_action(
        [&](auto&) {
            auto paths = directory_view->selected_file_paths();
            VERIFY(!paths.is_empty());

            do_copy(paths, FileOperation::Copy);
        },
        window);
    copy_action->set_enabled(false);

    auto create_archive_action
        = GUI::Action::create(
            "Create &Archive",
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-archive.png"sv)),
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

    auto set_wallpaper_action
        = GUI::Action::create(
            "Set as Desktop &Wallpaper",
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-settings.png"sv)),
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_set_wallpaper(paths.first(), directory_view->window());
            },
            window);

    directory_view->on_selection_change = [&](GUI::AbstractView const& view) {
        cut_action->set_enabled(!view.selection().is_empty());
        copy_action->set_enabled(!view.selection().is_empty());
    };

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto&) {
            ByteString path = directory_view->path();
            Vector<ByteString> selected = directory_view->selected_file_paths();

            show_properties(path, path, selected, directory_view->window());
        },
        window);

    auto paste_action = GUI::CommonActions::make_paste_action(
        [&](GUI::Action const&) {
            do_paste(directory_view->path(), directory_view->window());
        },
        window);
    paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "text/uri-list" && access(directory_view->path().characters(), W_OK) == 0);

    GUI::Clipboard::the().on_change = [&](ByteString const& data_type) {
        paste_action->set_enabled(data_type == "text/uri-list" && access(directory_view->path().characters(), W_OK) == 0);
    };

    auto desktop_view_context_menu = GUI::Menu::construct("Directory View"_string);

    auto file_manager_action = GUI::Action::create("Open in File &Manager", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"sv)), [&](auto&) {
        auto paths = directory_view->selected_file_paths();
        if (paths.is_empty()) {
            Desktop::Launcher::open(URL::create_with_file_scheme(directory_view->path()));
            return;
        }

        for (auto& path : paths) {
            if (FileSystem::is_directory(path))
                Desktop::Launcher::open(URL::create_with_file_scheme(path));
        }
    });

    auto open_terminal_action = GUI::Action::create("Open in &Terminal", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"sv)), [&](auto&) {
        auto paths = directory_view->selected_file_paths();
        if (paths.is_empty()) {
            spawn_terminal(window, directory_view->path());
            return;
        }

        for (auto& path : paths) {
            if (FileSystem::is_directory(path)) {
                spawn_terminal(window, path);
            }
        }
    });

    auto display_properties_action = GUI::Action::create("&Display Settings", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-settings.png"sv)), [&](GUI::Action const&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/bin/DisplaySettings"));
    });

    desktop_view_context_menu->add_action(directory_view->mkdir_action());
    desktop_view_context_menu->add_action(directory_view->touch_action());
    desktop_view_context_menu->add_action(paste_action);
    desktop_view_context_menu->add_separator();
    desktop_view_context_menu->add_action(file_manager_action);
    desktop_view_context_menu->add_action(open_terminal_action);
    desktop_view_context_menu->add_separator();
    desktop_view_context_menu->add_action(display_properties_action);

    auto desktop_context_menu = GUI::Menu::construct("Directory View Directory"_string);

    desktop_context_menu->add_action(file_manager_action);
    desktop_context_menu->add_action(open_terminal_action);
    desktop_context_menu->add_separator();
    desktop_context_menu->add_action(cut_action);
    desktop_context_menu->add_action(copy_action);
    desktop_context_menu->add_action(paste_action);
    desktop_context_menu->add_action(directory_view->delete_action());
    desktop_context_menu->add_action(directory_view->rename_action());
    desktop_context_menu->add_separator();
    desktop_context_menu->add_action(properties_action);

    RefPtr<GUI::Menu> file_context_menu;
    Vector<NonnullRefPtr<LauncherHandler>> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            auto& node = directory_view->node(index);
            if (node.is_directory()) {
                desktop_context_menu->popup(event.screen_position(), file_manager_action);
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File"_string);

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

                if (Gfx::Bitmap::is_path_a_supported_image_format(node.name)) {
                    file_context_menu->add_action(set_wallpaper_action);
                    file_context_menu->add_separator();
                }

                if (node.full_path().ends_with(".zip"sv, AK::CaseSensitivity::CaseInsensitive)) {
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
        virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override
        {
            if (domain == "WindowManager" && group == "Background" && key == "Wallpaper") {
                if (value.is_empty()) {
                    GUI::Desktop::the().set_wallpaper(nullptr, {});
                    return;
                }
                auto wallpaper_bitmap_or_error = Gfx::Bitmap::load_from_file(value);
                if (wallpaper_bitmap_or_error.is_error())
                    dbgln("Failed to load wallpaper bitmap from path: {}", wallpaper_bitmap_or_error.error());
                else
                    GUI::Desktop::the().set_wallpaper(wallpaper_bitmap_or_error.release_value(), {});
            }
        }
    } wallpaper_listener;

    auto selected_wallpaper = Config::read_string("WindowManager"sv, "Background"sv, "Wallpaper"sv, ""sv);
    RefPtr<Gfx::Bitmap> wallpaper_bitmap {};
    if (!selected_wallpaper.is_empty()) {
        wallpaper_bitmap = TRY(Gfx::Bitmap::load_from_file(selected_wallpaper));
    }
    // This sets the wallpaper at startup, even if there is no wallpaper, the
    // desktop should still show the background color. It's fine to pass a
    // nullptr to Desktop::set_wallpaper.
    GUI::Desktop::the().set_wallpaper(wallpaper_bitmap, {});

    window->show();
    return GUI::Application::the()->exec();
}

ErrorOr<int> run_in_windowed_mode(ByteString const& initial_location, ByteString const& entry_focused_on_init)
{
    auto window = GUI::Window::construct();
    window->set_title("File Manager");

    auto widget = window->set_main_widget<GUI::Widget>();
    TRY(widget->load_from_gml(file_manager_window_gml));

    auto& toolbar_container = *widget->find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    auto& main_toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("main_toolbar");

    auto& breadcrumb_toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("breadcrumb_toolbar");
    breadcrumb_toolbar.layout()->set_margins({ 0, 6 });
    auto& breadcrumbbar = *widget->find_descendant_of_type_named<GUI::PathBreadcrumbbar>("breadcrumbbar");

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

    // Open the root directory. FIXME: This is awkward.
    tree_view.toggle_index(directories_model->index(0, 0, {}));

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };

    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    auto& progressbar = *widget->find_descendant_of_type_named<GUI::Progressbar>("progressbar");
    progressbar.set_format(GUI::Progressbar::Format::ValueSlashMax);
    progressbar.set_frame_style(Gfx::FrameStyle::SunkenPanel);

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

    auto directory_context_menu = GUI::Menu::construct("Directory View Directory"_string);
    auto directory_view_context_menu = GUI::Menu::construct("Directory View"_string);
    auto tree_view_directory_context_menu = GUI::Menu::construct("Tree View Directory"_string);

    auto open_parent_directory_action = GUI::Action::create("Open &Parent Directory", { Mod_Alt, Key_Up }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"sv)), [&](GUI::Action const&) {
        directory_view->open_parent_directory();
    });

    auto open_child_directory_action = GUI::Action::create("Open &Child Directory", { Mod_Alt, Key_Down }, [&](GUI::Action const&) {
        breadcrumbbar.select_child_segment();
    });

    RefPtr<GUI::Action> layout_toolbar_action;
    RefPtr<GUI::Action> layout_location_action;
    RefPtr<GUI::Action> layout_statusbar_action;
    RefPtr<GUI::Action> layout_folderpane_action;

    auto show_toolbar = Config::read_bool("FileManager"sv, "Layout"sv, "ShowToolbar"sv, true);
    layout_toolbar_action = GUI::Action::create_checkable("&Toolbar", [&](auto& action) {
        if (action.is_checked()) {
            main_toolbar.set_visible(true);
            toolbar_container.set_visible(true);
        } else {
            main_toolbar.set_visible(false);
            if (!breadcrumb_toolbar.is_visible())
                toolbar_container.set_visible(false);
        }
        show_toolbar = action.is_checked();
        Config::write_bool("FileManager"sv, "Layout"sv, "ShowToolbar"sv, action.is_checked());
    });
    layout_toolbar_action->set_checked(show_toolbar);
    main_toolbar.set_visible(show_toolbar);

    auto show_location = Config::read_bool("FileManager"sv, "Layout"sv, "ShowLocationBar"sv, true);
    layout_location_action = GUI::Action::create_checkable("&Location Bar", [&](auto& action) {
        if (action.is_checked()) {
            breadcrumb_toolbar.set_visible(true);
            toolbar_container.set_visible(true);
        } else {
            breadcrumb_toolbar.set_visible(false);
            if (!main_toolbar.is_visible())
                toolbar_container.set_visible(false);
        }
        show_location = action.is_checked();
        Config::write_bool("FileManager"sv, "Layout"sv, "ShowLocationBar"sv, action.is_checked());
    });
    layout_location_action->set_checked(show_location);
    breadcrumb_toolbar.set_visible(show_location);

    toolbar_container.set_visible(show_location || show_toolbar);

    layout_statusbar_action = GUI::Action::create_checkable("&Status Bar", [&](auto& action) {
        action.is_checked() ? statusbar.set_visible(true) : statusbar.set_visible(false);
        Config::write_bool("FileManager"sv, "Layout"sv, "ShowStatusbar"sv, action.is_checked());
    });

    auto show_statusbar = Config::read_bool("FileManager"sv, "Layout"sv, "ShowStatusbar"sv, true);
    layout_statusbar_action->set_checked(show_statusbar);
    statusbar.set_visible(show_statusbar);

    layout_folderpane_action = GUI::Action::create_checkable("&Folder Pane", { Mod_Ctrl, Key_P }, [&](auto& action) {
        action.is_checked() ? tree_view.set_visible(true) : tree_view.set_visible(false);
        Config::write_bool("FileManager"sv, "Layout"sv, "ShowFolderPane"sv, action.is_checked());
    });

    auto show_folderpane = Config::read_bool("FileManager"sv, "Layout"sv, "ShowFolderPane"sv, true);
    layout_folderpane_action->set_checked(show_folderpane);
    tree_view.set_visible(show_folderpane);

    breadcrumbbar.on_hide_location_box = [&] {
        if (show_location)
            breadcrumb_toolbar.set_visible(true);
        if (!(show_location || show_toolbar))
            toolbar_container.set_visible(false);
    };

    auto view_type_action_group = make<GUI::ActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(directory_view->view_as_icons_action());
    view_type_action_group->add_action(directory_view->view_as_table_action());
    view_type_action_group->add_action(directory_view->view_as_columns_action());

    auto tree_view_selected_file_paths = [&] {
        Vector<ByteString> paths;
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
            VERIFY(!paths.is_empty());

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
            VERIFY(!paths.is_empty());

            do_copy(paths, FileOperation::Copy);
            refresh_tree_view();
        },
        window);
    copy_action->set_enabled(false);

    auto copy_path_action = GUI::Action::create(
        "Copy Path", [&](GUI::Action const&) {
            Vector<ByteString> selected_paths;
            if (directory_view->active_widget()->is_focused()) {
                selected_paths = directory_view->selected_file_paths();
            } else if (tree_view.is_focused()) {
                selected_paths = tree_view_selected_file_paths();
            }
            VERIFY(!selected_paths.is_empty());

            StringBuilder joined_paths_builder;
            joined_paths_builder.join('\n', selected_paths.span());
            GUI::Clipboard::the().set_plain_text(joined_paths_builder.string_view());
        },
        window);

    auto open_in_new_window_action
        = GUI::Action::create(
            "Open in New &Window",
            {},
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"sv)),
            [&](GUI::Action const& action) {
                Vector<ByteString> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view->selected_file_paths();

                for (auto& path : paths) {
                    if (FileSystem::is_directory(path))
                        Desktop::Launcher::open(URL::create_with_file_scheme(path));
                }
            },
            window);

    auto open_in_new_terminal_action
        = GUI::Action::create(
            "Open in &Terminal",
            {},
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"sv)),
            [&](GUI::Action const& action) {
                Vector<ByteString> paths;
                if (action.activator() == tree_view_directory_context_menu)
                    paths = tree_view_selected_file_paths();
                else
                    paths = directory_view->selected_file_paths();

                for (auto& path : paths) {
                    if (FileSystem::is_directory(path)) {
                        spawn_terminal(window, path);
                    }
                }
            },
            window);

    auto shortcut_action
        = GUI::Action::create(
            "Create Desktop &Shortcut",
            {},
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-symlink.png"sv)),
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
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-archive.png"sv)),
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

    auto set_wallpaper_action
        = GUI::Action::create(
            "Set as Desktop &Wallpaper",
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-settings.png"sv)),
            [&](GUI::Action const&) {
                auto paths = directory_view->selected_file_paths();
                if (paths.is_empty())
                    return;

                do_set_wallpaper(paths.first(), directory_view->window());
            },
            window);

    auto properties_action = GUI::CommonActions::make_properties_action(
        [&](auto& action) {
            ByteString container_dir_path;
            ByteString path;
            Vector<ByteString> selected;
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
            ByteString target_directory;
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
            ByteString target_directory;
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

    GUI::Clipboard::the().on_change = [&](ByteString const& data_type) {
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

    auto new_window_action = GUI::Action::create("&New Window", { Mod_Ctrl, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new-window.png"sv)), [&](GUI::Action const&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(directory_view->path()));
    });

    auto mkdir_action = GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"sv)), [&](GUI::Action const&) {
        directory_view->mkdir_action().activate();
        refresh_tree_view();
    });

    auto touch_action = GUI::Action::create("New &File...", { Mod_Ctrl | Mod_Shift, Key_F }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv)), [&](GUI::Action const&) {
        directory_view->touch_action().activate();
        refresh_tree_view();
    });

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(new_window_action);
    file_menu->add_action(mkdir_action);
    file_menu->add_action(touch_action);
    file_menu->add_action(focus_dependent_delete_action);
    file_menu->add_action(directory_view->rename_action());
    file_menu->add_separator();
    file_menu->add_action(properties_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto edit_menu = window->add_menu("&Edit"_string);
    edit_menu->add_action(cut_action);
    edit_menu->add_action(copy_action);
    edit_menu->add_action(paste_action);
    edit_menu->add_separator();
    edit_menu->add_action(select_all_action);

    auto show_dotfiles_in_view = [&](bool show_dotfiles) {
        directory_view->set_should_show_dotfiles(show_dotfiles);
        directories_model->set_should_show_dotfiles(show_dotfiles);
    };

    auto show_dotfiles_action = GUI::Action::create_checkable("&Show Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& action) {
        show_dotfiles_in_view(action.is_checked());
        refresh_tree_view();
        Config::write_bool("FileManager"sv, "DirectoryView"sv, "ShowDotFiles"sv, action.is_checked());
    });

    auto show_dotfiles = Config::read_bool("FileManager"sv, "DirectoryView"sv, "ShowDotFiles"sv, false);
    show_dotfiles |= initial_location.contains("/."sv);
    show_dotfiles_action->set_checked(show_dotfiles);
    show_dotfiles_in_view(show_dotfiles);

    auto view_menu = window->add_menu("&View"_string);
    auto layout_menu = view_menu->add_submenu("&Layout"_string);
    layout_menu->add_action(*layout_toolbar_action);
    layout_menu->add_action(*layout_location_action);
    layout_menu->add_action(*layout_statusbar_action);
    layout_menu->add_action(*layout_folderpane_action);

    view_menu->add_separator();

    view_menu->add_action(directory_view->view_as_icons_action());
    view_menu->add_action(directory_view->view_as_table_action());
    view_menu->add_action(directory_view->view_as_columns_action());
    view_menu->add_separator();
    view_menu->add_action(show_dotfiles_action);

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto go_to_location_action = GUI::Action::create("Go to &Location...", { Mod_Ctrl, Key_L }, Key_F6, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-to.png"sv)), [&](auto&) {
        toolbar_container.set_visible(true);
        breadcrumb_toolbar.set_visible(true);
        breadcrumbbar.show_location_text_box();
    });

    auto go_menu = window->add_menu("&Go"_string);
    go_menu->add_action(go_back_action);
    go_menu->add_action(go_forward_action);
    go_menu->add_action(open_parent_directory_action);
    go_menu->add_action(open_child_directory_action);
    go_menu->add_action(go_home_action);
    go_menu->add_action(go_to_location_action);
    go_menu->add_separator();
    go_menu->add_action(directory_view->open_terminal_action());

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("File Manager"_string, GUI::Icon::default_icon("app-file-manager"sv), window));

    main_toolbar.add_action(go_back_action);
    main_toolbar.add_action(go_forward_action);
    main_toolbar.add_action(open_parent_directory_action);
    main_toolbar.add_action(go_home_action);

    main_toolbar.add_separator();
    main_toolbar.add_action(directory_view->open_terminal_action());

    main_toolbar.add_separator();
    main_toolbar.add_action(mkdir_action);
    main_toolbar.add_action(touch_action);
    main_toolbar.add_separator();

    main_toolbar.add_action(focus_dependent_delete_action);
    main_toolbar.add_action(directory_view->rename_action());

    main_toolbar.add_separator();
    main_toolbar.add_action(cut_action);
    main_toolbar.add_action(copy_action);
    main_toolbar.add_action(paste_action);

    main_toolbar.add_separator();
    main_toolbar.add_action(directory_view->view_as_icons_action());
    main_toolbar.add_action(directory_view->view_as_table_action());
    main_toolbar.add_action(directory_view->view_as_columns_action());

    breadcrumbbar.on_path_change = [&](auto selected_path) {
        if (FileSystem::is_directory(selected_path)) {
            directory_view->open(selected_path);
        } else {
            dbgln("Breadcrumb path '{}' doesn't exist", selected_path);
            breadcrumbbar.set_current_path(directory_view->path());
        }
    };

    directory_view->on_path_change = [&](ByteString const& new_path, bool can_read_in_path, bool can_write_in_path) {
        auto icon = GUI::FileIconProvider::icon_for_path(new_path);
        auto* bitmap = icon.bitmap_for_size(16);
        window->set_icon(bitmap);

        window->set_title(ByteString::formatted("{} - File Manager", new_path));

        breadcrumbbar.set_current_path(new_path);

        if (!is_reacting_to_tree_view_selection_change) {
            auto new_index = directories_model->index(new_path, GUI::FileSystemModel::Column::Name);
            if (new_index.is_valid()) {
                tree_view.expand_all_parents_of(new_index);
                tree_view.set_cursor(new_index, GUI::AbstractView::SelectionUpdate::Set);
            }
        }

        mkdir_action->set_enabled(can_write_in_path);
        touch_action->set_enabled(can_write_in_path);
        paste_action->set_enabled(can_write_in_path && GUI::Clipboard::the().fetch_mime_type() == "text/uri-list");
        go_forward_action->set_enabled(directory_view->path_history_position() < directory_view->path_history_size() - 1);
        go_back_action->set_enabled(directory_view->path_history_position() > 0);
        open_parent_directory_action->set_enabled(breadcrumbbar.has_parent_segment());
        open_child_directory_action->set_enabled(breadcrumbbar.has_child_segment());
        directory_view->view_as_table_action().set_enabled(can_read_in_path);
        directory_view->view_as_icons_action().set_enabled(can_read_in_path);
        directory_view->view_as_columns_action().set_enabled(can_read_in_path);
    };

    directory_view->on_accepted_drop = [&] {
        refresh_tree_view();
    };

    directory_view->on_status_message = [&](StringView message) {
        statusbar.set_text(String::from_utf8(message).release_value_but_fixme_should_propagate_errors());
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
        cut_action->set_enabled(!selection.is_empty() && access(directory_view->path().characters(), W_OK) == 0);
        copy_action->set_enabled(!selection.is_empty());
        copy_path_action->set_text(selection.size() > 1 ? "Copy Paths" : "Copy Path");
        focus_dependent_delete_action->set_enabled((!tree_view.selection().is_empty() && tree_view.is_focused())
            || (!directory_view->current_view().selection().is_empty() && access(directory_view->path().characters(), W_OK) == 0));
    };

    auto directory_open_action = GUI::Action::create("Open", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv)), [&](auto&) {
        directory_view->open(directory_view->selected_file_paths().first());
    });

    directory_context_menu->add_action(directory_open_action);
    directory_context_menu->add_action(open_in_new_window_action);
    directory_context_menu->add_action(open_in_new_terminal_action);
    directory_context_menu->add_separator();
    directory_context_menu->add_action(cut_action);
    directory_context_menu->add_action(copy_action);
    directory_context_menu->add_action(copy_path_action);
    directory_context_menu->add_action(folder_specific_paste_action);
    directory_context_menu->add_action(directory_view->delete_action());
    directory_context_menu->add_action(directory_view->rename_action());
    directory_context_menu->add_action(shortcut_action);
    directory_context_menu->add_action(create_archive_action);
    directory_context_menu->add_separator();
    directory_context_menu->add_action(properties_action);

    directory_view_context_menu->add_action(mkdir_action);
    directory_view_context_menu->add_action(touch_action);
    directory_view_context_menu->add_action(paste_action);
    directory_view_context_menu->add_action(directory_view->open_terminal_action());
    directory_view_context_menu->add_separator();
    directory_view_context_menu->add_action(show_dotfiles_action);
    directory_view_context_menu->add_separator();
    directory_view_context_menu->add_action(properties_action);

    tree_view_directory_context_menu->add_action(open_in_new_window_action);
    tree_view_directory_context_menu->add_action(open_in_new_terminal_action);
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(mkdir_action);
    tree_view_directory_context_menu->add_action(touch_action);
    tree_view_directory_context_menu->add_action(cut_action);
    tree_view_directory_context_menu->add_action(copy_action);
    tree_view_directory_context_menu->add_action(copy_path_action);
    tree_view_directory_context_menu->add_action(paste_action);
    tree_view_directory_context_menu->add_action(tree_view_delete_action);
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(properties_action);

    RefPtr<GUI::Menu> file_context_menu;
    Vector<NonnullRefPtr<LauncherHandler>> current_file_handlers;
    RefPtr<GUI::Action> file_context_menu_action_default_action;

    directory_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            auto& node = directory_view->node(index);

            if (node.is_directory()) {
                auto should_get_enabled = access(node.full_path().characters(), W_OK) == 0 && GUI::Clipboard::the().fetch_mime_type() == "text/uri-list";
                folder_specific_paste_action->set_enabled(should_get_enabled);
                directory_context_menu->popup(event.screen_position(), directory_open_action);
            } else {
                file_context_menu = GUI::Menu::construct("Directory View File"_string);

                bool added_launch_file_handlers = add_launch_handler_actions_to_menu(file_context_menu, directory_view, node.full_path(), file_context_menu_action_default_action, current_file_handlers);
                if (added_launch_file_handlers)
                    file_context_menu->add_separator();

                file_context_menu->add_action(cut_action);
                file_context_menu->add_action(copy_action);
                file_context_menu->add_action(copy_path_action);
                file_context_menu->add_action(paste_action);
                file_context_menu->add_action(directory_view->delete_action());
                file_context_menu->add_action(directory_view->rename_action());
                file_context_menu->add_action(shortcut_action);
                file_context_menu->add_action(create_archive_action);
                file_context_menu->add_separator();

                if (Gfx::Bitmap::is_path_a_supported_image_format(node.name)) {
                    file_context_menu->add_action(set_wallpaper_action);
                    file_context_menu->add_separator();
                }

                if (node.full_path().ends_with(".zip"sv, AK::CaseSensitivity::CaseInsensitive)) {
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

    breadcrumbbar.on_paths_drop = [&](auto path, GUI::DropEvent const& event) {
        bool const has_accepted_drop = handle_drop(event, path, window).release_value_but_fixme_should_propagate_errors();
        if (has_accepted_drop)
            refresh_tree_view();
    };

    tree_view.on_drop = [&](GUI::ModelIndex const& index, GUI::DropEvent const& event) {
        auto const& target_node = directories_model->node(index);
        bool const has_accepted_drop = handle_drop(event, target_node.full_path(), window).release_value_but_fixme_should_propagate_errors();
        if (has_accepted_drop) {
            refresh_tree_view();
            const_cast<GUI::DropEvent&>(event).accept();
        }
    };

    directory_view->open(initial_location);
    directory_view->set_focus(true);

    paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "text/uri-list" && access(initial_location.characters(), W_OK) == 0);

    window->restore_size_and_position("FileManager"sv, "Window"sv, { { 640, 480 } });
    window->save_size_and_position_on_close("FileManager"sv, "Window"sv);

    window->show();

    directory_view->set_view_mode_from_string(Config::read_string("FileManager"sv, "DirectoryView"sv, "ViewMode"sv, "Icon"sv));

    if (!entry_focused_on_init.is_empty()) {
        auto matches = directory_view->current_view().model()->matches(entry_focused_on_init, GUI::Model::MatchesFlag::MatchFull | GUI::Model::MatchesFlag::FirstMatchOnly);
        if (!matches.is_empty())
            directory_view->current_view().set_cursor(matches.first(), GUI::AbstractView::SelectionUpdate::Set);
    }

    return GUI::Application::the()->exec();
}
