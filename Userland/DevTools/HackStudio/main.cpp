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
#include <LibGUI/Notification.h>
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
static void notify_make_not_available();
static void update_path_environment_variable();

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd tty accept rpath cpath wpath proc exec unix fattr thread unix ptrace", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd tty accept rpath cpath wpath proc exec fattr thread unix ptrace", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    s_window = GUI::Window::construct();
    s_window->resize(840, 600);
    s_window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png"));

    update_path_environment_variable();

    if (!make_is_available()) {
        notify_make_not_available();
    }

    const char* path_argument = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path_argument, "Path to a workspace or a file", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto argument_absolute_path = Core::File::real_path_for(path_argument);

    auto project_path = argument_absolute_path;
    if (argument_absolute_path.is_null())
        project_path = Core::File::real_path_for(".");

    s_hack_studio_widget = s_window->set_main_widget<HackStudioWidget>(project_path);

    s_window->set_title(String::formatted("{} - Hack Studio", s_hack_studio_widget->project().name()));

    auto menubar = GUI::MenuBar::construct();
    s_hack_studio_widget->initialize_menubar(menubar);
    app->set_menubar(menubar);

    s_window->show();

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

static void notify_make_not_available()
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"));
    notification->set_title("'make' Not Available");
    notification->set_text("You probably want to install the binutils, gcc, and make ports from the root of the Serenity repository");
    notification->show();
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
