/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NotificationWindow.h"
#include "NotificationWidget.h"
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/TextLayout.h>

namespace NotificationServer {

static OrderedHashMap<u32, RefPtr<NotificationWindow>> s_windows;

static void update_notification_window_locations(Gfx::IntRect const& screen_rect)
{
    Optional<Gfx::IntRect> last_window_rect;
    for (auto& window_entry : s_windows) {
        auto& window = window_entry.value;
        Gfx::IntPoint new_window_location;
        if (last_window_rect.has_value())
            new_window_location = last_window_rect.value().bottom_left().moved_down(9);
        else
            new_window_location = screen_rect.top_right().translated(-window->rect().width() - 24 - 1, 7);
        if (window->rect().location() != new_window_location) {
            window->move_to(new_window_location);
        }
        last_window_rect = window->rect();
    }
}

NotificationWindow::NotificationWindow(i32 client_id, String const& text, String const& title, Gfx::ShareableBitmap const& icon, URL::URL const& launch_url)
{
    m_id = client_id;

    set_window_type(GUI::WindowType::Notification);
    set_resizable(false);
    set_minimizable(false);

    Optional<Gfx::IntRect> lowest_notification_rect_on_screen;
    for (auto& window_entry : s_windows) {
        auto& window = window_entry.value;
        if (!lowest_notification_rect_on_screen.has_value()
            || (window->rect().y() > lowest_notification_rect_on_screen.value().y()))
            lowest_notification_rect_on_screen = window->rect();
    }

    s_windows.set(m_id, this);

    Gfx::IntRect rect;
    rect.set_width(220);
    rect.set_height(40);
    rect.set_location(GUI::Desktop::the().rect().top_right().translated(-rect.width() - 24 - 1, 7));

    if (lowest_notification_rect_on_screen.has_value())
        rect.set_location(lowest_notification_rect_on_screen.value().bottom_left().moved_down(9));

    set_rect(rect);

    auto widget = NotificationServer::NotificationWidget::try_create().release_value_but_fixme_should_propagate_errors();
    widget->set_greedy_for_hits(true);
    widget->on_click = [this] {
        if (m_launch_url.is_valid())
            Desktop::Launcher::open(m_launch_url);
    };
    set_main_widget(widget);

    m_image = widget->find_descendant_of_type_named<GUI::ImageWidget>("icon"sv);
    m_image->set_visible(icon.is_valid());
    if (icon.is_valid()) {
        m_image->set_bitmap(icon.bitmap());
    }

    m_title_label = widget->find_descendant_of_type_named<GUI::Label>("title");
    m_title_label->set_text(title);
    m_text_label = widget->find_descendant_of_type_named<GUI::Label>("text");
    m_text_label->set_text(text);
    m_launch_url = launch_url;

    // FIXME: There used to be code for setting the tooltip here, but since we
    // expand the notification now we no longer set the tooltip. Should there be
    // a limit to the lines shown in an expanded notification, at which point a
    // tooltip should be set?

    on_close = [this] {
        s_windows.remove(m_id);
        update_notification_window_locations(GUI::Desktop::the().rect());
    };
}

RefPtr<NotificationWindow> NotificationWindow::get_window_by_id(i32 id)
{
    auto window = s_windows.get(id);
    return window.value_or(nullptr);
}

void NotificationWindow::resize_to_fit_text()
{
    // FIXME: It would be good if Labels could size themselves based on their available width, but for now, we have to
    //        do the calculation manually.
    Gfx::TextLayout text_layout { m_text_label->font(), Utf8View { m_text_label->text() }, m_text_label->rect().to_type<float>() };
    auto line_count = text_layout.lines(Gfx::TextElision::None, m_text_label->text_wrapping()).size();

    auto line_height = m_text_label->font().preferred_line_height();
    auto text_height = line_height * line_count;
    m_text_label->set_height(text_height);
    set_height(m_title_label->height() + GUI::Layout::default_spacing + text_height + main_widget()->layout()->margins().vertical_total());
}

void NotificationWindow::enter_event(Core::Event&)
{
    m_hovering = true;
    resize_to_fit_text();
    move_to_front();
    update_notification_window_locations(GUI::Desktop::the().rect());
    if (m_launch_url.is_valid())
        set_cursor(Gfx::StandardCursor::Hand);
}

void NotificationWindow::leave_event(Core::Event&)
{
    m_hovering = false;
    m_text_label->set_height(40 - (m_title_label->height() + GUI::Layout::default_spacing + main_widget()->layout()->margins().vertical_total()));
    set_height(40);
    update_notification_window_locations(GUI::Desktop::the().rect());
    set_cursor(Gfx::StandardCursor::Arrow);
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
