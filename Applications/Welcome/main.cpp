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

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <stdio.h>
#include <unistd.h>

#include "BackgroundWidget.h"
#include "TextWidget.h"
#include "UnuncheckableButton.h"

struct ContentPage {
    String menu_name;
    String title;
    String icon = String::empty();
    Vector<String> content;
};

Optional<Vector<ContentPage>> parse_welcome_file(const String& path)
{
    const auto error = Optional<Vector<ContentPage>>();
    auto file = Core::File::construct(path);

    if (!file->open(Core::IODevice::ReadOnly))
        return error;

    Vector<ContentPage> pages;
    StringBuilder current_output_line;
    bool started = false;
    ContentPage current;
    while (true) {
        auto buffer = file->read_line(4096);
        if (buffer.is_null()) {
            if (file->error()) {
                file->close();
                return error;
            }

            break;
        }

        auto line = String((char*)buffer.data());
        if (line.length() > 1)
            line = line.substring(0, line.length() - 1); // remove newline
        switch (line[0]) {
        case '*':
            dbg() << "menu_item line:\t" << line;
            if (started)
                pages.append(current);
            else
                started = true;

            current = {};
            current.menu_name = line.substring(2, line.length() - 2);
            break;
        case '$':
            dbg() << "icon line: \t" << line;
            current.icon = line.substring(2, line.length() - 2);
            break;
        case '>':
            dbg() << "title line:\t" << line;
            current.title = line.substring(2, line.length() - 2);
            break;
        case '\n':
            dbg() << "newline";

            if (!current_output_line.to_string().is_empty())
                current.content.append(current_output_line.to_string());
            current_output_line.clear();
            break;
        case '#':
            dbg() << "comment line:\t" << line;
            break;
        default:
            dbg() << "content line:\t" << line;
            if (current_output_line.length() != 0)
                current_output_line.append(' ');
            current_output_line.append(line);
            break;
        }
    }

    if (started) {
        current.content.append(current_output_line.to_string());
        pages.append(current);
    }

    file->close();
    return pages;
}

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    Optional<Vector<ContentPage>> _pages = parse_welcome_file("/res/welcome.txt");
    if (!_pages.has_value()) {
        GUI::MessageBox::show("Could not open Welcome file.", "Welcome", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, nullptr);
        return 1;
    }
    auto pages = _pages.value();

    auto window = GUI::Window::construct();
    window->set_title("Welcome");
    Gfx::Rect window_rect { 0, 0, 640, 360 };
    window_rect.center_within(GUI::Desktop::the().rect());
    window->set_resizable(true);
    window->set_rect(window_rect);

    auto& background = window->set_main_widget<BackgroundWidget>();
    background.set_fill_with_background_color(false);
    background.set_layout<GUI::VerticalBoxLayout>();
    background.layout()->set_margins({ 16, 8, 16, 8 });
    background.layout()->set_spacing(8);
    background.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    //
    // header
    //

    auto header = background.add<GUI::Label>();
    header->set_font(Gfx::Font::load_from_file("/res/fonts/PebbletonBold11.font"));
    header->set_text("Welcome to SerenityOS!");
    header->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    header->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    header->set_preferred_size(0, 30);

    //
    // main section
    //

    auto main_section = background.add<GUI::Widget>();
    main_section->set_layout<GUI::HorizontalBoxLayout>();
    main_section->layout()->set_margins({ 0, 0, 0, 0 });
    main_section->layout()->set_spacing(8);
    main_section->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    auto menu = main_section->add<GUI::Widget>();
    menu->set_layout<GUI::VerticalBoxLayout>();
    menu->layout()->set_margins({ 0, 0, 0, 0 });
    menu->layout()->set_spacing(4);
    menu->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    menu->set_preferred_size(100, 0);

    auto stack = main_section->add<GUI::StackWidget>();
    stack->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    bool first = true;
    for (auto& page : pages) {
        auto content = stack->add<GUI::Widget>();
        content->set_layout<GUI::VerticalBoxLayout>();
        content->layout()->set_margins({ 0, 0, 0, 0 });
        content->layout()->set_spacing(8);
        content->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

        auto title_box = content->add<GUI::Widget>();
        title_box->set_layout<GUI::HorizontalBoxLayout>();
        title_box->layout()->set_spacing(4);
        title_box->set_preferred_size(0, 16);
        title_box->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);

        if (!page.icon.is_empty()) {
            auto icon = title_box->add<GUI::Label>();
            icon->set_icon(Gfx::Bitmap::load_from_file(page.icon));
            icon->set_preferred_size(16, 16);
            icon->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        }

        auto content_title = title_box->add<GUI::Label>();
        content_title->set_font(Gfx::Font::default_bold_font());
        content_title->set_text(page.title);
        content_title->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        content_title->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        content_title->set_preferred_size(0, 10);

        for (auto& paragraph : page.content) {
            auto content_text = content->add<TextWidget>();
            content_text->set_font(Gfx::Font::default_font());
            content_text->set_text(paragraph);
            content_text->set_text_alignment(Gfx::TextAlignment::TopLeft);
            content_text->set_line_height(12);
            content_text->wrap_and_set_height();
        }

        auto menu_option = menu->add<UnuncheckableButton>();
        menu_option->set_font(Gfx::Font::default_font());
        menu_option->set_text(page.menu_name);
        menu_option->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        menu_option->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        menu_option->set_preferred_size(0, 20);
        menu_option->set_checkable(true);
        menu_option->set_exclusive(true);

        if (first)
            menu_option->set_checked(true);

        menu_option->on_click = [content = content.ptr(), &stack] {
            stack->set_active_widget(content);
            content->invalidate_layout();
        };

        first = false;
    }

    window->show();
    return app.exec();
}
