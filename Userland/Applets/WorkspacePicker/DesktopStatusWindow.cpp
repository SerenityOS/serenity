/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopStatusWindow.h"
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Palette.h>

class DesktopStatusWidget : public GUI::Widget {
    C_OBJECT(DesktopStatusWidget);

public:
    virtual ~DesktopStatusWidget() override = default;

    Gfx::IntRect rect_for_desktop(unsigned row, unsigned column) const
    {
        auto& desktop = GUI::Desktop::the();

        auto workspace_columns = desktop.workspace_columns();
        auto workspace_rows = desktop.workspace_rows();

        auto desktop_width = (width() - gap() * (workspace_columns - 1)) / workspace_columns;
        auto desktop_height = (height() - gap() * (workspace_rows - 1)) / workspace_rows;

        return {
            column * (desktop_width + gap()), row * (desktop_height + gap()),
            desktop_width, desktop_height
        };
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Widget::paint_event(event);

        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.fill_rect({ 0, 0, width(), height() }, palette().button());

        auto& desktop = GUI::Desktop::the();

        auto active_color = palette().active_window_border1();
        auto inactive_color = palette().inactive_window_border1();

        for (unsigned row = 0; row < desktop.workspace_rows(); ++row) {
            for (unsigned column = 0; column < desktop.workspace_columns(); ++column) {
                painter.fill_rect(rect_for_desktop(row, column),
                    (row == current_row() && column == current_column()) ? active_color : inactive_color);
            }
        }
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        auto base_rect = rect_for_desktop(0, 0);
        auto row = event.y() / (base_rect.height() + gap());
        auto column = event.x() / (base_rect.width() + gap());

        // Handle case where divider is clicked.
        if (rect_for_desktop(row, column).contains(event.position()))
            GUI::ConnectionToWindowManagerServer::the().async_set_workspace(row, column);
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        auto& desktop = GUI::Desktop::the();

        auto column = current_column();
        auto row = current_row();

        auto workspace_columns = desktop.workspace_columns();
        auto workspace_rows = desktop.workspace_rows();
        auto direction = event.wheel_delta_y() < 0 ? 1 : -1;

        if (event.modifiers() & Mod_Shift)
            column = abs((int)column + direction) % workspace_columns;
        else
            row = abs((int)row + direction) % workspace_rows;

        GUI::ConnectionToWindowManagerServer::the().async_set_workspace(row, column);
    }

    unsigned current_row() const { return m_current_row; }
    void set_current_row(unsigned row) { m_current_row = row; }
    unsigned current_column() const { return m_current_column; }
    void set_current_column(unsigned column) { m_current_column = column; }

    unsigned gap() const { return m_gap; }

private:
    DesktopStatusWidget() = default;

    unsigned m_gap { 1 };

    unsigned m_current_row { 0 };
    unsigned m_current_column { 0 };
};

DesktopStatusWindow::DesktopStatusWindow()
{
    GUI::Desktop::the().on_receive_screen_rects([&](GUI::Desktop&) {
        auto& desktop = GUI::Desktop::the();
        if (desktop.workspace_rows() == 1 && desktop.workspace_columns() == 1)
            resize(0, 0);
        else
            resize(28, 16);

        update();
    });
    set_window_type(GUI::WindowType::Applet);
    set_has_alpha_channel(true);
    m_widget = &set_main_widget<DesktopStatusWidget>();
}

void DesktopStatusWindow::wm_event(GUI::WMEvent& event)
{
    if (event.type() == GUI::Event::WM_WorkspaceChanged) {
        auto& changed_event = static_cast<GUI::WMWorkspaceChangedEvent&>(event);
        m_widget->set_current_row(changed_event.current_row());
        m_widget->set_current_column(changed_event.current_column());
        update();
    }
}
