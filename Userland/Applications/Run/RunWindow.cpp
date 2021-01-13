/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
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

#include "RunWindow.h"
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <Applications/Run/RunGML.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Button.h>
#include <LibGUI/Event.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Widget.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

RunWindow::RunWindow()
{
    auto app_icon = GUI::Icon::default_icon("app-run");

    set_title("Run");
    set_icon(app_icon.bitmap_for_size(16));
    resize(345, 140);
    set_resizable(false);
    set_minimizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(run_gml);

    m_icon_image_widget = *main_widget.find_descendant_of_type_named<GUI::ImageWidget>("icon");
    m_icon_image_widget->set_bitmap(app_icon.bitmap_for_size(32));

    m_path_text_box = *main_widget.find_descendant_of_type_named<GUI::TextBox>("path");
    m_path_text_box->on_return_pressed = [this] {
        m_ok_button->click();
    };

    m_ok_button = *main_widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    m_ok_button->on_click = [this](auto) {
        do_run();
    };

    m_cancel_button = *main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_cancel_button->on_click = [this](auto) {
        close();
    };

    m_browse_button = *find_descendant_of_type_named<GUI::Button>("browse_button");
    m_browse_button->on_click = [this](auto) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(this);
        if (path.has_value())
            m_path_text_box->set_text(path.value().view());
    };
}

RunWindow::~RunWindow()
{
}

void RunWindow::event(Core::Event& event)
{
    if (event.type() == GUI::Event::KeyUp || event.type() == GUI::Event::KeyDown) {
        auto& key_event = static_cast<GUI::KeyEvent&>(event);
        if (key_event.key() == Key_Escape) {
            // Escape key pressed, close dialog
            close();
            return;
        }
    }

    Window::event(event);
}

void RunWindow::do_run()
{
    auto run_input = m_path_text_box->text();

    hide();

    if (run_via_launch(run_input) || run_as_command(run_input)) {
        close();
        return;
    }

    GUI::MessageBox::show_error(this, "Failed to run. Please check your command, path, or address, and try again.");

    show();
}

bool RunWindow::run_as_command(const String& run_input)
{
    pid_t child_pid;
    const char* shell_executable = "/bin/Shell"; // TODO query and use the user's preferred shell.
    const char* argv[] = { shell_executable, "-c", run_input.characters(), nullptr };

    if ((errno = posix_spawn(&child_pid, shell_executable, nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        return false;
    }

    // Command spawned in child shell. Hide and wait for exit code.
    int status;
    if (waitpid(child_pid, &status, 0) < 0)
        return false;

    int child_error = WEXITSTATUS(status);
    dbgln("Child shell exited with code {}", child_error);

    // 127 is typically the shell indicating command not found. 126 for all other errors.
    if (child_error == 126 || child_error == 127) {
        return false;
    }

    dbgln("Ran via command shell.");

    return true;
}

bool RunWindow::run_via_launch(const String& run_input)
{
    auto url = URL::create_with_url_or_path(run_input);

    if (url.protocol() == "file") {
        auto real_path = Core::File::real_path_for(url.path());
        if (real_path.is_null()) {
            // errno *should* be preserved from Core::File::real_path_for().
            warnln("Failed to launch '{}': {}", url.path(), strerror(errno));
            return false;
        }
        url = URL::create_with_url_or_path(real_path);
    }

    if (!Desktop::Launcher::open(url)) {
        warnln("Failed to launch '{}'", url);
        return false;
    }

    dbgln("Ran via URL launch.");

    return true;
}
