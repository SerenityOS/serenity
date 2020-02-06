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

#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto f = Core::File::construct();
    bool success;
    if (argc < 2) {
        success = f->open(STDIN_FILENO, Core::IODevice::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescription::No);
    } else {
        f->set_filename(argv[1]);
        success = f->open(Core::IODevice::OpenMode::ReadOnly);
    }
    if (!success) {
        fprintf(stderr, "Error: %s\n", f->error_string());
        return 1;
    }

    String html = String::copy(f->read_all());
    auto document = parse_html_document(html);

    auto window = GUI::Window::construct();
    auto widget = HtmlView::construct();
    widget->set_document(document);
    if (!widget->document()->title().is_null())
        window->set_title(String::format("%s - HTML", widget->document()->title().characters()));
    else
        window->set_title("HTML");
    window->set_main_widget(widget);
    window->show();

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("HTML");
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("HTML", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

    return app.exec();
}
