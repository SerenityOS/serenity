/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "NotificationWindow.h"
#include <AK/HashTable.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>

namespace NotificationServer {

static HashTable<RefPtr<NotificationWindow>> s_windows;

NotificationWindow::NotificationWindow(const String& text, const String& title)
{
    s_windows.set(this);

    set_window_type(GUI::WindowType::Tooltip);

    Gfx::Rect rect;
    rect.set_width(200);
    rect.set_height(40);
    rect.set_location(GUI::Desktop::the().rect().top_right().translated(-rect.width() - 8, 26));
    set_rect(rect);

    auto widget = GUI::Widget::construct();
    widget->set_fill_with_background_color(true);

    widget->set_layout(make<GUI::HorizontalBoxLayout>());
    widget->layout()->set_margins({ 4, 4, 4, 4 });
    widget->layout()->set_spacing(4);

    auto left_container = GUI::Widget::construct(widget.ptr());
    left_container->set_layout(make<GUI::VerticalBoxLayout>());

    auto title_label = GUI::Label::construct(title, left_container);
    title_label->set_font(Gfx::Font::default_bold_font());
    title_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    auto text_label = GUI::Label::construct(text, left_container);
    text_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto right_container = GUI::Widget::construct(widget.ptr());
    right_container->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    right_container->set_preferred_size(40, 0);
    right_container->set_layout(make<GUI::HorizontalBoxLayout>());

    auto button = GUI::Button::construct("Okay", right_container);
    button->on_click = [this](auto&) {
        s_windows.remove(this);
        close();
    };

    set_main_widget(widget);
}

NotificationWindow::~NotificationWindow()
{
}

}
