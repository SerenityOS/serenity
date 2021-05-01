/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
