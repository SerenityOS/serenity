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

#include "BoardListModel.h"
#include "ThreadCatalogModel.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio dns inet shared_buffer rpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio dns inet shared_buffer rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("ChanViewer");
    window->set_rect(100, 100, 800, 500);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-chanviewer.png"));

    auto widget = GUI::Widget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GUI::VerticalBoxLayout>());

    auto board_combo = widget->add<GUI::ComboBox>();
    board_combo->set_only_allow_values_from_model(true);
    board_combo->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    board_combo->set_preferred_size(0, 20);
    board_combo->set_model(BoardListModel::create());

    auto catalog_view = widget->add<GUI::TableView>();
    catalog_view->set_model(ThreadCatalogModel::create());
    auto& catalog_model = *static_cast<ThreadCatalogModel*>(catalog_view->model());

    auto statusbar = widget->add<GUI::StatusBar>();

    board_combo->on_change = [&] (auto&, const GUI::ModelIndex& index) {
        auto selected_board = board_combo->model()->data(index, GUI::Model::Role::Custom);
        ASSERT(selected_board.is_string());
        catalog_model.set_board(selected_board.to_string());
    };

    catalog_model.on_load_started = [&] {
        statusbar->set_text(String::format("Loading /%s/...", catalog_model.board().characters()));
    };

    catalog_model.on_load_finished = [&](bool success) {
        statusbar->set_text(success ? "Load finished" : "Load failed");
        if (success) {
            window->set_title(String::format("/%s/ - ChanViewer", catalog_model.board().characters()));
        }
    };

    window->show();

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("ChanViewer");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("ChanViewer", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-chanviewer.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
