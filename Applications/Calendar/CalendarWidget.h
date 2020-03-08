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
    void update_calendar_tiles(int target_month, int target_year);
    void show_add_event_window(Calendar* calendar);

    //TODO: Change to RefPtr or NonNullRefPtr...?
    Calendar* m_calendar;
    RefPtr<GUI::Label> m_selected_date_label;
    RefPtr<GUI::Button> m_prev_month_button;
    RefPtr<GUI::Button> m_next_month_button;

    class CalendarTile final : public GUI::Frame {
        C_OBJECT(CalendarTile)
    public:
        CalendarTile(Calendar* calendar, int index, int year, int month, int day);
        void update_values(Calendar* calendar, int index, int year, int month, int day);
        ~CalendarTile();

    private:
        virtual void paint_event(GUI::PaintEvent&) override;

        int m_index;
        int m_day;
        int m_month;
        int m_year;
        bool m_display_weekday_name;

        String m_weekday_name;
        String m_display_date;
        Calendar* m_calendar;
    };

    RefPtr<CalendarTile> m_calendar_tiles[35];
};
