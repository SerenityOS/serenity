/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
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

#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace GUI {

static const char* long_day_names[] = {
    "Sunday", "Monday", "Tuesday", "Wednesday",
    "Thursday", "Friday", "Saturday"
};

static const char* short_day_names[] = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
static const char* mini_day_names[] = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
static const char* micro_day_names[] = { "S", "M", "T", "W", "T", "F", "S" };

static const char* long_month_names[] = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
};

static const char* short_month_names[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
};

Calendar::Calendar(Core::DateTime date_time)
    : m_selected_date(date_time)
    , m_selected_year(date_time.year())
    , m_selected_month(date_time.month())
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(0);

    m_day_name_container = add<GUI::Widget>();
    m_day_name_container->set_layout<GUI::HorizontalBoxLayout>();
    m_day_name_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_day_name_container->set_preferred_size(0, 16);
    m_day_name_container->layout()->set_spacing(0);
    m_day_name_container->set_fill_with_background_color(true);
    m_day_name_container->set_background_role(Gfx::ColorRole::HoverHighlight);
    for (auto& day : m_day_names) {
        day = m_day_name_container->add<GUI::Label>();
        day->set_font(Gfx::Font::default_bold_font());
    }

    m_calendar_tile_container = add<GUI::Widget>();
    m_calendar_tile_container->set_layout<GUI::VerticalBoxLayout>();
    m_calendar_tile_container->layout()->set_spacing(0);

    for (auto& row : m_week_rows) {
        row = m_calendar_tile_container->add<GUI::Widget>();
        row->set_layout<GUI::HorizontalBoxLayout>();
        row->layout()->set_spacing(0);
    }

    int i = 0;
    for (int j = 0; j < 6; j++)
        for (int k = 0; k < 7; k++) {
            m_calendar_tiles[i] = m_week_rows[j]->add<CalendarTile>(i, date_time);
            m_calendar_tiles[i]->on_click = [this](int index) {
                m_previous_selected_date = m_selected_date;
                m_selected_date = m_calendar_tiles[index]->get_date_time();
                update_tiles(m_selected_date.year(), m_selected_date.month());
                if (on_calendar_tile_click)
                    on_calendar_tile_click();
            };
            m_calendar_tiles[i]->on_doubleclick = [this](int index) {
                if (m_calendar_tiles[index]->get_date_time().day() != m_previous_selected_date.day())
                    return;
                if (on_calendar_tile_doubleclick)
                    on_calendar_tile_doubleclick();
            };
            i++;
        }

    m_month_tile_container = add<GUI::Widget>();
    m_month_tile_container->set_visible(false);
    m_month_tile_container->set_layout<GUI::VerticalBoxLayout>();
    m_month_tile_container->set_fill_with_background_color(true);
    m_month_tile_container->set_background_role(Gfx::ColorRole::HoverHighlight);
    m_month_tile_container->layout()->set_spacing(0);

    for (auto& row : m_month_rows) {
        row = m_month_tile_container->add<GUI::Widget>();
        row->set_layout<GUI::HorizontalBoxLayout>();
        row->layout()->set_spacing(0);
    }

    i = 0;
    for (int j = 0; j < 3; j++)
        for (int k = 0; k < 4; k++) {
            m_month_tiles[i] = m_month_rows[j]->add<MonthTile>(i, date_time);
            m_month_tiles[i]->set_button_style(Gfx::ButtonStyle::CoolBar);
            m_month_tiles[i]->on_indexed_click = [this](int index) {
                toggle_mode();
                update_tiles(m_month_tiles[index]->get_date_time().year(), m_month_tiles[index]->get_date_time().month());
                if (on_month_tile_click)
                    on_month_tile_click();
            };
            i++;
        }

    update_tiles(selected_year(), selected_month());
}

Calendar::~Calendar()
{
}

void Calendar::toggle_mode()
{
    m_mode == Month ? m_mode = Year : m_mode = Month;

    if (mode() == Month) {
        m_day_name_container->set_visible(true);
        m_calendar_tile_container->set_visible(true);
        m_month_tile_container->set_visible(false);
    } else {
        m_day_name_container->set_visible(false);
        m_calendar_tile_container->set_visible(false);
        m_month_tile_container->set_visible(true);
    }

    this->resize(this->height(), this->width());
    update_tiles(selected_year(), selected_month());
}

void Calendar::set_grid(bool grid)
{
    if (m_grid == grid)
        return;

    m_grid = grid;

    for (int i = 0; i < 42; i++) {
        m_calendar_tiles[i]->set_grid(grid);
        m_calendar_tiles[i]->update();
    }
}

void Calendar::resize_event(GUI::ResizeEvent& event)
{
    if (m_day_name_container->is_visible()) {
        for (int i = 0; i < 7; i++) {
            if (event.size().width() < 120)
                m_day_names[i]->set_text(micro_day_names[i]);
            else if (event.size().width() < 200)
                m_day_names[i]->set_text(mini_day_names[i]);
            else if (event.size().width() < 480)
                m_day_names[i]->set_text(short_day_names[i]);
            else
                m_day_names[i]->set_text(long_day_names[i]);
        }
    }

    if (m_month_tile_container->is_visible()) {
        for (int i = 0; i < 12; i++) {
            if (event.size().width() < 250)
                m_month_tiles[i]->set_text(short_month_names[i]);
            else
                m_month_tiles[i]->set_text(long_month_names[i]);
        }
    }

    (event.size().width() < 200) ? set_grid(false) : set_grid(true);
}

void Calendar::update_tiles(unsigned int target_year, unsigned int target_month)
{
    set_selected_calendar(target_year, target_month);
    if (mode() == Month) {
        unsigned int i = 0;
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 7; x++) {
                auto date_time = Core::DateTime::create(target_year, target_month, 1);
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

                m_calendar_tiles[i]->update_values(i, date_time);
                m_calendar_tiles[i]->set_selected(date_time.year() == m_selected_date.year() && date_time.month() == m_selected_date.month() && date_time.day() == m_selected_date.day());
                m_calendar_tiles[i]->set_outside_selection(date_time.month() != selected_month() || date_time.year() != selected_year());
                m_calendar_tiles[i]->update();
                i++;
            }
    } else {
        for (int i = 0; i < 12; i++) {
            auto date_time = Core::DateTime::create(target_year, i + 1, 1);
            m_month_tiles[i]->update_values(date_time);
        }
    }
}

const String Calendar::selected_calendar_text(bool long_names)
{
    if (mode() == Month)
        return String::format("%s %u", long_names ? long_month_names[m_selected_month - 1] : short_month_names[m_selected_month - 1], m_selected_year);
    else
        return String::format("%u", m_selected_year);
}

void Calendar::set_selected_calendar(unsigned int year, unsigned int month)
{
    m_selected_year = year;
    m_selected_month = month;
}

Calendar::MonthTile::MonthTile(int index, Core::DateTime date_time)
    : m_index(index)
    , m_date_time(date_time)
{
}

Calendar::MonthTile::~MonthTile()
{
}

void Calendar::MonthTile::mouseup_event(GUI::MouseEvent& event)
{
    if (on_indexed_click)
        on_indexed_click(m_index);

    GUI::Button::mouseup_event(event);
}

Calendar::CalendarTile::CalendarTile(int index, Core::DateTime date_time)
{
    set_frame_thickness(0);
    update_values(index, date_time);
}

void Calendar::CalendarTile::update_values(int index, Core::DateTime date_time)
{
    m_index = index;
    m_date_time = date_time;
    m_display_date = (m_date_time.day() == 1) ? String::format("%s %u", short_month_names[m_date_time.month() - 1], m_date_time.day()) : String::number(m_date_time.day());
}

Calendar::CalendarTile::~CalendarTile()
{
}

void Calendar::CalendarTile::doubleclick_event(GUI::MouseEvent&)
{
    if (on_doubleclick)
        on_doubleclick(m_index);
}

void Calendar::CalendarTile::mousedown_event(GUI::MouseEvent&)
{
    if (on_click)
        on_click(m_index);
}
void Calendar::CalendarTile::enter_event(Core::Event&)
{
    m_hovered = true;
    update();
}

void Calendar::CalendarTile::leave_event(Core::Event&)
{
    m_hovered = false;
    update();
}

bool Calendar::CalendarTile::is_today() const
{
    auto current_date_time = Core::DateTime::now();
    return m_date_time.day() == current_date_time.day() && m_date_time.month() == current_date_time.month() && m_date_time.year() == current_date_time.year();
}

void Calendar::CalendarTile::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());

    if (is_hovered() || is_selected())
        painter.fill_rect(frame_inner_rect(), palette().hover_highlight());
    else
        painter.fill_rect(frame_inner_rect(), palette().base());

    if (m_index < 7)
        painter.draw_line(frame_inner_rect().top_left(), frame_inner_rect().top_right(), Color::NamedColor::Black);
    if (!((m_index + 1) % 7 == 0) && has_grid())
        painter.draw_line(frame_inner_rect().top_right(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);
    if (m_index < 35 && has_grid())
        painter.draw_line(frame_inner_rect().bottom_left(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);

    Gfx::IntRect day_rect;
    if (has_grid()) {
        day_rect = Gfx::IntRect(frame_inner_rect().x(), frame_inner_rect().y(), frame_inner_rect().width(), font().glyph_height() + 4);
        day_rect.set_y(frame_inner_rect().y() + 4);
    } else {
        day_rect = Gfx::IntRect(frame_inner_rect());
    }

    int highlight_rect_width = (font().glyph_width('0') * (m_display_date.length() + 1)) + 2;
    auto display_date = (m_date_time.day() == 1 && frame_inner_rect().width() > highlight_rect_width) ? m_display_date : String::number(m_date_time.day());

    if (is_today()) {
        if (has_grid()) {
            auto highlight_rect = Gfx::IntRect(day_rect.width() / 2 - (highlight_rect_width / 2), day_rect.y(), highlight_rect_width, font().glyph_height() + 4);
            painter.draw_rect(highlight_rect, palette().base_text());
        } else if (is_selected()) {
            painter.draw_rect(frame_inner_rect(), palette().base_text());
        }
        painter.draw_text(day_rect, display_date, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, palette().base_text());
    } else if (is_outside_selection()) {
        painter.draw_text(day_rect, display_date, Gfx::Font::default_font(), Gfx::TextAlignment::Center, Color::LightGray);
    } else {
        if (!has_grid() && is_selected())
            painter.draw_rect(frame_inner_rect(), palette().base_text());
        painter.draw_text(day_rect, display_date, Gfx::Font::default_font(), Gfx::TextAlignment::Center, palette().base_text());
    }
}

}
