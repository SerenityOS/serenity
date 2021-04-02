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

#include <LibCore/DateTime.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Calendar);

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

static const auto extra_large_font = Gfx::BitmapFont::load_from_file("/res/fonts/LizaRegular36.font");
static const auto large_font = Gfx::BitmapFont::load_from_file("/res/fonts/LizaRegular24.font");
static const auto medium_font = Gfx::BitmapFont::load_from_file("/res/fonts/PebbletonRegular14.font");
static const auto small_font = Gfx::BitmapFont::load_from_file("/res/fonts/KaticaRegular10.font");

Calendar::Calendar(Core::DateTime date_time, Mode mode)
    : m_selected_date(date_time)
    , m_mode(mode)
{
    set_fill_with_background_color(true);

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

    update_tiles(m_selected_date.year(), m_selected_date.month());
}

Calendar::~Calendar()
{
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
    this->resize(this->height(), this->width());
    invalidate_layout();
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

        const int GRID_LINES = 6;
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

        const int VERT_GRID_LINES = 27;
        const int HORI_GRID_LINES = 15;
        const int THREADING = 3;
        const int MONTH_TITLE = 19;
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

    unsigned months;
    mode() == Month ? months = 1 : months = 12;
    for (unsigned i = 0; i < months; i++) {
        if (mode() == Year)
            view_month = i + 1;
        for (unsigned j = 0; j < 42; j++) {
            auto date_time = Core::DateTime::create(view_year, view_month, 1);
            unsigned start_of_month = date_time.weekday();
            unsigned year;
            unsigned month;
            unsigned day;

            if (start_of_month == 0 && mode() != Year) {
                month = (view_month - 1 == 0) ? 12 : view_month - 1;
                year = (month == 12) ? view_year - 1 : view_year;
                date_time.set_time(year, month, 1);
                day = (date_time.days_in_month() - 6 + j);
            } else if (start_of_month > j) {
                month = (view_month - 1 == 0) ? 12 : view_month - 1;
                year = (month == 12) ? view_year - 1 : view_year;
                date_time.set_time(year, month, 1);
                day = (date_time.days_in_month() - (start_of_month) + j) + 1;
            } else if ((j - start_of_month) + 1 > date_time.days_in_month()) {
                month = (view_month + 1) > 12 ? 1 : view_month + 1;
                year = (month == 1) ? view_year + 1 : view_year;
                day = ((j - start_of_month) + 1) - date_time.days_in_month();
            } else {
                month = view_month;
                year = view_year;
                day = (j - start_of_month) + 1;
            }
            date_time.set_time(year, month, day);

            m_tiles[i][j].date_time = date_time;
            m_tiles[i][j].is_outside_selected_month = (date_time.month() != view_month
                || date_time.year() != view_year);
            m_tiles[i][j].is_selected = (date_time.year() == m_selected_date.year()
                && date_time.month() == m_selected_date.month()
                && date_time.day() == m_selected_date.day()
                && (mode() == Year ? !m_tiles[i][j].is_outside_selected_month : true));
            m_tiles[i][j].is_today = (date_time.day() == Core::DateTime::now().day()
                && date_time.month() == Core::DateTime::now().month()
                && date_time.year() == Core::DateTime::now().year());
        }
    }
    update();
}

String Calendar::formatted_date(Format format)
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
    default:
        VERIFY_NOT_REACHED();
    }
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

    int width = unadjusted_tile_size().width();
    int height = unadjusted_tile_size().height();
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
        painter.draw_text(year_only_rect, formatted_date(YearOnly), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
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
        painter.draw_text(month_year_rect, formatted_date(MonthOnly), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
        month_year_rect.set_x(month_year_rect.width() + (frame_inner_rect().width() % 2 ? 1 : 0));
        painter.draw_text(month_year_rect, formatted_date(YearOnly), medium_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
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
                set_font(small_font);
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
            painter.draw_text(day_rect, m_days[i].name, small_font->bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
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
                if (m_tiles[0][i].is_hovered || m_tiles[0][i].is_selected)
                    painter.fill_rect(tile_rect, palette().hover_highlight());
                else
                    painter.fill_rect(tile_rect, palette().base());

                auto text_alignment = Gfx::TextAlignment::TopRight;
                auto text_rect = Gfx::IntRect(
                    x_offset,
                    y_offset + 4,
                    m_tiles[0][i].width,
                    font().glyph_height() + 4);

                if (width > 150 && height > 150) {
                    set_font(extra_large_font);
                } else if (width > 100 && height > 100) {
                    set_font(large_font);
                } else if (width > 50 && height > 50) {
                    set_font(medium_font);
                    text_rect.set_width(m_tiles[0][i].width - 4);
                } else if (width >= 30 && height >= 30) {
                    set_font(small_font);
                    text_rect.set_width(m_tiles[0][i].width - 4);
                } else {
                    set_font(small_font);
                    text_alignment = Gfx::TextAlignment::Center;
                    text_rect = Gfx::IntRect(tile_rect);
                }

                auto display_date = String::number(m_tiles[0][i].date_time.day());
                if (m_tiles[0][i].is_selected && (width < 30 || height < 30))
                    painter.draw_rect(tile_rect, palette().base_text());

                if (m_tiles[0][i].is_today && !m_tiles[0][i].is_outside_selected_month) {
                    painter.draw_text(text_rect, display_date, font().bold_variant(), text_alignment, palette().base_text());
                } else if (m_tiles[0][i].is_outside_selected_month) {
                    painter.draw_text(text_rect, display_date, m_tiles[0][i].is_today ? font().bold_variant() : font(), text_alignment, Color::LightGray);
                } else {
                    painter.draw_text(text_rect, display_date, font(), text_alignment, palette().base_text());
                }
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

                    if (m_tiles[l][i].is_hovered || m_tiles[l][i].is_selected)
                        painter.fill_rect(tile_rect, palette().hover_highlight());
                    else
                        painter.fill_rect(tile_rect, palette().base());

                    if (width > 50 && height > 50) {
                        set_font(medium_font);
                    } else {
                        set_font(small_font);
                    }

                    auto display_date = String::number(m_tiles[l][i].date_time.day());
                    if (m_tiles[l][i].is_selected)
                        painter.draw_rect(tile_rect, palette().base_text());

                    if (m_tiles[l][i].is_today && !m_tiles[l][i].is_outside_selected_month) {
                        painter.draw_text(tile_rect, display_date, font().bold_variant(), Gfx::TextAlignment::Center, palette().base_text());
                    } else if (!m_tiles[l][i].is_outside_selected_month) {
                        painter.draw_text(tile_rect, display_date, font(), Gfx::TextAlignment::Center, palette().base_text());
                    }
                    i++;
                }
            }
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
                    m_selected_date = m_tiles[i][j].date_time;
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

void Calendar::doubleclick_event(GUI::MouseEvent& event)
{
    int months;
    mode() == Month ? months = 1 : months = 12;
    for (int i = 0; i < months; i++) {
        for (int j = 0; j < 42; j++) {
            if (m_tiles[i][j].date_time.day() != m_previous_selected_date.day())
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
}
