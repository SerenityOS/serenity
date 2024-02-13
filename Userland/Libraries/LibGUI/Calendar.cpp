/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateConstants.h>
#include <AK/String.h>
#include <LibConfig/Client.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Calendar);

namespace GUI {

static auto const extra_large_font = Gfx::BitmapFont::load_from_uri("resource://fonts/MarietaRegular36.font"sv);
static auto const large_font = Gfx::BitmapFont::load_from_uri("resource://fonts/MarietaRegular24.font"sv);
static auto const medium_font = Gfx::BitmapFont::load_from_uri("resource://fonts/PebbletonRegular14.font"sv);
static auto const small_font = Gfx::BitmapFont::load_from_uri("resource://fonts/KaticaRegular10.font"sv);

Calendar::Calendar(Core::DateTime date_time, Mode mode)
    : m_selected_date(date_time)
    , m_mode(mode)
{
    auto first_day_of_week = Config::read_string("Calendar"sv, "View"sv, "FirstDayOfWeek"sv, "Sunday"sv);
    m_first_day_of_week = static_cast<DayOfWeek>(day_of_week_index(first_day_of_week));

    auto first_day_of_weekend = Config::read_string("Calendar"sv, "View"sv, "FirstDayOfWeekend"sv, "Saturday"sv);
    m_first_day_of_weekend = static_cast<DayOfWeek>(day_of_week_index(first_day_of_weekend));

    auto weekend_length = Config::read_i32("Calendar"sv, "View"sv, "WeekendLength"sv, 2);
    m_weekend_length = weekend_length;

    set_fill_with_background_color(true);
    set_scrollbars_enabled(false);

    for (int i = 0; i < 7; i++) {
        Day day;
        m_days.append(move(day));
    }
    for (int i = 0; i < 12; i++) {
        MonthTile month;
        m_months.append(move(month));
        for (int j = 0; j < 42; j++) {
            Tile tile;
            m_tiles[i].append(move(tile));
        }
    }

    auto default_view = Config::read_string("Calendar"sv, "View"sv, "DefaultView"sv, "Month"sv);
    if (default_view == "Year") {
        m_mode = Year;
        m_show_days = false;
        m_show_year = true;
        m_show_month_year = true;
    }

    update_tiles(m_selected_date.year(), m_selected_date.month());

    REGISTER_ENUM_PROPERTY("mode", this->mode, this->set_mode, Calendar::Mode,
        { Calendar::Mode::Month, "Month" },
        { Calendar::Mode::Year, "Year" });
}

void Calendar::set_grid(bool show)
{
    if (m_grid == show)
        return;
    m_grid = show;
}

void Calendar::toggle_mode()
{
    m_mode == Month ? m_mode = Year : m_mode = Month;
    set_show_days_of_the_week(!m_show_days);
    set_show_year(!m_show_year);
    set_show_month_and_year(!m_show_month_year);
    update_tiles(this->view_year(), this->view_month());
    ResizeEvent resize_evt(relative_rect().size());
    event(resize_evt);
    invalidate_layout();
}

void Calendar::set_mode(Mode mode)
{
    if (mode != m_mode) {
        toggle_mode();
    }
}

void Calendar::show_previous_date()
{
    unsigned view_month = m_view_month;
    unsigned view_year = m_view_year;
    if (m_mode == GUI::Calendar::Month) {
        --view_month;
        if (view_month == 0) {
            view_month = 12;
            --view_year;
        }
    } else {
        --view_year;
    }
    update_tiles(view_year, view_month);
}

void Calendar::show_next_date()
{
    unsigned view_month = m_view_month;
    unsigned view_year = m_view_year;
    if (m_mode == GUI::Calendar::Month) {
        ++view_month;
        if (view_month == 13) {
            view_month = 1;
            ++view_year;
        }
    } else {
        ++view_year;
    }
    update_tiles(view_year, view_month);
}

void Calendar::resize_event(GUI::ResizeEvent& event)
{
    m_event_size.set_width(event.size().width() - (frame_thickness() * 2));
    m_event_size.set_height(event.size().height() - (frame_thickness() * 2));

    if (mode() == Month) {
        if (m_event_size.width() < 160 || m_event_size.height() < 130)
            set_show_month_and_year(false);
        else if (m_event_size.width() >= 160 && m_event_size.height() >= 130)
            set_show_month_and_year(true);

        set_show_year(false);

        int const GRID_LINES = 6;
        int tile_width = (m_event_size.width() - GRID_LINES) / 7;
        int width_remainder = (m_event_size.width() - GRID_LINES) % 7;
        int y_offset = is_showing_days_of_the_week() ? 16 : 0;
        y_offset += is_showing_month_and_year() ? 24 : 0;
        int tile_height = (m_event_size.height() - y_offset - GRID_LINES) / 6;
        int height_remainder = (m_event_size.height() - y_offset - GRID_LINES) % 6;

        set_unadjusted_tile_size(tile_width, tile_height);
        tile_width < 30 || tile_height < 30 ? set_grid(false) : set_grid(true);

        for (int i = 0; i < 42; i++) {
            m_tiles[0][i].width = tile_width;
            m_tiles[0][i].height = tile_height;
        }

        for (auto& day : m_days)
            day.width = tile_width;

        for (int i = 0; i < width_remainder; i++) {
            m_days[i].width = (tile_width + 1);
            for (int j = i; j < i + 36; j += 7) {
                m_tiles[0][j].width = tile_width + 1;
            }
        }

        for (int j = 0; j < height_remainder * 7; j++)
            m_tiles[0][j].height = tile_height + 1;

        if (is_showing_days_of_the_week()) {
            for (int i = 0; i < 7; i++) {
                if (m_event_size.width() < 138)
                    m_days[i].name = micro_day_names[i];
                else if (m_event_size.width() < 200)
                    m_days[i].name = mini_day_names[i];
                else if (m_event_size.width() < 480)
                    m_days[i].name = short_day_names[i];
                else
                    m_days[i].name = long_day_names[i];
            }
        }
    } else {
        if (m_event_size.width() < 140 && m_event_size.height() < 120)
            set_show_year(false);
        else if (m_event_size.width() >= 140 && m_event_size.height() >= 120)
            set_show_year(true);

        set_show_month_and_year(false);

        int const VERT_GRID_LINES = 27;
        int const HORI_GRID_LINES = 15;
        int const THREADING = 3;
        int const MONTH_TITLE = 19;
        int tile_width = (m_event_size.width() - VERT_GRID_LINES) / 28;
        int width_remainder = (m_event_size.width() - VERT_GRID_LINES) % 28;
        int y_offset = is_showing_year() ? 22 : 0;
        y_offset += (MONTH_TITLE * 3) + (THREADING * 3);
        int tile_height = (m_event_size.height() - y_offset - HORI_GRID_LINES) / 18;
        int height_remainder = (m_event_size.height() - y_offset - HORI_GRID_LINES) % 18;

        set_grid(false);
        set_unadjusted_tile_size(tile_width, tile_height);
        if (unadjusted_tile_size().width() < 17 || unadjusted_tile_size().height() < 13)
            m_show_month_tiles = true;
        else
            m_show_month_tiles = false;

        if (m_show_month_tiles) {
            int month_tile_width = m_event_size.width() / 4;
            int width_remainder = m_event_size.width() % 4;
            int y_offset = is_showing_year() ? 23 : 0;
            int month_tile_height = (m_event_size.height() - y_offset) / 3;
            int height_remainder = (m_event_size.height() - y_offset) % 3;

            for (int i = 0; i < 12; i++) {
                m_months[i].width = month_tile_width;
                m_months[i].height = month_tile_height;
                if (m_event_size.width() < 250)
                    m_months[i].name = short_month_names[i];
                else
                    m_months[i].name = long_month_names[i];
            }

            if (width_remainder) {
                for (int i = 0; i < width_remainder; i++) {
                    for (int j = i; j < 12; j += 4) {
                        m_months[j].width = month_tile_width + 1;
                    }
                }
            }

            if (height_remainder) {
                for (int i = 0; i < height_remainder * 4; i++) {
                    m_months[i].height = month_tile_height + 1;
                }
            }
            return;
        }

        for (int i = 0; i < 12; i++) {
            int remainder = 0;
            if (i == 0 || i == 4 || i == 8)
                remainder = min(width_remainder, 7);
            if (i == 1 || i == 5 || i == 9)
                width_remainder > 7 ? remainder = min(width_remainder - 7, 7) : remainder = 0;
            if (i == 2 || i == 6 || i == 10)
                width_remainder > 14 ? remainder = min(width_remainder - 14, 7) : remainder = 0;
            if (i == 3 || i == 7 || i == 11)
                width_remainder > 21 ? remainder = width_remainder - 21 : remainder = 0;
            m_month_size[i].set_width(remainder + 6 + tile_width * 7);

            if (i >= 0 && i <= 3)
                remainder = min(height_remainder, 6);
            if (i >= 4 && i <= 7)
                height_remainder > 6 ? remainder = min(height_remainder - 6, 6) : remainder = 0;
            if (i >= 8 && i <= 12)
                height_remainder > 12 ? remainder = height_remainder - 12 : remainder = 0;
            m_month_size[i].set_height(remainder + 5 + tile_height * 6);

            for (int j = 0; j < 42; j++) {
                m_tiles[i][j].width = tile_width;
                m_tiles[i][j].height = tile_height;
            }
        }

        if (width_remainder) {
            for (int i = 0; i < 12; i += 4) {
                for (int j = 0; j < min(width_remainder, 7); j++) {
                    for (int k = j; k < j + 36; k += 7) {
                        m_tiles[i][k].width = tile_width + 1;
                    }
                }
            }
        }
        if (width_remainder > 7) {
            for (int i = 1; i < 12; i += 4) {
                for (int j = 0; j < min(width_remainder - 7, 7); j++) {
                    for (int k = j; k < j + 36; k += 7) {
                        m_tiles[i][k].width = tile_width + 1;
                    }
                }
            }
        }
        if (width_remainder > 14) {
            for (int i = 2; i < 12; i += 4) {
                for (int j = 0; j < min(width_remainder - 14, 7); j++) {
                    for (int k = j; k < j + 36; k += 7) {
                        m_tiles[i][k].width = tile_width + 1;
                    }
                }
            }
        }
        if (width_remainder > 21) {
            for (int i = 3; i < 12; i += 4) {
                for (int j = 0; j < width_remainder - 21; j++) {
                    for (int k = j; k < j + 36; k += 7) {
                        m_tiles[i][k].width = tile_width + 1;
                    }
                }
            }
        }
        if (height_remainder) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < min(height_remainder, 6) * 7; j++) {
                    m_tiles[i][j].height = tile_height + 1;
                }
            }
        }
        if (height_remainder > 6) {
            for (int i = 4; i < 8; i++) {
                for (int j = 0; j < min(height_remainder - 6, 6) * 7; j++) {
                    m_tiles[i][j].height = tile_height + 1;
                }
            }
        }
        if (height_remainder > 12) {
            for (int i = 8; i < 12; i++) {
                for (int j = 0; j < (height_remainder - 12) * 7; j++) {
                    m_tiles[i][j].height = tile_height + 1;
                }
            }
        }
    }
}

void Calendar::update_tiles(unsigned view_year, unsigned view_month)
{
    set_view_date(view_year, view_month);

    auto now = Core::DateTime::now();
    unsigned months = mode() == Month ? 1 : 12;
    for (unsigned i = 0; i < months; i++) {
        if (mode() == Year)
            view_month = i + 1;

        auto first_day_of_current_month = Core::DateTime::create(view_year, view_month, 1);
        unsigned start_of_month = (first_day_of_current_month.weekday() - to_underlying(m_first_day_of_week) + 7) % 7;
        unsigned days_from_previous_month_to_show = start_of_month == 0 ? 7 : start_of_month;

        for (unsigned j = 0; j < 42; j++) {
            unsigned year;
            unsigned month;
            unsigned day;

            if (j + 1 <= days_from_previous_month_to_show) {
                // Day from previous month.
                month = (view_month - 1 == 0) ? 12 : view_month - 1;
                year = (month == 12) ? view_year - 1 : view_year;
                day = days_in_month(year, month) + j + 1 - days_from_previous_month_to_show;
            } else if (j + 1 > days_from_previous_month_to_show + first_day_of_current_month.days_in_month()) {
                // Day from next month.
                month = (view_month + 1) > 12 ? 1 : view_month + 1;
                year = (month == 1) ? view_year + 1 : view_year;
                day = j + 1 - days_from_previous_month_to_show - first_day_of_current_month.days_in_month();
            } else {
                // Day from current month.
                month = view_month;
                year = view_year;
                day = j + 1 - days_from_previous_month_to_show;
            }

            m_tiles[i][j].year = year;
            m_tiles[i][j].month = month;
            m_tiles[i][j].day = day;
            m_tiles[i][j].is_outside_selected_month = (month != view_month
                || year != view_year);
            m_tiles[i][j].is_selected = (year == m_selected_date.year()
                && month == m_selected_date.month()
                && day == m_selected_date.day()
                && (mode() == Year ? !m_tiles[i][j].is_outside_selected_month : true));
            m_tiles[i][j].is_today = (day == now.day()
                && month == now.month()
                && year == now.year());
        }
    }
    update();
}

ErrorOr<String> Calendar::formatted_date(Format format)
{
    switch (format) {
    case ShortMonthYear:
        return String::formatted("{} {}", short_month_names[view_month() - 1], view_year());
    case LongMonthYear:
        return String::formatted("{} {}", long_month_names[view_month() - 1], view_year());
    case MonthOnly:
        return String::formatted("{}", long_month_names[view_month() - 1]);
    case YearOnly:
        return String::number(view_year());
    }
    VERIFY_NOT_REACHED();
}

void Calendar::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    if (has_grid())
        painter.fill_rect(frame_inner_rect(), palette().threed_shadow2());
    else
        painter.fill_rect(frame_inner_rect(), palette().base());

    painter.translate(frame_thickness(), frame_thickness());

    int x_offset = 0;
    int y_offset = 0;

    if (is_showing_year()) {
        auto year_only_rect = Gfx::IntRect(
            0,
            0,
            frame_inner_rect().width(),
            22);
        y_offset += year_only_rect.height();
        painter.fill_rect(year_only_rect, palette().hover_highlight());
        painter.draw_text(year_only_rect, formatted_date(YearOnly).release_value_but_fixme_should_propagate_errors(), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, (!m_show_month_tiles ? palette().threed_shadow1() : palette().threed_shadow2()), 1);
        y_offset += 1;
        if (!m_show_month_tiles) {
            painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, palette().threed_highlight(), 1);
            y_offset += 1;
        }
    } else if (is_showing_month_and_year()) {
        auto month_year_rect = Gfx::IntRect(
            0,
            0,
            frame_inner_rect().width(),
            22);
        painter.fill_rect(month_year_rect, palette().hover_highlight());
        month_year_rect.set_width(frame_inner_rect().width() / 2);
        painter.draw_text(month_year_rect, formatted_date(MonthOnly).release_value_but_fixme_should_propagate_errors(), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        month_year_rect.set_x(month_year_rect.width() + (frame_inner_rect().width() % 2 ? 1 : 0));
        painter.draw_text(month_year_rect, formatted_date(YearOnly).release_value_but_fixme_should_propagate_errors(), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        y_offset += 22;
        painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, palette().threed_shadow1(), 1);
        y_offset += 1;
        painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, palette().threed_highlight(), 1);
        y_offset += 1;
    }

    if (mode() == Year && m_show_month_tiles) {
        int i = 0;
        for (int j = 0; j < 3; j++) {
            x_offset = 0;
            for (int k = 0; k < 4; k++) {
                if (k > 0)
                    x_offset += m_months[i - 1].width;
                auto month_tile_rect = Gfx::IntRect(
                    x_offset,
                    y_offset,
                    m_months[i].width,
                    m_months[i].height);
                m_months[i].rect = month_tile_rect.translated(frame_thickness(), frame_thickness());
                Gfx::StylePainter::paint_button(
                    painter, month_tile_rect, palette(),
                    Gfx::ButtonStyle::Normal,
                    m_months[i].is_being_pressed,
                    m_months[i].is_hovered,
                    false, true, false);
                set_font(*small_font);
                painter.draw_text(month_tile_rect, m_months[i].name, font(), Gfx::TextAlignment::Center, palette().base_text());
                i++;
            }
            y_offset += m_months[i - 1].height;
        }
        return;
    }

    if (is_showing_days_of_the_week()) {
        auto days_of_the_week_rect = Gfx::IntRect(
            0,
            y_offset,
            frame_inner_rect().width(),
            16);
        painter.fill_rect(days_of_the_week_rect, palette().hover_highlight());
        for (int i = 0; i < 7; i++) {
            if (i > 0)
                x_offset += m_days[i - 1].width + 1;
            Gfx::IntRect day_rect = Gfx::IntRect(
                x_offset,
                y_offset,
                m_days[i].width,
                16);
            auto const& day_name = m_days[(i + to_underlying(m_first_day_of_week)) % 7].name;
            painter.draw_text(day_rect, day_name, small_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        }
        y_offset += days_of_the_week_rect.height();
        painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, palette().threed_shadow2(), 1);
        y_offset += 1;
    }

    if (mode() == Month) {
        int i = 0;
        for (int j = 0; j < 6; j++) {
            x_offset = 0;
            if (j > 0)
                y_offset += m_tiles[0][(j - 1) * 7].height + 1;
            for (int k = 0; k < 7; k++) {
                if (k > 0)
                    x_offset += m_tiles[0][k - 1].width + 1;
                auto tile_rect = Gfx::IntRect(
                    x_offset,
                    y_offset,
                    m_tiles[0][i].width,
                    m_tiles[0][i].height);
                m_tiles[0][i].rect = tile_rect.translated(frame_thickness(), frame_thickness());

                paint_tile(painter, m_tiles[0][i], tile_rect, x_offset, y_offset, k);

                i++;
            }
        }
    } else {
        for (int i = 0; i < 4; i++) {
            static int x_month_offset;
            x_month_offset += (i > 0 ? m_month_size[i - 1].width() + 1 : 0);
            auto month_rect = Gfx::IntRect(
                x_month_offset,
                y_offset,
                m_month_size[i].width(),
                19);
            painter.fill_rect(month_rect, palette().hover_highlight());
            painter.draw_text(month_rect, long_month_names[i], medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
            if (i > 0 && i < 4) {
                painter.draw_line({ x_month_offset - 1, y_offset - 1 }, { x_month_offset - 1, y_offset + 18 }, palette().threed_shadow2(), 1);
                painter.draw_line({ x_month_offset, y_offset - 1 }, { x_month_offset, y_offset + 18 }, palette().threed_highlight(), 1);
            }
            if (i == 3)
                x_month_offset = 0;
        }
        y_offset += 19;
        painter.draw_line({ 0, y_offset }, { frame_inner_rect().width(), y_offset }, palette().threed_shadow2(), 1);
        y_offset += 1;

        int x_translation = 0;
        int y_translation = y_offset;
        for (int l = 0; l < 12; l++) {
            if ((l > 0 && l < 4) || (l > 4 && l < 8) || (l > 8)) {
                x_translation += m_month_size[l - 1].width() + 1;
            } else if (l % 4 == 0) {
                x_translation = 0;
            }
            if (l < 4 || (l > 4 && l < 8) || l > 8) {
                y_offset = y_translation;
            } else if (l == 4 || l == 8) {
                y_translation += m_month_size[l - 1].height();
                painter.draw_line({ 0, y_translation }, { frame_inner_rect().width(), y_translation }, palette().threed_shadow1(), 1);
                y_translation += 1;
                painter.draw_line({ 0, y_translation }, { frame_inner_rect().width(), y_translation }, palette().threed_highlight(), 1);
                y_translation += 1;
                y_offset = y_translation;
                for (int i = l; i < (l == 4 ? 8 : 12); i++) {
                    static int x_month_offset;
                    x_month_offset += (i > (l == 4 ? 4 : 8) ? m_month_size[i - 1].width() + 1 : 0);
                    auto month_rect = Gfx::IntRect(
                        x_month_offset,
                        y_offset,
                        m_month_size[i].width(),
                        19);
                    painter.fill_rect(month_rect, palette().hover_highlight());
                    painter.draw_text(month_rect, long_month_names[i], medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
                    if (i > (l == 4 ? 4 : 8) && i < (l == 4 ? 8 : 12)) {
                        painter.draw_line({ x_month_offset - 1, y_offset - 1 }, { x_month_offset - 1, y_offset + 18 }, palette().threed_shadow2(), 1);
                        painter.draw_line({ x_month_offset, y_offset - 1 }, { x_month_offset, y_offset + 18 }, palette().threed_highlight(), 1);
                    }
                    if (i == 7 || i == 11)
                        x_month_offset = 0;
                }
                y_translation += 19;
                painter.draw_line({ 0, y_translation }, { frame_inner_rect().width(), y_translation }, palette().threed_shadow2(), 1);
                y_translation += 1;
                y_offset = y_translation;
            }

            int i = 0;
            for (int j = 0; j < 6; j++) {
                x_offset = 0;
                if (j > 0)
                    y_offset += m_tiles[l][(j - 1) * 7].height + (j < 6 ? 1 : 0);
                if (j == 0 && l != 3 && l != 7 && l != 11) {
                    painter.draw_line(
                        { m_month_size[l].width() + x_translation, y_offset },
                        { m_month_size[l].width() + x_translation, y_offset + m_month_size[l].height() },
                        palette().threed_shadow2(),
                        1);
                }
                for (int k = 0; k < 7; k++) {
                    if (k > 0)
                        x_offset += m_tiles[l][k - 1].width + 1;
                    auto tile_rect = Gfx::IntRect(
                        x_offset + x_translation,
                        y_offset,
                        m_tiles[l][i].width,
                        m_tiles[l][i].height);
                    m_tiles[l][i].rect = tile_rect.translated(frame_thickness(), frame_thickness());

                    paint_tile(painter, m_tiles[l][i], tile_rect, x_offset, y_offset, k);

                    i++;
                }
            }
        }
    }
}

void Calendar::paint_tile(GUI::Painter& painter, GUI::Calendar::Tile& tile, Gfx::IntRect& tile_rect, int x_offset, int y_offset, int day_offset)
{
    int width = unadjusted_tile_size().width();
    int height = unadjusted_tile_size().height();

    if (mode() == Month) {
        bool is_weekend = is_day_in_weekend((DayOfWeek)((day_offset + to_underlying(m_first_day_of_week)) % 7));

        Color background_color = palette().base();

        if (tile.is_hovered || tile.is_selected) {
            background_color = palette().hover_highlight();
        } else if (is_weekend) {
            background_color = palette().gutter();
        }

        painter.fill_rect(tile_rect, background_color);

        auto text_alignment = Gfx::TextAlignment::TopRight;
        auto text_rect = Gfx::IntRect(
            x_offset,
            y_offset + 4,
            tile.width - 4,
            font().pixel_size_rounded_up() + 4);

        if (width > 150 && height > 150) {
            set_font(*extra_large_font);
        } else if (width > 100 && height > 100) {
            set_font(*large_font);
        } else if (width > 50 && height > 50) {
            set_font(*medium_font);
        } else if (width >= 30 && height >= 30) {
            set_font(*small_font);
        } else {
            set_font(*small_font);
            text_alignment = Gfx::TextAlignment::Center;
            text_rect = Gfx::IntRect(tile_rect);
        }

        auto display_date = ByteString::number(tile.day);
        if (tile.is_selected && (width < 30 || height < 30))
            painter.draw_rect(tile_rect, palette().base_text());

        if (tile.is_today && !tile.is_outside_selected_month) {
            painter.draw_text(text_rect, display_date, font().bold_variant(), text_alignment, palette().base_text());
        } else if (tile.is_outside_selected_month) {
            painter.draw_text(text_rect, display_date, tile.is_today ? font().bold_variant() : font(), text_alignment, Color::LightGray);
        } else {
            painter.draw_text(text_rect, display_date, font(), text_alignment, palette().base_text());
        }
    } else {
        if (tile.is_hovered || tile.is_selected)
            painter.fill_rect(tile_rect, palette().hover_highlight());
        else
            painter.fill_rect(tile_rect, palette().base());

        if (width > 50 && height > 50) {
            set_font(*medium_font);
        } else {
            set_font(*small_font);
        }

        auto display_date = ByteString::number(tile.day);
        if (tile.is_selected)
            painter.draw_rect(tile_rect, palette().base_text());

        if (tile.is_today && !tile.is_outside_selected_month) {
            painter.draw_text(tile_rect, display_date, font().bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        } else if (!tile.is_outside_selected_month) {
            painter.draw_text(tile_rect, display_date, font(), Gfx::TextAlignment::Center, palette().base_text());
        }
    }
}

void Calendar::leave_event(Core::Event&)
{
    int months;
    mode() == Month ? months = 1 : months = 12;
    for (int i = 0; i < months; i++) {
        if (mode() == Year && m_show_month_tiles) {
            m_months[i].is_hovered = false;
            continue;
        } else {
            for (int j = 0; j < 42; j++) {
                m_tiles[i][j].is_hovered = false;
            }
        }
    }
    update();
}

void Calendar::mousemove_event(GUI::MouseEvent& event)
{
    static int last_index_i;
    static int last_index_j;

    if (mode() == Year && m_show_month_tiles) {
        if (m_months[last_index_i].rect.contains(event.position()) && (m_months[last_index_i].is_hovered || m_months[last_index_i].is_being_pressed)) {
            return;
        } else {
            m_months[last_index_i].is_hovered = false;
            m_months[last_index_i].is_being_pressed = false;
            update(m_months[last_index_i].rect);
        }
    } else {
        if (m_tiles[last_index_i][last_index_j].rect.contains(event.position()) && m_tiles[last_index_i][last_index_j].is_hovered) {
            return;
        } else {
            m_tiles[last_index_i][last_index_j].is_hovered = false;
            update(m_tiles[last_index_i][last_index_j].rect);
        }
    }

    int months;
    mode() == Month ? months = 1 : months = 12;
    for (int i = 0; i < months; i++) {
        if (mode() == Year && m_show_month_tiles) {
            if (m_months[i].rect.contains(event.position())) {
                if (m_currently_pressed_index == -1 || m_currently_pressed_index == i)
                    m_months[i].is_hovered = true;
                if (m_currently_pressed_index == i)
                    m_months[i].is_being_pressed = true;
                update(m_months[last_index_i].rect);
                if (m_months[i].is_being_pressed == true)
                    m_currently_pressed_index = i;
                last_index_i = i;
                update(m_months[i].rect);
                break;
            }
        } else {
            for (int j = 0; j < 42; j++) {
                if (mode() == Year && m_tiles[i][j].is_outside_selected_month)
                    continue;
                if (m_tiles[i][j].rect.contains(event.position())) {
                    m_tiles[i][j].is_hovered = true;
                    update(m_tiles[last_index_i][last_index_j].rect);
                    last_index_i = i;
                    last_index_j = j;
                    update(m_tiles[i][j].rect);
                    break;
                }
            }
        }
    }
}

void Calendar::mouseup_event(GUI::MouseEvent& event)
{
    int months;
    mode() == Month ? months = 1 : months = 12;
    for (int i = 0; i < months; i++) {
        if (mode() == Year && m_show_month_tiles) {
            if (m_months[i].rect.contains(event.position()) && m_months[i].is_being_pressed) {
                set_view_date(view_year(), (unsigned)i + 1);
                toggle_mode();
                if (on_month_click)
                    on_month_click();
            }
        } else {
            for (int j = 0; j < 42; j++) {
                if (mode() == Year && m_tiles[i][j].is_outside_selected_month)
                    continue;
                if (m_tiles[i][j].rect.contains(event.position())) {
                    m_previous_selected_date = m_selected_date;
                    m_selected_date = Core::DateTime::create(m_tiles[i][j].year, m_tiles[i][j].month, m_tiles[i][j].day);
                    update_tiles(m_selected_date.year(), m_selected_date.month());
                    if (on_tile_click)
                        on_tile_click();
                }
            }
        }
        if (months == 12) {
            m_months[i].is_being_pressed = false;
            m_months[i].is_hovered = false;
        }
    }
    m_currently_pressed_index = -1;
    update();
}

void Calendar::mousedown_event(GUI::MouseEvent& event)
{
    if (mode() == Year && m_show_month_tiles) {
        for (int i = 0; i < 12; i++) {
            if (m_months[i].rect.contains(event.position())) {
                m_months[i].is_being_pressed = true;
                m_currently_pressed_index = i;
                update(m_months[i].rect);
                break;
            }
        }
    }
}

void Calendar::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.wheel_delta_y() > 0)
        show_next_date();
    else
        show_previous_date();

    if (on_scroll)
        on_scroll();
}

void Calendar::doubleclick_event(GUI::MouseEvent& event)
{
    int months;
    mode() == Month ? months = 1 : months = 12;
    for (int i = 0; i < months; i++) {
        for (int j = 0; j < 42; j++) {
            if (m_tiles[i][j].day != m_previous_selected_date.day())
                continue;
            if (mode() == Year && m_tiles[i][j].is_outside_selected_month)
                continue;
            if (m_tiles[i][j].rect.contains(event.position())) {
                if (on_tile_doubleclick)
                    on_tile_doubleclick();
            }
        }
    }
}

size_t Calendar::day_of_week_index(ByteString const& day_name)
{
    auto const& day_names = AK::long_day_names;
    return AK::find_index(day_names.begin(), day_names.end(), day_name);
}

void Calendar::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain == "Calendar" && group == "View" && key == "FirstDayOfWeek") {
        m_first_day_of_week = static_cast<DayOfWeek>(day_of_week_index(value));
        update_tiles(m_view_year, m_view_month);
    } else if (domain == "Calendar" && group == "View" && key == "FirstDayOfWeekend") {
        m_first_day_of_weekend = static_cast<DayOfWeek>(day_of_week_index(value));
        update();
    }
}

void Calendar::config_i32_did_change(StringView domain, StringView group, StringView key, i32 value)
{
    if (domain == "Calendar" && group == "View" && key == "WeekendLength") {
        m_weekend_length = value;
        update();
    }
}

bool Calendar::is_day_in_weekend(DayOfWeek day)
{
    auto day_index = to_underlying(day);
    auto weekend_start_index = to_underlying(m_first_day_of_weekend);
    auto weekend_end_index = weekend_start_index + m_weekend_length;

    if (day_index < weekend_start_index)
        day_index += 7;

    return day_index < weekend_end_index;
}

ErrorOr<String> MonthListModel::column_name(int column) const
{
    switch (column) {
    case Column::Month:
        return "Month"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant MonthListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto const& month = (m_mode == MonthListModel::DisplayMode::Short ? AK::short_month_names : AK::long_month_names)[index.row()];
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Month:
            return month;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return {};
}

}
