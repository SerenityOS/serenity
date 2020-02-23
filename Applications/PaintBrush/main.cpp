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

#include "PaintableWidget.h"
#include "PaletteWidget.h"
#include "ToolboxWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix wpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 640, 480);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-paintbrush.png"));

    auto horizontal_container = GUI::Widget::construct();
    window->set_main_widget(horizontal_container);
    horizontal_container->set_layout(make<GUI::HorizontalBoxLayout>());
    horizontal_container->layout()->set_spacing(0);

    horizontal_container->add<ToolboxWidget>();

    auto vertical_container = horizontal_container->add<GUI::Widget>();
    vertical_container->set_layout(make<GUI::VerticalBoxLayout>());
    vertical_container->layout()->set_spacing(0);

    auto paintable_widget = vertical_container->add<PaintableWidget>();
    paintable_widget->set_focus(true);
    vertical_container->add<PaletteWidget>(*paintable_widget);

    window->show();

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("PaintBrush");

    app_menu->add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        auto bitmap = Gfx::Bitmap::load_from_file(open_path.value());
        if (!bitmap) {
            GUI::MessageBox::show(String::format("Failed to load '%s'", open_path.value().characters()), "Open failed", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }
        paintable_widget->set_bitmap(*bitmap);
    }));
    app_menu->add_separator();
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));

    menubar->add_menu(move(app_menu));

    auto edit_menu = GUI::Menu::construct("Edit");
    menubar->add_menu(move(edit_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("PaintBrush", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-paintbrush.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
