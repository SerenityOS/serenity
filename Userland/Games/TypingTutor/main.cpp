/*
 * Copyright (c) 2021, Davide Carella <carelladavide1@gmail.com>.
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

#include "TypingTutorWidget.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

struct WordlistData {
    String name;
    String path;
};

Vector<WordlistData> g_wordlists;
RefPtr<TypingTutorCanvasWidget> g_canvas;
GUI::ActionGroup g_languages_group;

static void load_wordlists(GUI::Menu&);

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-typingtutor");

    if (pledge("stdio recvfd sendfd rpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_resizable(false);
    window->set_title("Typing Tutor");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto widget = TypingTutorWidget::construct();
    g_canvas = widget->canvas_widget();

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Type Tutor");

    app_menu.add_action(GUI::Action::create("New game", { Mod_Ctrl, Key_N }, [&](auto&) {
        widget->reset();
    }));

    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));
    app_menu.add_separator();

    load_wordlists(app_menu);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Type Tutor", app_icon, window));

    app->set_menubar(move(menubar));
    window->set_main_widget(widget);
    window->show();

    return app->exec();
}

void load_wordlists(GUI::Menu& menu)
{
    Core::DirIterator dt("/res/wordlists", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto name = dt.next_path();
        auto path = String::formatted("/res/wordlists/{}", name);
        g_wordlists.append({ LexicalPath(name).title(), path });
    }
    quick_sort(g_wordlists, [](auto& a, auto& b) { return a.name < b.name; });

    auto& languages_menu = menu.add_submenu("Languages");
    g_languages_group.set_exclusive(true);

    size_t i;
    for (i = 0; i < g_wordlists.size(); ++i) {
        auto action = GUI::Action::create_checkable(g_wordlists[i].name, [i](auto&) {
            g_canvas->set_wordlist_path(g_wordlists[i].path);
        });

        if (g_wordlists[i].path == g_canvas->wordlist_path())
            action->set_checked(true);

        languages_menu.add_action(action);
        g_languages_group.add_action(*action);
    }
}
