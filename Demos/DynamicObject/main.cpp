/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "lib.h"
#include <LibCore/Command.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/internals.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv, [[maybe_unused]] char** env)
{
    printf("Well Hello Friends!\n");
    printf("trying to open /etc/fstab for writing..\n");
    int rc = open("/etc/fstab", O_RDWR);
    if (rc == -1) {
        int _errno = errno;
        perror("open failed");
        printf("rc: %d, errno: %d\n", rc, _errno);
    }
    printf("ls: %s\n", Core::command("ls", {}).characters());
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->resize(240, 160);
    window->set_title("Hello World!");
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hello-world.png"));

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_background_color(Color::White);
    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 4, 4, 4, 4 });

    auto& label = main_widget.add<GUI::Label>();
    label.set_text("Hello\nWorld!");

    auto& button = main_widget.add<GUI::Button>();
    button.set_text("Good-bye");
    button.on_click = [&](auto) {
        app->quit();
    };

    window->show();

    return app->exec();
    // return func() + g_tls1 + g_tls2;
}
