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

#include <LibGUI/Widget.h>

namespace GUI {

class Splitter : public Widget {
    C_OBJECT(Splitter);

public:
    virtual ~Splitter() override;

protected:
    explicit Splitter(Gfx::Orientation);

    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;

    virtual void did_layout() override;

private:
    void recompute_grabbable_rect(const Widget&, const Widget&);
    bool get_resize_candidates_at(const Gfx::IntPoint&, Widget*&, Widget*&);
    void override_cursor(bool do_override);

    Gfx::Orientation m_orientation;
    bool m_resizing { false };
    bool m_overriding_cursor { false };
    Gfx::IntPoint m_resize_origin;
    WeakPtr<Widget> m_first_resizee;
    WeakPtr<Widget> m_second_resizee;
    Gfx::IntSize m_first_resizee_start_size;
    Gfx::IntSize m_second_resizee_start_size;
    Gfx::IntRect m_grabbable_rect;
};

class VerticalSplitter final : public Splitter {
    C_OBJECT(VerticalSplitter)
public:
    virtual ~VerticalSplitter() override { }

private:
    VerticalSplitter()
        : Splitter(Gfx::Orientation::Vertical)
    {
    }
};

class HorizontalSplitter final : public Splitter {
    C_OBJECT(HorizontalSplitter)
public:
    virtual ~HorizontalSplitter() override { }

private:
    HorizontalSplitter()
        : Splitter(Gfx::Orientation::Horizontal)
    {
    }
};

}
