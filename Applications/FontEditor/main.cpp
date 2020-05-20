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

#include "FontEditor.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath accept unix cpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer rpath accept cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The font file for editing.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    RefPtr<Gfx::Font> edited_font;
    if (path == nullptr) {
        path = "/tmp/saved.font";
        edited_font = Gfx::Font::default_font().clone();
    } else {
        edited_font = Gfx::Font::load_from_file(path)->clone();
        if (!edited_font) {
            String message = String::format("Couldn't load font: %s\n", path);
            GUI::MessageBox::show(message, "Font Editor", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
            return 1;
        }
    }

    // Convert 256 char font to 384 char font.
    if (edited_font->type() == Gfx::FontTypes::Default)
        edited_font->set_type(Gfx::FontTypes::LatinExtendedA);

    auto app_icon = GUI::Icon::default_icon("app-font-editor");

    auto window = GUI::Window::construct();
    window->set_title(String::format("%s - Font Editor", path));
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& font_editor_widget = window->set_main_widget<FontEditorWidget>(path, move(edited_font));
    window->set_rect({ 50, 50, font_editor_widget.preferred_width(), font_editor_widget.preferred_height() });

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Font Editor");
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Font Editor", app_icon.bitmap_for_size(32), window);
    }));

    app.set_menubar(move(menubar));

    window->show();

    return app.exec();
}
