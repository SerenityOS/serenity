/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Editor.h"
#include "HackStudio.h"
#include "HackStudioWidget.h"
#include "Project.h"
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Notification.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace HackStudio;

static WeakPtr<HackStudioWidget> s_hack_studio_widget;

static bool make_is_available();
static ErrorOr<void> notify_make_not_available();
static void update_path_environment_variable();
static Optional<ByteString> last_opened_project_path();
static ErrorOr<NonnullRefPtr<HackStudioWidget>> create_hack_studio_widget(bool mode_coredump, StringView path, pid_t pid_to_debug);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd tty rpath cpath wpath proc exec unix fattr thread ptrace"));

    auto app = TRY(GUI::Application::create(arguments));
    app->set_config_domain("HackStudio"_string);
    Config::enable_permissive_mode();
    Config::pledge_domains({ "HackStudio", "Terminal", "FileManager" });

    auto window = GUI::Window::construct();
    window->restore_size_and_position("HackStudio"sv, "Window"sv, { { 840, 600 } });
    window->save_size_and_position_on_close("HackStudio"sv, "Window"sv);
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png"sv));
    window->set_icon(icon);

    update_path_environment_variable();

    if (!make_is_available()) {
        TRY(notify_make_not_available());
    }

    StringView path_argument;
    bool mode_coredump = false;
    pid_t pid_to_debug = -1;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path_argument, "Path to a workspace or a file", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(mode_coredump, "Debug a coredump in HackStudio", "coredump", 'c');
    args_parser.add_option(pid_to_debug, "Attach debugger to running process", "pid", 'p', "PID");
    args_parser.parse(arguments);

    auto hack_studio_widget = TRY(create_hack_studio_widget(mode_coredump, path_argument, pid_to_debug));
    window->set_main_widget(hack_studio_widget);
    s_hack_studio_widget = hack_studio_widget;

    window->set_title(ByteString::formatted("{} - Hack Studio", hack_studio_widget->project().name()));

    TRY(hack_studio_widget->initialize_menubar(*window));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        hack_studio_widget->locator().close();
        if (hack_studio_widget->warn_unsaved_changes("There are unsaved changes, do you want to save before exiting?") == HackStudioWidget::ContinueDecision::Yes)
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();
    hack_studio_widget->update_actions();

    if (mode_coredump)
        hack_studio_widget->open_coredump(path_argument);

    if (pid_to_debug != -1)
        hack_studio_widget->debug_process(pid_to_debug);

    return app->exec();
}

static bool make_is_available()
{
    auto maybe_process = Core::Process::spawn({
        .executable = "make",
        .search_for_executable_in_path = true,
        .arguments = { "--version" },
        .file_actions = { Core::FileAction::OpenFile {
            .path = "/dev/null"sv,
            .mode = Core::File::OpenMode::Write,
            .fd = STDOUT_FILENO,
        } },
    });
    if (maybe_process.is_error()) {
        warnln("Failed to spawn make: {}", maybe_process.release_error());
        return false;
    }
    auto process = maybe_process.release_value();

    auto maybe_result = process.wait_for_termination();
    if (maybe_result.is_error()) {
        warnln("Error running make: {}", maybe_result.release_error());
        return false;
    }

    return maybe_result.value();
}

static ErrorOr<void> notify_make_not_available()
{
    auto notification = GUI::Notification::construct();
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"sv));
    notification->set_icon(icon);
    notification->set_title("'make' Not Available"_string);
    notification->set_text("You probably want to install the binutils, gcc, and make ports from the root of the Serenity repository"_string);
    notification->show();
    return {};
}

static void update_path_environment_variable()
{
    StringBuilder path;

    auto const* path_env_ptr = getenv("PATH");
    if (path_env_ptr != NULL)
        path.append({ path_env_ptr, strlen(path_env_ptr) });

    if (path.length())
        path.append(':');
    path.append(DEFAULT_PATH_SV);
    setenv("PATH", path.to_byte_string().characters(), true);
}

static Optional<ByteString> last_opened_project_path()
{
    auto projects = HackStudioWidget::read_recent_projects();
    if (projects.size() == 0)
        return {};

    if (!FileSystem::exists(projects[0]))
        return {};

    return { projects[0] };
}

namespace HackStudio {

GUI::TextEditor& current_editor()
{
    return s_hack_studio_widget->current_editor();
}

void open_file(ByteString const& filename)
{
    s_hack_studio_widget->open_file(filename);
}

void open_file(ByteString const& filename, size_t line, size_t column)
{
    s_hack_studio_widget->open_file(filename, line, column);
}

RefPtr<EditorWrapper> current_editor_wrapper()
{
    if (!s_hack_studio_widget)
        return nullptr;
    return s_hack_studio_widget->current_editor_wrapper();
}

Project& project()
{
    return s_hack_studio_widget->project();
}

ByteString currently_open_file()
{
    if (!s_hack_studio_widget)
        return {};
    return s_hack_studio_widget->active_file();
}

void set_current_editor_wrapper(RefPtr<EditorWrapper> wrapper)
{
    s_hack_studio_widget->set_current_editor_wrapper(wrapper);
}

void update_editor_window_title()
{
    s_hack_studio_widget->update_current_editor_title();
    s_hack_studio_widget->update_window_title();
}

Locator& locator()
{
    return s_hack_studio_widget->locator();
}

void for_each_open_file(Function<void(ProjectFile const&)> func)
{
    s_hack_studio_widget->for_each_open_file(move(func));
}

bool semantic_syntax_highlighting_is_enabled()
{
    return s_hack_studio_widget->semantic_syntax_highlighting_is_enabled();
}

}

static ErrorOr<NonnullRefPtr<HackStudioWidget>> create_hack_studio_widget(bool mode_coredump, StringView raw_path_argument, pid_t pid_to_debug)
{
    ByteString project_path;
    if (pid_to_debug != -1 || mode_coredump)
        project_path = "/usr/src/serenity";
    else if (!raw_path_argument.is_null())
        project_path = raw_path_argument;
    else if (auto last_path = last_opened_project_path(); last_path.has_value())
        project_path = last_path.release_value();
    else
        project_path = TRY(FileSystem::real_path("."sv));

    return HackStudioWidget::create(project_path);
}
