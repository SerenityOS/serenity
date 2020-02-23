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

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/TextAlignment.h>
#include <stdio.h>
#include <sys/utsname.h>

struct DialogOption {
    String title;
    String cmd;
    bool enabled;
    bool default_action;
};

Vector<DialogOption> get_options()
{
    Vector<DialogOption> options;
    auto config = Core::ConfigFile::get_for_system("SystemDialog");
    for (auto title : config->groups()) {
        dbg() << "title = " << title;
        auto command = config->read_entry(title, "command", "");
        dbg() << "\tcommand=" << command;
        auto enabled = config->read_bool_entry(title, "enabled", true);
        dbg() << "\tenabled=" << enabled;
        auto default_entry = config->read_bool_entry(title, "default", false);
        dbg() << "\tdefault=" << default_entry;

        ASSERT(!(command == "" && enabled));

        options.append({ title, command, enabled, default_entry });
    }
    return options;
}

int main(int argc, char** argv)
{
    Vector<DialogOption> options;

    if (pledge("stdio shared_buffer rpath wpath cpath unix fattr exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc/SystemDialog.ini", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Shell", "rx") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer rpath wpath cpath exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    options = get_options();

    if (pledge("stdio shared_buffer rpath exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto dialog = GUI::Dialog::construct(nullptr);
    Gfx::Rect rect({ 0, 0, 180, 180 + ((options.size() - 3) * 16) });
    rect.center_within(GUI::Desktop::the().rect());
    dialog->set_rect(rect);
    dialog->set_resizable(false);
    dialog->set_title("SerenityOS");

    auto main = GUI::Widget::construct();
    dialog->set_main_widget(main);
    main->set_layout(make<GUI::VerticalBoxLayout>());
    main->layout()->set_margins({ 8, 8, 8, 8 });
    main->layout()->set_spacing(8);
    main->set_fill_with_background_color(true);

    auto header = GUI::Label::construct(main.ptr());
    header->set_text("What would you like to do?");
    header->set_preferred_size(0, 16);
    header->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    header->set_font(Gfx::Font::default_bold_font());

    int selected;
    for (int i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto radio = GUI::RadioButton::construct(main);
        radio->set_enabled(action.enabled);
        radio->set_text(action.title);

        radio->on_checked = [&selected, i](auto) {
            selected = i;
        };

        if (action.default_action) {
            radio->set_checked(true);
            selected = i;
        }
    }

    auto button_box = GUI::Widget::construct(main.ptr());
    button_box->set_layout(make<GUI::HorizontalBoxLayout>());
    button_box->layout()->set_spacing(8);

    auto ok_button = GUI::Button::construct(button_box);
    ok_button->on_click = [&](auto&) {
        dialog->done(true);
    };
    ok_button->set_text("OK");

    auto cancel_button = GUI::Button::construct(button_box);
    cancel_button->on_click = [&](auto&) {
        dialog->done(false);
    };
    cancel_button->set_text("Cancel");

    dialog->exec();

    if (pledge("stdio shared_buffer exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!dialog->result())
        return 0;

    // TODO Don't rely on the shell
    auto command = options[selected].cmd.characters();
    dbg() << command;
    if (execl("/bin/Shell", "/bin/Shell", "-c", command, NULL) < 0) {
        perror("execl");
        return 1;
    }
}
