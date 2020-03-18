#pragma once

#include "Calendar.h"
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

class CalendarWidget final : public GUI::Widget {
    C_OBJECT(CalendarWidget)

public:
    CalendarWidget();
    virtual ~CalendarWidget() override;

private:
    void update_calendar_tiles(int target_year, int target_month);
    void show_add_event_window(Calendar* calendar);

    OwnPtr<Calendar> m_calendar;
    RefPtr<GUI::Label> m_selected_date_label;
    RefPtr<GUI::Button> m_prev_month_button;
    RefPtr<GUI::Button> m_next_month_button;
    RefPtr<GUI::Button> m_add_event_button;

    class CalendarTile final : public GUI::Frame {
        C_OBJECT(CalendarTile)
    public:
        CalendarTile(Calendar& calendar, int index, Core::DateTime m_date_time);
        void update_values(Calendar& calendar, int index, Core::DateTime date_time);
        virtual ~CalendarTile() override;

    private:
        virtual void paint_event(GUI::PaintEvent&) override;

        int m_index { 0 };
        bool m_display_weekday_name { false };

        String m_weekday_name;
        String m_display_date;
        Core::DateTime m_date_time;
        Calendar& m_calendar;
    };

    RefPtr<CalendarTile> m_calendar_tiles[35];
    int m_tile_width { 85 };
    int m_tile_height { 85 };
};
