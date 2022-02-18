/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

void update_frame(RefPtr<Gfx::Bitmap>, unsigned num_cycles);

constexpr size_t DRAWTARGET_WIDTH = 500;
constexpr size_t DRAWTARGET_HEIGHT = 500;

class Demo final : public GUI::Widget {
    C_OBJECT(Demo)
public:
    virtual ~Demo() override;
    bool show_window_frame() const { return m_show_window_frame; }

    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

protected:
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        if (on_context_menu_request)
            on_context_menu_request(event);
    }

private:
    Demo();

    RefPtr<Gfx::Bitmap> m_bitmap;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    int m_cycles;
    bool m_show_window_frame { true };
};
