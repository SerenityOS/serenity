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

#include "Profile.h"
#include "ProfileTimelineWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <profile-file>\n", argv[0]);
        return 0;
    }

    const char* path = argv[1];
    OwnPtr<Profile> profile;

    if (!strcmp(path, "perfcore")) {
        profile = Profile::load_from_perfcore_file(path);
    } else {
        profile = Profile::load_from_file(path);
    }

    if (!profile) {
        fprintf(stderr, "Unable to load profile '%s'\n", path);
        return 1;
    }

    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("ProfileViewer");
    window->set_rect(100, 100, 800, 600);

    auto main_widget = GUI::Widget::construct();
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout(make<GUI::VerticalBoxLayout>());

    auto timeline_widget = ProfileTimelineWidget::construct(*profile, main_widget);

    auto tree_view = GUI::TreeView::construct(main_widget);
    tree_view->set_headers_visible(true);
    tree_view->set_size_columns_to_fit_content(true);
    tree_view->set_model(profile->model());

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("ProfileViewer");
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); }));

    menubar->add_menu(move(app_menu));

    auto view_menu = GUI::Menu::construct("View");
    auto invert_action = GUI::Action::create("Invert tree", { Mod_Ctrl, Key_I }, [&](auto& action) {
        action.set_checked(!action.is_checked());
        profile->set_inverted(action.is_checked());
    });
    invert_action->set_checkable(true);
    invert_action->set_checked(false);
    view_menu->add_action(invert_action);

    menubar->add_menu(move(view_menu));

    app.set_menubar(move(menubar));

    window->show();
    return app.exec();
}
