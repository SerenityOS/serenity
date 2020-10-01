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

#include "HackStudio.h"
#include "HackStudioWidget.h"
#include "Project.h"
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace HackStudio;

static RefPtr<GUI::Window> s_window;
static RefPtr<HackStudioWidget> s_hack_studio_widget;

static bool make_is_available();
static void update_path_environment_variable();
static String path_to_project(const String& path_argument_absolute_path);
static void open_default_project_file(const String& project_path);

int main(int argc, char** argv)
{
    if (pledge("stdio tty accept rpath cpath wpath shared_buffer proc exec unix fattr thread unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio tty accept rpath cpath wpath shared_buffer proc exec fattr thread unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    s_window = GUI::Window::construct();
    s_window->resize(840, 600);
    s_window->set_title("HackStudio");
    s_window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png"));

    update_path_environment_variable();

    if (!make_is_available())
        GUI::MessageBox::show(s_window, "The 'make' command is not available. You probably want to install the binutils, gcc, and make ports from the root of the Serenity repository.", "Error", GUI::MessageBox::Type::Error);

    const char* path_argument = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path_argument, "Path to a workspace or a file", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto argument_absolute_path = Core::File::real_path_for(path_argument);

    auto menubar = GUI::MenuBar::construct();
    auto project_path = path_to_project(argument_absolute_path);
    s_hack_studio_widget = s_window->set_main_widget<HackStudioWidget>(project_path);

    s_hack_studio_widget->initialize_menubar(menubar);
    app->set_menubar(menubar);

    s_window->show();

    open_default_project_file(argument_absolute_path);
    s_hack_studio_widget->update_actions();

    return app->exec();
}

static bool make_is_available()
{
    pid_t pid;
    const char* argv[] = { "make", "--version", nullptr };
    posix_spawn_file_actions_t action;
    posix_spawn_file_actions_init(&action);
    posix_spawn_file_actions_addopen(&action, STDOUT_FILENO, "/dev/null", O_WRONLY, 0);

    if ((errno = posix_spawnp(&pid, "make", &action, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        return false;
    }
    int wstatus;
    waitpid(pid, &wstatus, 0);
    posix_spawn_file_actions_destroy(&action);
    return WEXITSTATUS(wstatus) == 0;
}

static void update_path_environment_variable()
{
    StringBuilder path;
    path.append(getenv("PATH"));
    if (path.length())
        path.append(":");
    path.append("/bin:/usr/bin:/usr/local/bin");
    setenv("PATH", path.to_string().characters(), true);
}

static String path_to_project(const String& path_argument_absolute_path)
{
    if (path_argument_absolute_path.ends_with(".hsp"))
        return path_argument_absolute_path;
    else
        return "/home/anon/Source/little/little.hsp";
}

static void open_default_project_file(const String& project_path)
{
    if (!project_path.is_empty() && !project_path.ends_with(".hsp"))
        open_file(project_path);
    else
        open_file(s_hack_studio_widget->project().default_file());
}

namespace HackStudio {

GUI::TextEditor& current_editor()
{
    return s_hack_studio_widget->current_editor();
}

void open_file(const String& file_name)
{
    return s_hack_studio_widget->open_file(file_name);
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

String currently_open_file()
{
    if (!s_hack_studio_widget)
        return {};
    return s_hack_studio_widget->currently_open_file();
}

void set_current_editor_wrapper(RefPtr<EditorWrapper> wrapper)
{
    s_hack_studio_widget->set_current_editor_wrapper(wrapper);
}

}
