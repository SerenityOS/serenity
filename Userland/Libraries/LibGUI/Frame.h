/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
    virtual ~Frame() override = default;

    int frame_thickness() const;

    virtual Margins content_margins() const override { return { frame_thickness() }; }

    Gfx::FrameStyle frame_style() const { return m_style; }
    void set_frame_style(Gfx::FrameStyle);

    Gfx::IntRect frame_inner_rect_for_size(Gfx::IntSize size) const { return { frame_thickness(), frame_thickness(), size.width() - frame_thickness() * 2, size.height() - frame_thickness() * 2 }; }
    Gfx::IntRect frame_inner_rect() const { return frame_inner_rect_for_size(size()); }

    virtual Gfx::IntRect children_clip_rect() const override;

protected:
    Frame();
    void paint_event(PaintEvent&) override;

private:
    Gfx::FrameStyle m_style { Gfx::FrameStyle::NoFrame };
};

}
