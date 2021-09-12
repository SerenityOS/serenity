/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/Window.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();
    window->set_title("DumpLayoutTree");
    window->resize(800, 600);
    window->show();
    auto& web_view = window->set_main_widget<Web::OutOfProcessWebView>();
    web_view.load(URL::create_with_file_protocol(argv[1]));
    web_view.on_load_finish = [&](auto&) {
        auto dump = web_view.dump_layout_tree();
        write(STDOUT_FILENO, dump.characters(), dump.length() + 1);
        _exit(0);
    };
    return app->exec();
}
