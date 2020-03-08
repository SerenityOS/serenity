#include "CalendarWidget.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>

CalendarWidget::CalendarWidget()
{
    set_fill_with_background_color(true);
    m_calendar = new Calendar(Calendar::get_year(), Calendar::get_month());

    m_selected_date_label = add<GUI::Label>();
    m_selected_date_label->set_relative_rect(10, 13, 100, 25);
    m_selected_date_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    //TODO: Change text to a bigger size.
    m_selected_date_label->set_font(Gfx::Font::default_bold_font());
    m_selected_date_label->set_text(m_calendar->get_selected_date_text());

    m_prev_month_button = add<GUI::Button>();
    m_prev_month_button->set_text("<");
    m_prev_month_button->set_font(Gfx::Font::default_bold_font());
    m_prev_month_button->set_relative_rect(90, 5, 40, 40);
    m_prev_month_button->on_click = [&] {
        int m_target_month = m_calendar->get_selected_month() - 1;
        int m_target_year = m_calendar->get_selected_year();

        if (m_calendar->get_selected_month() <= 1) {
            m_target_month = 12;
            m_target_year--;
        }
        update_calendar_tiles(m_target_month, m_target_year);
    };

    m_next_month_button = add<GUI::Button>();
    m_next_month_button->set_text(">");
    m_next_month_button->set_font(Gfx::Font::default_bold_font());
    m_next_month_button->set_relative_rect(131, 5, 40, 40);
    m_next_month_button->on_click = [&] {
        int m_target_month = m_calendar->get_selected_month() + 1;
        int m_target_year = m_calendar->get_selected_year();

        if (m_calendar->get_selected_month() >= 12) {
            m_target_month = 1;
            m_target_year++;
        }
        update_calendar_tiles(m_target_month, m_target_year);
    };

    m_prev_month_button = add<GUI::Button>();
    m_prev_month_button->set_text("Add Event");
    m_prev_month_button->set_relative_rect(475, 13, 100, 25);
    m_prev_month_button->on_click = [] {
        //TODO: Open a dialoge/window for adding events
        dbg() << "TODO: Implement events";
    };

    update_calendar_tiles(m_calendar->get_selected_month(), m_calendar->get_selected_year());
}

CalendarWidget::~CalendarWidget()
{
}

void CalendarWidget::update_calendar_tiles(int target_month, int target_year)
{
    int i = 0;
    //TODO: Add another row if the end of the month doesn't fit onto the current tile array.
    for (int y = 0; y < 5; y++)
        for (int x = 0; x < 7; x++) {
            int x_offset = x * 85;
            int y_offset = (y * 85) + 50;

            int start_of_month = Core::DateTime::day_of_week(target_year, target_month, 1);
            int year;
            int month;
            int day;

            if (start_of_month > i) {
                month = target_month - 1;
                year = (month == 12) ? target_year - 1 : target_year;
                day = Core::DateTime::get_number_of_days_in_month(target_year, month) - (start_of_month - 1) + i;

            } else if ((i - start_of_month) + 1 > Core::DateTime::get_number_of_days_in_month(target_year, target_month)) {
                month = (target_month + 1) > 12 ? 1 : target_month + 1;
                year = (month == 1) ? target_year + 1 : target_year;
                day = ((i - start_of_month) + 1) - Core::DateTime::get_number_of_days_in_month(target_year, target_month);
            } else {
                year = target_year;
                month = target_month;
                day = (i - start_of_month) + 1;
            }

            if (!m_calendar_tiles[i]) {
                m_calendar_tiles[i] = add<CalendarTile>(m_calendar, i, year, month, day);
                m_calendar_tiles[i]->set_layout<GUI::HorizontalBoxLayout>();
                m_calendar_tiles[i]->set_relative_rect(x_offset, y_offset, 85, 85);
            } else {
                //TODO: Try to just call another consturctor. adopt(*new CalendarTile(m_calendar, i, year, month, day)) doesn't seeem to work
                m_calendar_tiles[i]->update_values(m_calendar, i, year, month, day);
                m_calendar_tiles[i]->update();
            }
            i++;
        }

    m_calendar->set_selected_date(target_year, target_month);
    m_selected_date_label->set_text(m_calendar->get_selected_date_text());
}

CalendarWidget::CalendarTile::CalendarTile(Calendar* calendar, int index, int year, int month, int day)
    : m_index(index)
    , m_day(day)
    , m_month(month)
    , m_year(year)
    , m_calendar(calendar)
{
    update_values(calendar, index, year, month, day);
}

void CalendarWidget::CalendarTile::update_values(Calendar* calendar, int index, int year, int month, int day)
{
    m_calendar = calendar;
    m_index = index;
    m_year = year;
    m_month = month;
    m_day = day;
    m_display_weekday_name = index < 7;

    if (m_display_weekday_name) {
        switch (day) {
        case 1:
            m_weekday_name = "Sun";
            break;
        case 2:
            m_weekday_name = "Mon";
            break;
        case 3:
            m_weekday_name = "Tue";
            break;
        case 4:
            m_weekday_name = "Wed";
            break;
        case 5:
            m_weekday_name = "Thu";
            break;
        case 6:
            m_weekday_name = "Fri";
            break;
        case 7:
            m_weekday_name = "Sat";
            break;
        default:
            break;
        }
    }

    m_display_date = (m_day == 1) ? String::format("%s %d", Calendar::get_name_of_month(m_month).characters(), m_day) : String::number(m_day);
}

CalendarWidget::CalendarTile::~CalendarTile()
{
}

void CalendarWidget::CalendarTile::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::NamedColor::White);

    Gfx::Rect day_rect;

    if (m_display_weekday_name) {
        Gfx::Rect weekday_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        weekday_rect.set_top(frame_inner_rect().y() + 4);
        painter.draw_text(weekday_rect, m_weekday_name, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, Color::Black);

        day_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        day_rect.set_top(frame_inner_rect().y() + 15);
    } else {
        day_rect = frame_inner_rect().shrunken(0, frame_inner_rect().height() / 1.2);
        day_rect.set_top(frame_inner_rect().y() + 4);
    }

    if (Calendar::is_today(m_year, m_month, m_day)) {
        Gfx::Rect highlight_rect = day_rect.shrunken(day_rect.width() / 1.25, 0);
        painter.draw_rect(highlight_rect, Color::NamedColor::Blue);
        painter.draw_text(day_rect, m_display_date, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, Color::Black);
    } else
        painter.draw_text(day_rect, m_display_date, Gfx::TextAlignment::Center, Color::Black);
}
