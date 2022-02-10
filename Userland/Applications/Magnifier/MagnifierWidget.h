/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>

class MagnifierWidget final : public GUI::Frame {
    C_OBJECT(MagnifierWidget);

public:
    virtual ~MagnifierWidget() override = default;
    void set_scale_factor(int scale_factor);
    void set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter>);
    void pause_capture(bool pause)
    {
        m_pause_capture = pause;
        if (!pause)
            m_frame_offset_from_head = 0;
    }
    void display_previous_frame();
    void display_next_frame();

private:
    MagnifierWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;

    void sync();

    int m_scale_factor { 2 };
    OwnPtr<Gfx::ColorBlindnessFilter> m_color_filter;
    RefPtr<Gfx::Bitmap> m_grabbed_bitmap;
    CircularQueue<RefPtr<Gfx::Bitmap>, 512> m_grabbed_bitmaps {};
    ssize_t m_frame_offset_from_head { 0 };
    bool m_pause_capture { false };
};
