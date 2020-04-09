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

#include "CalendarWidget.h"
#include "AddEventDialog.h"
#include "Calendar.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

CalendarWidget::CalendarWidget()
{
    m_calendar = make<Calendar>(Core::DateTime::now());

    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    m_top_container = add<Widget>();
    m_top_container->set_layout<GUI::HorizontalBoxLayout>();
    m_top_container->layout()->set_margins({ 4, 4, 4, 4 });
    m_top_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_top_container->set_preferred_size(0, 45);

    auto& top_left_container = m_top_container->add<Widget>();
    top_left_container.set_layout<GUI::HorizontalBoxLayout>();
    top_left_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    top_left_container.set_preferred_size(0, 45);
    m_selected_date_label = top_left_container.add<GUI::Label>(m_calendar->selected_date_text());
    m_selected_date_label->set_font(Gfx::Font::default_bold_font());
    m_selected_date_label->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_selected_date_label->set_preferred_size(80, 14);
    m_selected_date_label->set_text_alignment(Gfx::TextAlignment::Center);

    m_bottom_container = add<Widget>();

    m_prev_month_button = top_left_container.add<GUI::Button>("<");
    m_prev_month_button->set_font(Gfx::Font::default_bold_font());
    m_prev_month_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_prev_month_button->set_preferred_size(40, 40);
    m_prev_month_button->on_click = [this] {
        int m_target_month = m_calendar->selected_month() - 1;
        int m_target_year = m_calendar->selected_year();

        if (m_calendar->selected_month() <= 1) {
            m_target_month = 12;
            m_target_year--;
        }
        update_calendar_tiles(m_target_year, m_target_month);
    };

    m_next_month_button = top_left_container.add<GUI::Button>(">");
    m_next_month_button->set_font(Gfx::Font::default_bold_font());
    m_next_month_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_next_month_button->set_preferred_size(40, 40);
    m_next_month_button->on_click = [this] {
        int m_target_month = m_calendar->selected_month() + 1;
        int m_target_year = m_calendar->selected_year();

        if (m_calendar->selected_month() >= 12) {
            m_target_month = 1;
            m_target_year++;
        }
        update_calendar_tiles(m_target_year, m_target_month);
    };

    auto& top_right_container = m_top_container->add<Widget>();
    top_right_container.set_layout<GUI::HorizontalBoxLayout>();
    top_right_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    top_right_container.set_preferred_size(0, 45);

    top_right_container.layout()->add_spacer();

    m_add_event_button = top_right_container.add<GUI::Button>("Add Event");
    m_add_event_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_add_event_button->set_preferred_size(100, 25);
    m_add_event_button->on_click = [this] {
        show_add_event_window();
    };

    update_calendar_tiles(m_calendar->selected_year(), m_calendar->selected_month());
}

CalendarWidget::~CalendarWidget()
{
}

void CalendarWidget::resize_event(GUI::ResizeEvent& event)
{
    if (event.size().width() < 350) {
        if (m_next_month_button->size_policy(Orientation::Horizontal) == GUI::SizePolicy::Fixed)
            m_next_month_button->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        if (m_prev_month_button->size_policy(Orientation::Horizontal) == GUI::SizePolicy::Fixed)
            m_prev_month_button->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    } else {
        if (m_next_month_button->size_policy(Orientation::Horizontal) == GUI::SizePolicy::Fill)
            m_next_month_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        if (m_prev_month_button->size_policy(Orientation::Horizontal) == GUI::SizePolicy::Fill)
            m_prev_month_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    }

    if (m_top_container->height() > event.size().height() / 3) {
        if (m_top_container->is_visible())
            m_top_container->set_visible(false);
    } else if (!m_top_container->is_visible())
        m_top_container->set_visible(true);

    int top_container_height = (m_top_container->is_visible()) ? 47 : 0;
    m_tile_width = event.size().width() / 7;
    m_tile_height = (event.size().height() - top_container_height) / 5;

    int i = 0;
    for (int y = 0; y < 5; y++)
        for (int x = 0; x < 7; x++) {
            int x_offset = x * m_tile_width;
            int y_offset = (y * m_tile_height);
            m_calendar_tiles[i]->set_relative_rect(x_offset, y_offset, m_tile_width, m_tile_height);
            i++;
        }
}

void CalendarWidget::update_calendar_tiles(int target_year, int target_month)
{
    unsigned int i = 0;
    //TODO: Modify m_tile_height if the end of the month doesn't fit onto the current tile array
    for (int y = 0; y < 5; y++)
        for (int x = 0; x < 7; x++) {
            auto date_time = Core::DateTime::create(target_year, target_month, 1);
            int x_offset = x * m_tile_width;
            int y_offset = (y * m_tile_height);

            unsigned int start_of_month = date_time.weekday();
            unsigned int year;
            unsigned int month;
            unsigned int day;

            if (start_of_month > i) {
                month = (target_month - 1 == 0) ? 12 : target_month - 1;
                year = (month == 12) ? target_year - 1 : target_year;
                date_time.set_time(year, month, 1);
                day = (date_time.days_in_month() - (start_of_month) + i) + 1;
                date_time.set_time(year, month, day);

            } else if ((i - start_of_month) + 1 > date_time.days_in_month()) {
                month = (target_month + 1) > 12 ? 1 : target_month + 1;
                year = (month == 1) ? target_year + 1 : target_year;
                day = ((i - start_of_month) + 1) - date_time.days_in_month();
                date_time.set_time(year, month, day);
            } else {
                month = target_month;
                year = target_year;
                day = (i - start_of_month) + 1;
                date_time.set_time(year, month, day);
            }

            if (!m_calendar_tiles[i]) {
                m_calendar_tiles[i] = m_bottom_container->add<CalendarTile>(*m_calendar, i, date_time);
                m_calendar_tiles[i]->set_frame_thickness(0);
                m_calendar_tiles[i]->set_relative_rect(x_offset, y_offset, 85, 85);
            } else {
                m_calendar_tiles[i]->update_values(*m_calendar, i, date_time);
                m_calendar_tiles[i]->update();
            }
            i++;
        }

    m_calendar->set_selected_date(target_year, target_month);
    m_selected_date_label->set_text(m_calendar->selected_date_text());
}

void CalendarWidget::show_add_event_window()
{
    AddEventDialog::show(*move(m_calendar), Core::DateTime::now(), window());
}

CalendarWidget::CalendarTile::CalendarTile(Calendar& calendar, int index, Core::DateTime date_time)
    : m_index(index)
    , m_date_time(date_time)
    , m_calendar(calendar)
{
    update_values(calendar, index, date_time);
}

void CalendarWidget::CalendarTile::update_values(Calendar& calendar, int index, Core::DateTime date_time)
{
    m_calendar = calendar;
    m_index = index;
    m_date_time = date_time;
    m_display_weekday_name = index < 7;

    if (m_display_weekday_name) {
        static const String m_day_names[] = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        m_weekday_name = m_day_names[index];
    }

    m_display_date = (m_date_time.day() == 1) ? String::format("%s %d", Calendar::name_of_month(m_date_time.month()).characters(), m_date_time.day()) : String::number(m_date_time.day());
}

CalendarWidget::CalendarTile::~CalendarTile()
{
}

void CalendarWidget::CalendarTile::doubleclick_event(GUI::MouseEvent& event)
{
    GUI::Widget::doubleclick_event(event);
    //TOOD: Should be calling show_add_event_window. Would we just replace m_calender /w m_calender_widget?
    AddEventDialog::show(&m_calendar, m_date_time, window());
}

void CalendarWidget::CalendarTile::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(frame_inner_rect(), palette().base());

    painter.draw_line(frame_inner_rect().top_right(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);
    if (m_index == 0 || m_index % 7 == 0)
        painter.draw_line(frame_inner_rect().top_left(), frame_inner_rect().bottom_left(), Color::NamedColor::Black);

    if (m_index < 7)
        painter.draw_line(frame_inner_rect().top_left(), frame_inner_rect().top_right(), Color::NamedColor::Black);
    painter.draw_line(frame_inner_rect().bottom_left(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);

    Gfx::Rect day_rect = Gfx::Rect(frame_inner_rect().x(), frame_inner_rect().y(), frame_inner_rect().width(), font().glyph_height() + 4);

    int weekday_characters_width = (font().glyph_width('0') * (m_weekday_name.length() + 1)) + 4;
    if (m_display_weekday_name && (frame_inner_rect().height() > (font().glyph_height() + 4) * 2) && (frame_inner_rect().width() > weekday_characters_width)) {
        auto weekday_rect = Gfx::Rect(frame_inner_rect().x(), frame_inner_rect().y(), frame_inner_rect().width(), font().glyph_height() + 4);
        weekday_rect.set_top(frame_inner_rect().y() + 2);
        painter.draw_text(weekday_rect, m_weekday_name, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, palette().base_text());
        day_rect.set_y(frame_inner_rect().y() + 15);
    } else {
        day_rect = Gfx::Rect(frame_inner_rect().x(), frame_inner_rect().y(), frame_inner_rect().width(), font().glyph_height() + 4);
        day_rect.set_y(frame_inner_rect().y() + 4);
    }

    int highlight_rect_width = (font().glyph_width('0') * (m_display_date.length() + 1)) + 2;
    auto display_date = (m_date_time.day() == 1 && frame_inner_rect().width() > highlight_rect_width) ? m_display_date : String::number(m_date_time.day());

    if (m_calendar.is_today(m_date_time)) {
        auto highlight_rect = Gfx::Rect(day_rect.width() / 2 - (highlight_rect_width / 2), day_rect.y(), highlight_rect_width, font().glyph_height() + 4);
        painter.draw_rect(highlight_rect, palette().base_text());
        painter.draw_text(day_rect, display_date, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, palette().base_text());
    } else
        painter.draw_text(day_rect, display_date, Gfx::TextAlignment::Center, palette().base_text());
}
