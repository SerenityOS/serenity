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
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <profile-file>\n", argv[0]);
        return 0;
    }

    const char* path = argv[1];
    auto profile = Profile::load_from_perfcore_file(path);

    if (!profile) {
        fprintf(stderr, "Unable to load profile '%s'\n", path);
        return 1;
    }

    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("ProfileViewer");
    window->set_rect(100, 100, 800, 600);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    main_widget.add<ProfileTimelineWidget>(*profile);

    auto& bottom_splitter = main_widget.add<GUI::VerticalSplitter>();

    auto& tree_view = bottom_splitter.add<GUI::TreeView>();
    tree_view.set_headers_visible(true);
    tree_view.set_size_columns_to_fit_content(true);
    tree_view.set_model(profile->model());

    auto& disassembly_view = bottom_splitter.add<GUI::TableView>();
    disassembly_view.set_size_columns_to_fit_content(true);

    tree_view.on_selection = [&](auto& index) {
        profile->set_disassembly_index(index);
        disassembly_view.set_model(profile->disassembly_model());
    };

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("ProfileViewer");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); }));

    auto& view_menu = menubar->add_menu("View");
    auto invert_action = GUI::Action::create_checkable("Invert tree", { Mod_Ctrl, Key_I }, [&](auto& action) {
        profile->set_inverted(action.is_checked());
    });
    invert_action->set_checked(false);
    view_menu.add_action(invert_action);

    auto percent_action = GUI::Action::create_checkable("Show percentages", { Mod_Ctrl, Key_P }, [&](auto& action) {
        profile->set_show_percentages(action.is_checked());
        tree_view.update();
        disassembly_view.update();
    });
    percent_action->set_checked(false);
    view_menu.add_action(percent_action);

    app.set_menubar(move(menubar));

    window->show();
    return app.exec();
}
