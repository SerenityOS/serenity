#include "CalendarWidget.h"
#include "AddEventDialog.h"
#include "Calendar.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>

CalendarWidget::CalendarWidget()
{
    set_fill_with_background_color(true);
    m_calendar = make<Calendar>(Core::DateTime::now());

    m_selected_date_label = add<GUI::Label>();
    m_selected_date_label->set_relative_rect(20, 13, 100, 25);
    m_selected_date_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_selected_date_label->set_font(Gfx::Font::default_bold_font());
    m_selected_date_label->set_text(m_calendar->selected_date_text());

    m_prev_month_button = add<GUI::Button>();
    m_prev_month_button->set_text("<");
    m_prev_month_button->set_font(Gfx::Font::default_bold_font());
    m_prev_month_button->set_relative_rect(90, 5, 40, 40);
    m_prev_month_button->on_click = [this] {
        int m_target_month = m_calendar->selected_month() - 1;
        int m_target_year = m_calendar->selected_year();

        if (m_calendar->selected_month() <= 1) {
            m_target_month = 12;
            m_target_year--;
        }
        update_calendar_tiles(m_target_year, m_target_month);
    };

    m_next_month_button = add<GUI::Button>();
    m_next_month_button->set_text(">");
    m_next_month_button->set_font(Gfx::Font::default_bold_font());
    m_next_month_button->set_relative_rect(131, 5, 40, 40);
    m_next_month_button->on_click = [this] {
        int m_target_month = m_calendar->selected_month() + 1;
        int m_target_year = m_calendar->selected_year();

        if (m_calendar->selected_month() >= 12) {
            m_target_month = 1;
            m_target_year++;
        }
        update_calendar_tiles(m_target_year, m_target_month);
    };

    m_add_event_button = add<GUI::Button>();
    m_add_event_button->set_text("Add Event");
    m_add_event_button->set_relative_rect(475, 13, 100, 25);
    m_add_event_button->on_click = [this] {
        AddEventDialog::show(m_calendar, window());
    };

    update_calendar_tiles(m_calendar->selected_year(), m_calendar->selected_month());
}

CalendarWidget::~CalendarWidget()
{
}

void CalendarWidget::update_calendar_tiles(int target_year, int target_month)
{
    unsigned int i = 0;
    //TODO: Modify m_tile_height if the end of the month doesn't fit onto the current tile array
    for (int y = 0; y < 5; y++)
        for (int x = 0; x < 7; x++) {
            auto date_time = Core::DateTime::create(target_year, target_month, 1);
            int x_offset = x * m_tile_width;
            int y_offset = (y * m_tile_height) + 50;

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
                m_calendar_tiles[i] = add<CalendarTile>(*m_calendar, i, date_time);
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

    m_display_date = (m_date_time.day() == 1) ? String::format("%s %d", name_of_month(m_date_time.month()).characters(), m_date_time.day()) : String::number(m_date_time.day());
}

CalendarWidget::CalendarTile::~CalendarTile()
{
}

void CalendarWidget::CalendarTile::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::NamedColor::White);

    painter.draw_line(frame_inner_rect().top_right(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);
    if (m_index == 0 || m_index % 7 == 0)
        painter.draw_line(frame_inner_rect().top_left(), frame_inner_rect().bottom_left(), Color::NamedColor::Black);

    if (m_index < 7)
        painter.draw_line(frame_inner_rect().top_left(), frame_inner_rect().top_right(), Color::NamedColor::Black);
    painter.draw_line(frame_inner_rect().bottom_left(), frame_inner_rect().bottom_right(), Color::NamedColor::Black);

    Gfx::Rect day_rect;

    if (m_display_weekday_name) {
        auto weekday_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        weekday_rect.set_top(frame_inner_rect().y() + 2);
        painter.draw_text(weekday_rect, m_weekday_name, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, Color::Black);

        day_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        day_rect.set_top(frame_inner_rect().y() + 15);
    } else {
        day_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        day_rect.set_top(frame_inner_rect().y() + 4);
    }

    if (m_calendar.is_today(m_date_time)) {
        auto highlight_rect = day_rect.shrunken(day_rect.width() - (font().glyph_width('x') * (m_display_date.length() + 1)) - 4, 0);
        painter.draw_rect(highlight_rect, Color::NamedColor::Blue);
        painter.draw_text(day_rect, m_display_date, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, Color::Black);
    } else
        painter.draw_text(day_rect, m_display_date, Gfx::TextAlignment::Center, Color::Black);
}
