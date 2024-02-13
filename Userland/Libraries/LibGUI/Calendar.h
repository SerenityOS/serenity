/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibConfig/Listener.h>
#include <LibCore/DateTime.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>

namespace GUI {

class Calendar
    : public GUI::AbstractScrollableWidget
    , public Config::Listener {
    C_OBJECT(Calendar)

public:
    struct Tile {
        unsigned year;
        unsigned month;
        unsigned day;
        Gfx::IntRect rect;
        int width { 0 };
        int height { 0 };
        bool is_today { false };
        bool is_selected { false };
        bool is_hovered { false };
        bool is_outside_selected_month { false };
    };

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

    virtual ~Calendar() override = default;

    void set_selected_date(Core::DateTime date_time) { m_selected_date = date_time; }
    Core::DateTime selected_date() const { return m_selected_date; }

    void set_view_date(unsigned year, unsigned month)
    {
        m_view_year = year;
        m_view_month = month;
    }
    unsigned view_year() const { return m_view_year; }
    unsigned view_month() const { return m_view_month; }

    ErrorOr<String> formatted_date(Format format = LongMonthYear);

    Mode mode() const { return m_mode; }
    void set_mode(Mode);
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

    void show_previous_date();
    void show_next_date();

    Gfx::IntSize unadjusted_tile_size() const { return m_unadjusted_tile_size; }
    void set_unadjusted_tile_size(int width, int height)
    {
        m_unadjusted_tile_size.set_width(width);
        m_unadjusted_tile_size.set_height(height);
    }

    virtual void config_string_did_change(StringView, StringView, StringView, StringView) override;
    virtual void config_i32_did_change(StringView, StringView, StringView, i32 value) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void paint_tile(GUI::Painter&, GUI::Calendar::Tile&, Gfx::IntRect&, int x_offset, int y_offset, int day_offset);

    Function<void()> on_scroll;
    Function<void()> on_tile_click;
    Function<void()> on_tile_doubleclick;
    Function<void()> on_month_click;

protected:
    Calendar(Core::DateTime date_time = Core::DateTime::now(), Mode mode = Month);

private:
    static size_t day_of_week_index(ByteString const&);

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;

    enum class DayOfWeek {
        Sunday,
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday,
        Saturday
    };

    bool is_day_in_weekend(DayOfWeek);

    struct Day {
        ByteString name;
        int width { 0 };
        int height { 16 };
    };
    Vector<Day> m_days;

    struct MonthTile {
        ByteString name;
        Gfx::IntRect rect;
        int width { 0 };
        int height { 0 };
        bool is_hovered { false };
        bool is_being_pressed { false };
    };
    Vector<MonthTile> m_months;

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

    DayOfWeek m_first_day_of_week { DayOfWeek::Sunday };
    DayOfWeek m_first_day_of_weekend { DayOfWeek::Saturday };
    int m_weekend_length { 2 };
};

class MonthListModel final : public GUI::Model {
public:
    enum DisplayMode {
        Short,
        Long,
    };

    enum Column {
        Month,
        __Count,
    };

    static NonnullRefPtr<MonthListModel> create(DisplayMode mode = DisplayMode::Short) { return adopt_ref(*new MonthListModel(mode)); }
    virtual ~MonthListModel() override = default;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 12; }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

private:
    MonthListModel(DisplayMode mode)
        : m_mode(mode)
    {
    }

    DisplayMode m_mode;
};

}
