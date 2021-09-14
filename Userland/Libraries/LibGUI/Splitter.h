/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

    int first_resizee_minimum_size() { return m_first_resizee_minimum_size; }
    void set_first_resizee_minimum_size(int minimum_size) { m_first_resizee_minimum_size = minimum_size; }
    int second_resizee_minimum_size() { return m_second_resizee_minimum_size; }
    void set_second_resizee_minimum_size(int minimum_size) { m_second_resizee_minimum_size = minimum_size; }

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
    void override_cursor(bool do_override);
    Gfx::IntRect rect_between_widgets(GUI::Widget const& first_widget, GUI::Widget const& second_widget, bool honor_grabbable_margins) const;

    Gfx::Orientation m_orientation;
    bool m_resizing { false };
    bool m_overriding_cursor { false };
    Gfx::IntPoint m_resize_origin;
    WeakPtr<Widget> m_first_resizee;
    WeakPtr<Widget> m_second_resizee;
    Gfx::IntSize m_first_resizee_start_size;
    Gfx::IntSize m_second_resizee_start_size;
    int m_first_resizee_minimum_size { 0 };
    int m_second_resizee_minimum_size { 0 };

    void recompute_grabbables();

    struct Grabbable {
        // Index in m_grabbables, for convenience.
        size_t index { 0 };

        // The full grabbable rect, includes the content margin of adjacent elements.
        Gfx::IntRect grabbable_rect;
        // The rect used for painting. Does not include content margins.
        Gfx::IntRect paint_rect;

        WeakPtr<Widget> first_widget;
        WeakPtr<Widget> second_widget;
    };

    Grabbable* grabbable_at(Gfx::IntPoint const&);
    void set_hovered_grabbable(Grabbable*);

    Vector<Grabbable> m_grabbables;
    Optional<size_t> m_hovered_index;
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
