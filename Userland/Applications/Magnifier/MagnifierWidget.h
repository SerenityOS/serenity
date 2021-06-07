/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGfx/Point.h>

class MagnifierWidget final : public GUI::Widget {
    C_OBJECT(MagnifierWidget)

public:
    MagnifierWidget();
    virtual ~MagnifierWidget();
    void set_scale_factor(int scale_factor);
    void track_cursor_globally();

private:
    virtual void paint_event(GUI::PaintEvent&) override;

    void sync();

    Gfx::IntPoint m_mouse_position;
    int m_scale_factor { 2 };
    int m_desktop_display_scale { 1 };
    RefPtr<Gfx::Bitmap> m_grabbed_bitmap;
};
