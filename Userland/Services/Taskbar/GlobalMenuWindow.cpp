/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlobalMenuWindow.h"
#include <AK/Debug.h>
#include <AK/IterationDecision.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <stdio.h>

class MenuWidget final : public GUI::Widget {
    C_OBJECT(MenuWidget);

public:
    virtual ~MenuWidget() override = default;

private:
    MenuWidget() = default;

    virtual void paint_event(GUI::PaintEvent&) override
    {
        GUI::Painter painter(*this);
        painter.fill_rect({ 0, 0, width(), GlobalMenuWindow::global_menu_height() }, palette().button());
        painter.draw_line({ 0, height() - 1 }, { width() - 1, height() - 1 }, palette().threed_highlight());
    }
};

GlobalMenuWindow::GlobalMenuWindow()
{
    set_window_type(GUI::WindowType::GlobalMenu);
    set_title("Global Menu");

    auto const& rect = GUI::Desktop::the().rects()[GUI::Desktop::the().main_screen_index()];
    set_rect({ rect.x(), rect.y(), rect.width(), global_menu_height() });

    auto main_widget = set_main_widget<MenuWidget>();
    main_widget->set_layout<GUI::HorizontalBoxLayout>();
    main_widget->layout()->set_margins(GUI::Margins(0, 6, 1, 6));
    main_widget->set_height(GlobalMenuWindow::global_menu_height());

    m_global_menu_area_container = main_widget->add<GUI::Frame>();
    m_global_menu_area_container->set_layout<GUI::VerticalBoxLayout>();
    m_global_menu_area_container->set_frame_style(Gfx::FrameStyle::NoFrame);

    m_enabled = Config::read_bool("Taskbar"sv, "GlobalMenu"sv, "Enabled"sv, false);
    update_global_menu_enabled();
}

void GlobalMenuWindow::config_bool_did_change(StringView domain, StringView group, StringView key, bool value)
{
    if (domain == "Taskbar"sv && group == "GlobalMenu"sv) {
        if (key == "Enabled"sv) {
            m_enabled = value;
        }

        update_global_menu_enabled();
        update_global_menu_area();
    }
}

void GlobalMenuWindow::update_global_menu_enabled()
{
    if (m_enabled)
        show();
    else
        hide();

    GUI::ConnectionToWindowManagerServer::the().async_set_global_menu_area_enabled(m_enabled);
}
void GlobalMenuWindow::update_global_menu_area()
{
    if (!main_widget())
        return;

    m_global_menu_area_container->update();

    auto rect = m_global_menu_area_container->window_relative_rect().centered_within(main_widget()->rect());
    GUI::ConnectionToWindowManagerServer::the().async_set_global_menu_area_rect(rect.translated(-1, 0));
}

void GlobalMenuWindow::screen_rects_change_event(GUI::ScreenRectsChangeEvent& event)
{
    auto const& rect = event.rects()[event.main_screen_index()];
    set_rect({ rect.x(), rect.y(), rect.width(), global_menu_height() });
    update_global_menu_area();
}
