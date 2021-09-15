/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

class Frame : public Widget {
    C_OBJECT(Frame)
public:
    virtual ~Frame() override;

    int frame_thickness() const { return m_thickness; }
    void set_frame_thickness(int thickness);

    virtual Margins content_margins() const override { return { frame_thickness() }; }

    Gfx::FrameShadow frame_shadow() const { return m_shadow; }
    void set_frame_shadow(Gfx::FrameShadow shadow) { m_shadow = shadow; }

    Gfx::FrameShape frame_shape() const { return m_shape; }
    void set_frame_shape(Gfx::FrameShape shape) { m_shape = shape; }

    Gfx::IntRect frame_inner_rect_for_size(const Gfx::IntSize& size) const { return { m_thickness, m_thickness, size.width() - m_thickness * 2, size.height() - m_thickness * 2 }; }
    Gfx::IntRect frame_inner_rect() const { return frame_inner_rect_for_size(size()); }

    virtual Gfx::IntRect children_clip_rect() const override;

protected:
    Frame();
    void paint_event(PaintEvent&) override;

private:
    int m_thickness { 0 };
    Gfx::FrameShadow m_shadow { Gfx::FrameShadow::Plain };
    Gfx::FrameShape m_shape { Gfx::FrameShape::NoFrame };
};

}
