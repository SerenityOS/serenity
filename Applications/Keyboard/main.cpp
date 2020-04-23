/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "CharacterMapFileListModel.h"
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/WindowServerConnection.h>
#include <stdio.h>

#include <LibGUI/MessageBox.h>

int main(int argc, char** argv)
{
    if (pledge("accept cpath rpath exec fattr proc shared_buffer stdio thread unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("accept cpath rpath exec proc shared_buffer stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<String> character_map_files;
    Core::DirIterator iterator("/res/keymaps/", Core::DirIterator::Flags::SkipDots);
    while (iterator.has_next()) {
        auto name = iterator.next_path();
        name.replace(".json", "");
        character_map_files.append(name);
    }
    quick_sort(character_map_files);

    auto window = GUI::Window::construct();
    window->set_title("Keyboard");
    window->set_rect(200, 200, 300, 70);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-keyboard.png"));

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.set_fill_with_background_color(true);
    root_widget.layout()->set_spacing(0);
    root_widget.layout()->set_margins({ 4, 4, 4, 4 });

    auto& character_map_file_selection_container = root_widget.add<GUI::Widget>();
    character_map_file_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    character_map_file_selection_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    character_map_file_selection_container.set_preferred_size(0, 22);

    auto& character_map_filelabel = character_map_file_selection_container.add<GUI::Label>();
    character_map_filelabel.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    character_map_filelabel.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    character_map_filelabel.set_preferred_size({ 70, 0 });
    character_map_filelabel.set_text("Character Mapping File:");

    auto& character_map_file_combo = character_map_file_selection_container.add<GUI::ComboBox>();
    character_map_file_combo.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    character_map_file_combo.set_preferred_size(0, 22);
    character_map_file_combo.set_only_allow_values_from_model(true);
    character_map_file_combo.set_model(*CharacterMapFileListModel<AK::String>::create(character_map_files));

    root_widget.layout()->add_spacer();

    auto apply_settings = [&](bool quit) {
        String character_map_file = character_map_file_combo.text();
        if (character_map_file.is_empty()) {
            GUI::MessageBox::show("Please select cahracter mapping file.", "Keyboard", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }

        auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetCharacterMap>(character_map_file);
        if (!response->success()) {
            GUI::MessageBox::show("The character set could not be changed.", "Keyboard", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }

        if (quit)
            app.quit();
    };

    auto& bottom_widget = root_widget.add<GUI::Widget>();
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.layout()->add_spacer();
    bottom_widget.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    bottom_widget.set_preferred_size(1, 22);

    auto& apply_button = bottom_widget.add<GUI::Button>();
    apply_button.set_text("Apply");
    apply_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    apply_button.set_preferred_size(60, 22);
    apply_button.on_click = [&] {
        apply_settings(false);
    };

    auto& ok_button = bottom_widget.add<GUI::Button>();
    ok_button.set_text("OK");
    ok_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    ok_button.set_preferred_size(60, 22);
    ok_button.on_click = [&] {
        apply_settings(true);
    };

    auto& cancel_button = bottom_widget.add<GUI::Button>();
    cancel_button.set_text("Cancel");
    cancel_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    cancel_button.set_preferred_size(60, 22);
    cancel_button.on_click = [&] {
        app.quit();
    };

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app.quit();
        });

    auto about_action = GUI::Action::create("About",
        [&](auto&) {
            GUI::AboutDialog::show("Keyboard", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-keyboard.png"), window);
        });

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Keyboard");
    app_menu.add_action(quit_action);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(about_action);

    app.set_menubar(move(menubar));

    window->show();

    return app.exec();
}
