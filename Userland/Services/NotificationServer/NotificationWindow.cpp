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
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/ShareableBitmap.h>

namespace NotificationServer {

static HashMap<u32, RefPtr<NotificationWindow>> s_windows;

void update_notification_window_locations()
{
    Gfx::IntRect last_window_rect;
    for (auto& window_entry : s_windows) {
        auto& window = window_entry.value;
        Gfx::IntPoint new_window_location;
        if (last_window_rect.is_null())
            new_window_location = GUI::Desktop::the().rect().top_right().translated(-window->rect().width() - 24, 26);
        else
            new_window_location = last_window_rect.bottom_left().translated(0, 10);
        if (window->rect().location() != new_window_location) {
            window->move_to(new_window_location);
            window->set_original_rect(window->rect());
        }
        last_window_rect = window->rect();
    }
}

NotificationWindow::NotificationWindow(i32 client_id, const String& text, const String& title, const Gfx::ShareableBitmap& icon)
{
    m_id = client_id;
    s_windows.set(m_id, this);

    set_window_type(GUI::WindowType::Notification);
    set_resizable(false);
    set_minimizable(false);

    Gfx::IntRect lowest_notification_rect_on_screen;
    for (auto& window_entry : s_windows) {
        auto& window = window_entry.value;
        if (window->m_original_rect.y() > lowest_notification_rect_on_screen.y())
            lowest_notification_rect_on_screen = window->m_original_rect;
    }

    Gfx::IntRect rect;
    rect.set_width(220);
    rect.set_height(40);
    rect.set_location(GUI::Desktop::the().rect().top_right().translated(-rect.width() - 24, 26));

    if (!lowest_notification_rect_on_screen.is_null())
        rect.set_location(lowest_notification_rect_on_screen.bottom_left().translated(0, 10));

    set_rect(rect);

    m_original_rect = rect;

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);

    widget.set_layout<GUI::HorizontalBoxLayout>();
    widget.layout()->set_margins({ 8, 8, 8, 8 });
    widget.layout()->set_spacing(6);

    if (icon.is_valid()) {
        auto& image = widget.add<GUI::ImageWidget>();
        image.set_bitmap(icon.bitmap());
    }

    auto& left_container = widget.add<GUI::Widget>();
    left_container.set_layout<GUI::VerticalBoxLayout>();

    auto& title_label = left_container.add<GUI::Label>(title);
    title_label.set_font(Gfx::FontDatabase::default_bold_font());
    title_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    auto& text_label = left_container.add<GUI::Label>(text);
    text_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    widget.set_tooltip(text);
    title_label.set_tooltip(text);
    text_label.set_tooltip(text);

    auto& right_container = widget.add<GUI::Widget>();
    right_container.set_fixed_width(36);
    right_container.set_layout<GUI::HorizontalBoxLayout>();

    on_close_request = [this] {
        s_windows.remove(m_id);
        update_notification_window_locations();
        return CloseRequestDecision::Close;
    };
}

NotificationWindow::~NotificationWindow()
{
}

}
