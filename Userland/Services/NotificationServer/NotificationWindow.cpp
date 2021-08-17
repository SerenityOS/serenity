/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NotificationWindow.h"
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/ShareableBitmap.h>

namespace NotificationServer {

static HashMap<u32, RefPtr<NotificationWindow>> s_windows;

static void update_notification_window_locations(const Gfx::IntRect& screen_rect)
{
    Gfx::IntRect last_window_rect;
    for (auto& window_entry : s_windows) {
        auto& window = window_entry.value;
        Gfx::IntPoint new_window_location;
        if (last_window_rect.is_null())
            new_window_location = screen_rect.top_right().translated(-window->rect().width() - 24, 7);
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
    rect.set_location(GUI::Desktop::the().rect().top_right().translated(-rect.width() - 24, 7));

    if (!lowest_notification_rect_on_screen.is_null())
        rect.set_location(lowest_notification_rect_on_screen.bottom_left().translated(0, 10));

    set_rect(rect);

    m_original_rect = rect;

    auto& widget = set_main_widget<GUI::Widget>();

    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::HorizontalBoxLayout>();
    widget.layout()->set_margins(8);
    widget.layout()->set_spacing(6);

    m_image = &widget.add<GUI::ImageWidget>();
    m_image->set_visible(icon.is_valid());
    if (icon.is_valid()) {
        m_image->set_bitmap(icon.bitmap());
    }

    auto& left_container = widget.add<GUI::Widget>();
    left_container.set_layout<GUI::VerticalBoxLayout>();

    m_title_label = &left_container.add<GUI::Label>(title);
    m_title_label->set_font(Gfx::FontDatabase::default_font().bold_variant());
    m_title_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_text_label = &left_container.add<GUI::Label>(text);
    m_text_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    // FIXME: There used to be code for setting the tooltip here, but since we
    // expand the notification now we no longer set the tooltip. Should there be
    // a limit to the lines shown in an expanded notification, at which point a
    // tooltip should be set?

    on_close = [this] {
        s_windows.remove(m_id);
        update_notification_window_locations(GUI::Desktop::the().rect());
    };
}

NotificationWindow::~NotificationWindow()
{
}

RefPtr<NotificationWindow> NotificationWindow::get_window_by_id(i32 id)
{
    auto window = s_windows.get(id);
    return window.value_or(nullptr);
}

void NotificationWindow::resize_to_fit_text()
{
    auto line_height = m_text_label->font().glyph_height();
    auto total_height = m_text_label->preferred_height();

    m_text_label->set_fixed_height(total_height);
    set_height(40 - line_height + total_height);
}

void NotificationWindow::enter_event(Core::Event&)
{
    m_hovering = true;
    resize_to_fit_text();
    move_to_front();
}

void NotificationWindow::leave_event(Core::Event&)
{
    m_hovering = false;
    m_text_label->set_fixed_height(-1);
    set_height(40);
}

void NotificationWindow::set_text(String const& value)
{
    m_text_label->set_text(value);
    if (m_hovering)
        resize_to_fit_text();
}

void NotificationWindow::set_title(String const& value)
{
    m_title_label->set_text(value);
}

void NotificationWindow::set_image(Gfx::ShareableBitmap const& image)
{
    m_image->set_visible(image.is_valid());
    if (image.is_valid()) {
        m_image->set_bitmap(image.bitmap());
    }
}

void NotificationWindow::set_height(int height)
{
    auto rect = this->rect();
    rect.set_height(height);
    set_rect(rect);
}

void NotificationWindow::screen_rects_change_event(GUI::ScreenRectsChangeEvent& event)
{
    update_notification_window_locations(event.rects()[event.main_screen_index()]);
}

}
