/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "TaskbarFrame.h"
#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <time.h>

namespace Taskbar {

class ClockWidget final : public TaskbarFrame {
    C_OBJECT(ClockWidget);

public:
    virtual ~ClockWidget() override = default;

    void update_format(ByteString const&);

private:
    ClockWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;

    void tick_clock()
    {
        tzset();
        update();
    }

    void open();
    void close();

    void position_calendar_window();
    void jump_to_current_date();

    void update_selected_calendar_button();

    ByteString m_time_format;
    RefPtr<GUI::Window> m_calendar_window;
    RefPtr<GUI::Calendar> m_calendar;
    RefPtr<GUI::Button> m_next_date;
    RefPtr<GUI::Button> m_prev_date;
    RefPtr<GUI::Button> m_selected_calendar_button;
    RefPtr<GUI::Button> m_jump_to_button;
    RefPtr<GUI::Button> m_calendar_launcher;
    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<Core::Timer> m_timer;
    int m_time_width { 0 };
    Gfx::IntSize m_window_size { 158, 186 };
};

}
