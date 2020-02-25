/*
 * Copyright (c) 2020, the SerenityOS developers.
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
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>

#include "PowerDialog.h"

struct PowerOption {
    String title;
    Vector<char const*> cmd;
    bool enabled;
    bool default_action;
};

static const Vector<PowerOption> options = {
    { "Shut down", { "/bin/shutdown", "--now", nullptr }, true, true },
    { "Restart", { "/bin/reboot", nullptr }, true, false },
    { "Log out", {}, false, false },
    { "Sleep", {}, false, false },
};

Vector<char const*> PowerDialog::show()
{
    auto rc = PowerDialog::construct()->exec();
    if (rc < 0)
        return {};

    return options[rc].cmd;
}

PowerDialog::PowerDialog()
	: GUI::Dialog(nullptr)
{
    Gfx::Rect rect({ 0, 0, 180, 180 + ((static_cast<int>(options.size()) - 3) * 16) });
    rect.center_within(GUI::Desktop::the().rect());
    set_rect(rect);
    set_resizable(false);
    set_title("SerenityOS");
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/power.png"));

    auto main = GUI::Widget::construct();
    set_main_widget(main);
    main->set_layout(make<GUI::VerticalBoxLayout>());
    main->layout()->set_margins({ 8, 8, 8, 8 });
    main->layout()->set_spacing(8);
    main->set_fill_with_background_color(true);

    auto header = main->add<GUI::Label>();
    header->set_text("What would you like to do?");
    header->set_preferred_size(0, 16);
    header->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    header->set_font(Gfx::Font::default_bold_font());

    int selected = -1;
    for (size_t i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto radio = main->add<GUI::RadioButton>();
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

    auto button_box = main->add<GUI::Widget>();
    button_box->set_layout(make<GUI::HorizontalBoxLayout>());
    button_box->layout()->set_spacing(8);

    auto ok_button = button_box->add<GUI::Button>();
    ok_button->on_click = [this, &selected](auto&) {
        done(selected);
    };
    ok_button->set_text("OK");

    auto cancel_button = button_box->add<GUI::Button>();
    cancel_button->on_click = [this](auto&) {
        done(-1);
    };
    cancel_button->set_text("Cancel");
}

PowerDialog::~PowerDialog()
{
}
