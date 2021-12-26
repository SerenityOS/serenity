/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibDraw/StylePainter.h>
#include <LibGUI/GWidget.h>

namespace GUI {

class Frame : public Widget {
    C_OBJECT(Frame)
public:
    virtual ~Frame() override;

    int frame_thickness() const { return m_thickness; }
    void set_frame_thickness(int thickness) { m_thickness = thickness; }

    FrameShadow frame_shadow() const { return m_shadow; }
    void set_frame_shadow(FrameShadow shadow) { m_shadow = shadow; }

    FrameShape frame_shape() const { return m_shape; }
    void set_frame_shape(FrameShape shape) { m_shape = shape; }

    Rect frame_inner_rect_for_size(const Size& size) const { return { m_thickness, m_thickness, size.width() - m_thickness * 2, size.height() - m_thickness * 2 }; }
    Rect frame_inner_rect() const { return frame_inner_rect_for_size(size()); }

protected:
    explicit Frame(Widget* parent = nullptr);
    void paint_event(PaintEvent&) override;

private:
    int m_thickness { 0 };
    FrameShadow m_shadow { FrameShadow::Plain };
    FrameShape m_shape { FrameShape::NoFrame };
};

}
