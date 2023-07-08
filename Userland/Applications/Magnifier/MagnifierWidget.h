/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <LibGUI/ColorFilterer.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>

class MagnifierWidget final
    : public GUI::Frame
    , public GUI::ColorFilterer {
    C_OBJECT(MagnifierWidget);

public:
    virtual ~MagnifierWidget() override = default;
    void set_scale_factor(int scale_factor);
    virtual void set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter>) override;
    void show_grid(bool);
    Gfx::Color grid_color() { return m_grid_color; }
    void set_grid_color(Gfx::Color);

    void pause_capture(bool pause)
    {
        m_pause_capture = pause;
        if (!pause)
            m_frame_offset_from_head = 0;
    }
    void lock_location(bool);
    void display_previous_frame();
    void display_next_frame();
    RefPtr<Gfx::Bitmap> current_bitmap() const { return m_grabbed_bitmap; }

    virtual Optional<GUI::UISize> calculated_min_size() const override
    {
        return GUI::UISize { frame_thickness() * 2 + m_scale_factor, frame_thickness() * 2 + m_scale_factor };
    }

private:
    MagnifierWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;

    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

    void sync();

    int m_scale_factor { 2 };
    OwnPtr<Gfx::ColorBlindnessFilter> m_color_filter;
    RefPtr<Gfx::Bitmap> m_grabbed_bitmap;
    CircularQueue<RefPtr<Gfx::Bitmap>, 512> m_grabbed_bitmaps {};
    ssize_t m_frame_offset_from_head { 0 };
    bool m_pause_capture { false };
    bool m_currently_dragging { false };
    Gfx::IntPoint m_last_drag_position {};
    Optional<Gfx::IntPoint> m_locked_location {};
    bool m_show_grid { false };
    Gfx::Color m_grid_color { 255, 0, 255, 100 };
};
