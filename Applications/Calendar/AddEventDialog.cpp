/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
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

#include "AddEventDialog.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>

AddEventDialog::AddEventDialog(Calendar* calendar, Window* parent_window)
    : Dialog(parent_window)
    , m_calendar(calendar)
{
    resize(230, 120);
    set_title("Add Event");
    set_resizable(false);

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::HorizontalBoxLayout>();

    auto& main_container = widget.add<GUI::Widget>();
    main_container.set_layout<GUI::VerticalBoxLayout>();
    main_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto make_label = [&](const StringView& text, bool bold = false) {
        auto& label = main_container.add<GUI::Label>(text);
        label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        label.set_preferred_size(0, 14);
        if (bold)
            label.set_font(Gfx::Font::default_bold_font());
    };
    make_label("TODO: Implement add event dialog", true);

    main_container.layout()->add_spacer();

    auto& button_container = main_container.add<GUI::Widget>();
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_container.set_preferred_size(0, 20);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->add_spacer();
    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    ok_button.set_preferred_size(80, 20);
    ok_button.on_click = [this] {
        done(Dialog::ExecOK);
    };
}

AddEventDialog::~AddEventDialog()
{
}
