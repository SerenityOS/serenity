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

#include <AK/ByteBuffer.h>
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto f = Core::File::construct();
    URL url;
    bool success;
    if (argc < 2) {
        success = f->open(STDIN_FILENO, Core::IODevice::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescription::No);
    } else {
        url = URL::create_with_file_protocol(argv[1]);
        f->set_filename(argv[1]);
        success = f->open(Core::IODevice::OpenMode::ReadOnly);
    }
    if (!success) {
        fprintf(stderr, "Error: %s\n", f->error_string());
        return 1;
    }

    auto html = f->read_all();

    auto window = GUI::Window::construct();
    window->set_title("HTML");
    auto& widget = window->set_main_widget<Web::OutOfProcessWebView>();
    widget.on_title_change = [&](auto& title) {
        window->set_title(String::formatted("{} - HTML", title));
    };
    widget.load_html(html, url);
    window->show();

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("HTML");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("HTML", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window);
    }));

    app->set_menubar(move(menubar));

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

    return app->exec();
}
