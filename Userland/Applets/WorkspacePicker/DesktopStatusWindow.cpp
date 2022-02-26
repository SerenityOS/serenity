/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopStatusWindow.h"
#include <LibGUI/ConnectionToWindowMangerServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Palette.h>

class DesktopStatusWidget : public GUI::Widget {
    C_OBJECT(DesktopStatusWidget);

public:
    virtual ~DesktopStatusWidget() override = default;

    Gfx::IntRect rect_for_desktop(unsigned row, unsigned col) const
    {
        auto& desktop = GUI::Desktop::the();

        auto vcols = desktop.workspace_columns();
        auto vrows = desktop.workspace_rows();

        auto desktop_width = (width() - gap() * (vcols - 1)) / vcols;
        auto desktop_height = (height() - gap() * (vrows - 1)) / vrows;

        return {
            col * (desktop_width + gap()), row * (desktop_height + gap()),
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
            for (unsigned col = 0; col < desktop.workspace_columns(); ++col) {
                painter.fill_rect(rect_for_desktop(row, col),
                    (row == current_row() && col == current_col()) ? active_color : inactive_color);
            }
        }
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        auto base_rect = rect_for_desktop(0, 0);
        auto row = event.y() / (base_rect.height() + gap());
        auto col = event.x() / (base_rect.width() + gap());

        // Handle case where divider is clicked.
        if (rect_for_desktop(row, col).contains(event.position()))
            GUI::ConnectionToWindowMangerServer::the().async_set_workspace(row, col);
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        auto& desktop = GUI::Desktop::the();

        auto col = current_col();
        auto row = current_row();

        auto vcols = desktop.workspace_columns();
        auto vrows = desktop.workspace_rows();
        auto direction = event.wheel_delta_y() < 0 ? 1 : -1;

        if (event.modifiers() & Mod_Shift)
            col = abs((int)col + direction) % vcols;
        else
            row = abs((int)row + direction) % vrows;

        GUI::ConnectionToWindowMangerServer::the().async_set_workspace(row, col);
    }

    unsigned current_row() const { return m_current_row; }
    void set_current_row(unsigned row) { m_current_row = row; }
    unsigned current_col() const { return m_current_col; }
    void set_current_col(unsigned col) { m_current_col = col; }

    unsigned gap() const { return m_gap; }

private:
    DesktopStatusWidget() = default;

    unsigned m_gap { 1 };

    unsigned m_current_row { 0 };
    unsigned m_current_col { 0 };
};

DesktopStatusWindow::DesktopStatusWindow()
{
    GUI::Desktop::the().on_receive_screen_rects([&](GUI::Desktop&) {
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
        m_widget->set_current_col(changed_event.current_column());
        update();
    }
}
