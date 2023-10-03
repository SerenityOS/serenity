/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class Splitter : public Widget {
    C_OBJECT(Splitter);

public:
    enum class OpportunisticResizee {
        First,
        Second
    };

    virtual ~Splitter() override = default;
    void set_opportunistic_resizee(OpportunisticResizee resizee) { m_opportunistic_resizee = resizee; }

protected:
    explicit Splitter(Gfx::Orientation);

    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;

    virtual void did_layout() override;
    virtual void custom_layout() override;

    OpportunisticResizee opportunistic_resizee() const { return m_opportunistic_resizee; }

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
    OpportunisticResizee m_opportunistic_resizee { OpportunisticResizee::Second };
    size_t m_last_child_count { 0 };
    int m_first_resizee_max_size { 0 };
    int m_second_resizee_max_size { 0 };

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

    Grabbable* grabbable_at(Gfx::IntPoint);
    void set_hovered_grabbable(Grabbable*);

    Vector<Grabbable> m_grabbables;
    Optional<size_t> m_hovered_index;
};

class VerticalSplitter final : public Splitter {
    C_OBJECT(VerticalSplitter)
public:
    virtual ~VerticalSplitter() override = default;

private:
    VerticalSplitter()
        : Splitter(Gfx::Orientation::Vertical)
    {
    }
};

class HorizontalSplitter final : public Splitter {
    C_OBJECT(HorizontalSplitter)
public:
    virtual ~HorizontalSplitter() override = default;

private:
    HorizontalSplitter()
        : Splitter(Gfx::Orientation::Horizontal)
    {
    }
};

}
