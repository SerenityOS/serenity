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

#include <LibGUI/Frame.h>
#include <LibGUI/ScrollBar.h>

namespace GUI {

class ScrollableWidget : public Frame {
    C_OBJECT(ScrollableWidget)
public:
    virtual ~ScrollableWidget() override;

    Gfx::IntSize content_size() const { return m_content_size; }
    int content_width() const { return m_content_size.width(); }
    int content_height() const { return m_content_size.height(); }

    Gfx::IntRect visible_content_rect() const;

    Gfx::IntRect widget_inner_rect() const;

    Gfx::IntRect viewport_rect_in_content_coordinates() const
    {
        auto viewport_rect = visible_content_rect();
        viewport_rect.set_size(widget_inner_rect().size());
        return viewport_rect;
    }

    void scroll_into_view(const Gfx::IntRect&, Orientation);
    void scroll_into_view(const Gfx::IntRect&, bool scroll_horizontally, bool scroll_vertically);

    void set_scrollbars_enabled(bool);
    bool is_scrollbars_enabled() const { return m_scrollbars_enabled; }

    Gfx::IntSize available_size() const;
    Gfx::IntSize excess_size() const;

    ScrollBar& vertical_scrollbar() { return *m_vertical_scrollbar; }
    const ScrollBar& vertical_scrollbar() const { return *m_vertical_scrollbar; }
    ScrollBar& horizontal_scrollbar() { return *m_horizontal_scrollbar; }
    const ScrollBar& horizontal_scrollbar() const { return *m_horizontal_scrollbar; }
    Widget& corner_widget() { return *m_corner_widget; }
    const Widget& corner_widget() const { return *m_corner_widget; }

    void scroll_to_top();
    void scroll_to_bottom();

    int width_occupied_by_vertical_scrollbar() const;
    int height_occupied_by_horizontal_scrollbar() const;

    void set_should_hide_unnecessary_scrollbars(bool b) { m_should_hide_unnecessary_scrollbars = b; }
    bool should_hide_unnecessary_scrollbars() const { return m_should_hide_unnecessary_scrollbars; }

    Gfx::IntPoint to_content_position(const Gfx::IntPoint& widget_position) const;
    Gfx::IntPoint to_widget_position(const Gfx::IntPoint& content_position) const;

    Gfx::IntRect to_content_rect(const Gfx::IntRect& widget_rect) const { return { to_content_position(widget_rect.location()), widget_rect.size() }; }
    Gfx::IntRect to_widget_rect(const Gfx::IntRect& content_rect) const { return { to_widget_position(content_rect.location()), content_rect.size() }; }

protected:
    ScrollableWidget();
    virtual void custom_layout() override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void did_scroll() { }
    void set_content_size(const Gfx::IntSize&);
    void set_size_occupied_by_fixed_elements(const Gfx::IntSize&);

private:
    class ScrollableWidgetScrollBar final : public ScrollBar {
        C_OBJECT(ScrollableWidgetScrollBar);

    protected:
        explicit ScrollableWidgetScrollBar(ScrollableWidget& owner, Gfx::Orientation orientation)
            : ScrollBar(orientation)
            , m_owner(owner)
        {
        }

        virtual void mousewheel_event(MouseEvent& event) override
        {
            m_owner.handle_wheel_event(event, *this);
        }

    private:
        ScrollableWidget& m_owner;
    };
    friend class ScrollableWidgetScrollBar;

    void update_scrollbar_ranges();
    void handle_wheel_event(MouseEvent&, Widget&);

    RefPtr<ScrollableWidgetScrollBar> m_vertical_scrollbar;
    RefPtr<ScrollableWidgetScrollBar> m_horizontal_scrollbar;
    RefPtr<Widget> m_corner_widget;
    Gfx::IntSize m_content_size;
    Gfx::IntSize m_size_occupied_by_fixed_elements;
    bool m_scrollbars_enabled { true };
    bool m_should_hide_unnecessary_scrollbars { false };
};

}
