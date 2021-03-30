/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2020-2021, the SerenityOS developers
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

#pragma once

#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Widget.h>

namespace GUI {

class Calendar final : public GUI::Widget {
    C_OBJECT(Calendar)

public:
    enum Mode {
        Month,
        Year
    };

    enum Format {
        ShortMonthYear,
        LongMonthYear,
        MonthOnly,
        YearOnly
    };

    Calendar(Core::DateTime date_time = Core::DateTime::now(), Mode mode = Month);
    virtual ~Calendar() override;

    void set_selected_date(Core::DateTime date_time) { m_selected_date = date_time; }
    Core::DateTime selected_date() const { return m_selected_date; }

    void set_view_date(unsigned year, unsigned month)
    {
        m_view_year = year;
        m_view_month = month;
    }
    unsigned view_year() const { return m_view_year; }
    unsigned view_month() const { return m_view_month; }

    String formatted_date(Format format = LongMonthYear);

    Mode mode() const { return m_mode; }
    void toggle_mode();

    void update_tiles(unsigned year, unsigned month);

    void set_grid(bool);
    bool has_grid() const { return m_grid; }

    void set_show_year(bool b) { m_show_year = b; }
    bool is_showing_year() const { return m_show_year; }

    void set_show_month_and_year(bool b) { m_show_month_year = b; }
    bool is_showing_month_and_year() const { return m_show_month_year; }

    void set_show_days_of_the_week(bool b) { m_show_days = b; }
    bool is_showing_days_of_the_week() const { return m_show_days; }

    Gfx::IntSize unadjusted_tile_size() const { return m_unadjusted_tile_size; }
    void set_unadjusted_tile_size(int width, int height)
    {
        m_unadjusted_tile_size.set_width(width);
        m_unadjusted_tile_size.set_height(height);
    }

    Function<void()> on_tile_click;
    Function<void()> on_tile_doubleclick;
    Function<void()> on_month_click;

private:
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&);
    virtual void leave_event(Core::Event&) override;

    struct Day {
        String name;
        int width { 0 };
        int height { 16 };
    };
    Vector<Day> m_days;

    struct MonthTile {
        String name;
        Gfx::IntRect rect;
        int width { 0 };
        int height { 0 };
        bool is_hovered { false };
        bool is_being_pressed { false };
    };
    Vector<MonthTile> m_months;

    struct Tile {
        Core::DateTime date_time;
        Gfx::IntRect rect;
        int width { 0 };
        int height { 0 };
        bool is_today { false };
        bool is_selected { false };
        bool is_hovered { false };
        bool is_outside_selected_month { false };
    };
    Vector<Tile> m_tiles[12];

    bool m_grid { true };
    bool m_show_month_year { true };
    bool m_show_days { true };
    bool m_show_year { false };
    bool m_show_month_tiles { false };
    int m_currently_pressed_index { -1 };
    unsigned m_view_year;
    unsigned m_view_month;
    Core::DateTime m_selected_date;
    Core::DateTime m_previous_selected_date;
    Gfx::IntSize m_unadjusted_tile_size;
    Gfx::IntSize m_event_size;
    Gfx::IntSize m_month_size[12];
    Mode m_mode { Month };
};

}
