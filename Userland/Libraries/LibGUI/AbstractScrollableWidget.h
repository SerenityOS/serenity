/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGUI/Scrollbar.h>

namespace GUI {

class AbstractScrollableWidget : public Frame {
    C_OBJECT_ABSTRACT(AbstractScrollableWidget);

public:
    virtual ~AbstractScrollableWidget() override;

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

    Scrollbar& vertical_scrollbar() { return *m_vertical_scrollbar; }
    const Scrollbar& vertical_scrollbar() const { return *m_vertical_scrollbar; }
    Scrollbar& horizontal_scrollbar() { return *m_horizontal_scrollbar; }
    const Scrollbar& horizontal_scrollbar() const { return *m_horizontal_scrollbar; }
    Widget& corner_widget() { return *m_corner_widget; }
    const Widget& corner_widget() const { return *m_corner_widget; }

    void scroll_to_top();
    void scroll_to_bottom();

    void set_automatic_scrolling_timer(bool active);
    virtual Gfx::IntPoint automatic_scroll_delta_from_position(const Gfx::IntPoint&) const;

    int width_occupied_by_vertical_scrollbar() const;
    int height_occupied_by_horizontal_scrollbar() const;

    virtual Margins content_margins() const override;

    void set_should_hide_unnecessary_scrollbars(bool b) { m_should_hide_unnecessary_scrollbars = b; }
    bool should_hide_unnecessary_scrollbars() const { return m_should_hide_unnecessary_scrollbars; }

    Gfx::IntPoint to_content_position(const Gfx::IntPoint& widget_position) const;
    Gfx::IntPoint to_widget_position(const Gfx::IntPoint& content_position) const;

    Gfx::IntRect to_content_rect(const Gfx::IntRect& widget_rect) const { return { to_content_position(widget_rect.location()), widget_rect.size() }; }
    Gfx::IntRect to_widget_rect(const Gfx::IntRect& content_rect) const { return { to_widget_position(content_rect.location()), content_rect.size() }; }

protected:
    AbstractScrollableWidget();
    virtual void custom_layout() override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void did_scroll() { }
    void set_content_size(const Gfx::IntSize&);
    void set_size_occupied_by_fixed_elements(const Gfx::IntSize&);
    virtual void on_automatic_scrolling_timer_fired() {};
    int autoscroll_threshold() const { return m_autoscroll_threshold; }

private:
    class AbstractScrollableWidgetScrollbar final : public Scrollbar {
        C_OBJECT(AbstractScrollableWidgetScrollbar);

    private:
        explicit AbstractScrollableWidgetScrollbar(AbstractScrollableWidget& owner, Gfx::Orientation orientation)
            : Scrollbar(orientation)
            , m_owner(owner)
        {
        }

        virtual void mousewheel_event(MouseEvent& event) override
        {
            m_owner.handle_wheel_event(event, *this);
        }

        AbstractScrollableWidget& m_owner;
    };
    friend class ScrollableWidgetScrollbar;

    void update_scrollbar_ranges();
    void handle_wheel_event(MouseEvent&, Widget&);

    RefPtr<AbstractScrollableWidgetScrollbar> m_vertical_scrollbar;
    RefPtr<AbstractScrollableWidgetScrollbar> m_horizontal_scrollbar;
    RefPtr<Widget> m_corner_widget;
    Gfx::IntSize m_content_size;
    Gfx::IntSize m_size_occupied_by_fixed_elements;
    bool m_scrollbars_enabled { true };
    bool m_should_hide_unnecessary_scrollbars { false };

    RefPtr<Core::Timer> m_automatic_scrolling_timer;
    bool m_active_scrolling_enabled { false };
    int m_autoscroll_threshold { 20 };
};

}
