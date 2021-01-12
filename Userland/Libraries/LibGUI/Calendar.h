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

#pragma once

#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

namespace GUI {

class Calendar final : public GUI::Widget {
    C_OBJECT(Calendar)

public:
    enum Mode {
        Month,
        Year
    };

    enum {
        ShortNames,
        LongNames
    };

    Calendar(Core::DateTime);
    virtual ~Calendar() override;

    unsigned int selected_year() const { return m_selected_year; }
    unsigned int selected_month() const { return m_selected_month; }
    const String selected_calendar_text(bool long_names = ShortNames);
    void update_tiles(unsigned int target_year, unsigned int target_month);
    void set_selected_calendar(unsigned int year, unsigned int month);
    void set_selected_date(Core::DateTime date_time) { m_selected_date = date_time; }
    Core::DateTime selected_date() const { return m_selected_date; }
    void toggle_mode();
    void set_grid(bool grid);
    bool has_grid() { return m_grid; }
    Mode mode() const { return m_mode; }

    Function<void()> on_calendar_tile_click;
    Function<void()> on_calendar_tile_doubleclick;
    Function<void()> on_month_tile_click;

private:
    virtual void resize_event(GUI::ResizeEvent&) override;

    class CalendarTile final : public GUI::Frame {
        C_OBJECT(CalendarTile)
    public:
        CalendarTile(int index, Core::DateTime m_date_time);
        void update_values(int index, Core::DateTime date_time);
        virtual ~CalendarTile() override;
        bool is_today() const;
        bool is_hovered() const { return m_hovered; }
        bool is_selected() const { return m_selected; }
        void set_selected(bool b) { m_selected = b; }
        bool is_outside_selection() const { return m_outside_selection; }
        void set_outside_selection(bool b) { m_outside_selection = b; }
        bool has_grid() const { return m_grid; }
        void set_grid(bool b) { m_grid = b; }
        Core::DateTime get_date_time() { return m_date_time; }
        Function<void(int index)> on_doubleclick;
        Function<void(int index)> on_click;

    private:
        virtual void doubleclick_event(GUI::MouseEvent&) override;
        virtual void mousedown_event(GUI::MouseEvent&) override;
        virtual void enter_event(Core::Event&) override;
        virtual void leave_event(Core::Event&) override;
        virtual void paint_event(GUI::PaintEvent&) override;

        int m_index { 0 };
        bool m_outside_selection { false };
        bool m_hovered { false };
        bool m_selected { false };
        bool m_grid { true };
        String m_display_date;
        Core::DateTime m_date_time;
    };

    class MonthTile final : public GUI::Button {
        C_OBJECT(MonthTile)
    public:
        MonthTile(int index, Core::DateTime m_date_time);
        virtual ~MonthTile() override;
        void update_values(Core::DateTime date_time) { m_date_time = date_time; }
        Core::DateTime get_date_time() { return m_date_time; }
        Function<void(int index)> on_indexed_click;

    private:
        virtual void mouseup_event(GUI::MouseEvent&) override;

        int m_index { 0 };
        Core::DateTime m_date_time;
    };

    RefPtr<MonthTile> m_month_tiles[12];
    RefPtr<CalendarTile> m_calendar_tiles[42];
    RefPtr<GUI::Label> m_day_names[7];
    RefPtr<GUI::Widget> m_week_rows[6];
    RefPtr<GUI::Widget> m_month_rows[3];
    RefPtr<GUI::Widget> m_month_tile_container;
    RefPtr<GUI::Widget> m_calendar_tile_container;
    RefPtr<GUI::Widget> m_day_name_container;

    Core::DateTime m_selected_date;
    Core::DateTime m_previous_selected_date;
    unsigned int m_selected_year { 0 };
    unsigned int m_selected_month { 0 };
    bool m_grid { true };
    Mode m_mode { Month };
};

}
