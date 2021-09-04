/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Margins.h>
#include <LibGUI/Widget.h>

namespace GUI {

class TabWidget : public Widget {
    C_OBJECT(TabWidget)
public:
    enum TabPosition {
        Top,
        Bottom,
    };

    virtual ~TabWidget() override;

    TabPosition tab_position() const { return m_tab_position; }
    void set_tab_position(TabPosition);

    Optional<size_t> active_tab_index() const;

    Widget* active_widget() { return m_active_widget.ptr(); }
    Widget const* active_widget() const { return m_active_widget.ptr(); }
    void set_active_widget(Widget*);
    void set_tab_index(int);

    int bar_height() const { return m_bar_visible ? 21 : 0; }

    GUI::Margins const& container_margins() const { return m_container_margins; }
    void set_container_margins(GUI::Margins const&);

    void add_widget(StringView const&, Widget&);
    void remove_widget(Widget&);

    template<class T, class... Args>
    T& add_tab(StringView const& title, Args&&... args)
    {
        auto t = T::construct(forward<Args>(args)...);
        add_widget(title, *t);
        return *t;
    }

    void remove_tab(Widget& tab) { remove_widget(tab); }
    void remove_all_tabs_except(Widget& tab);

    void set_tab_title(Widget& tab, StringView const& title);
    void set_tab_icon(Widget& tab, const Gfx::Bitmap*);

    void activate_next_tab();
    void activate_previous_tab();

    void set_text_alignment(Gfx::TextAlignment alignment) { m_text_alignment = alignment; }
    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }

    bool uniform_tabs() const { return m_uniform_tabs; }
    void set_uniform_tabs(bool uniform_tabs) { m_uniform_tabs = uniform_tabs; }
    int uniform_tab_width() const;

    void set_bar_visible(bool bar_visible);
    bool is_bar_visible() const { return m_bar_visible; };

    void set_close_button_enabled(bool close_button_enabled) { m_close_button_enabled = close_button_enabled; };

    Function<void(size_t)> on_tab_count_change;
    Function<void(Widget&)> on_change;
    Function<void(Widget&)> on_middle_click;
    Function<void(Widget&)> on_tab_close_click;
    Function<void(Widget&, ContextMenuEvent const&)> on_context_menu_request;

protected:
    TabWidget();

    virtual void paint_event(PaintEvent&) override;
    virtual void child_event(Core::ChildEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

private:
    Gfx::IntRect child_rect_for_size(const Gfx::IntSize&) const;
    Gfx::IntRect button_rect(size_t index) const;
    Gfx::IntRect close_button_rect(size_t index) const;
    Gfx::IntRect bar_rect() const;
    Gfx::IntRect container_rect() const;
    void update_bar();
    void update_focus_policy();
    int bar_margin() const { return 2; }

    RefPtr<Widget> m_active_widget;

    struct TabData {
        int width(const Gfx::Font&) const;
        String title;
        RefPtr<Gfx::Bitmap> icon;
        Widget* widget { nullptr };
    };
    Vector<TabData> m_tabs;
    TabPosition m_tab_position { TabPosition::Top };
    Optional<size_t> m_hovered_tab_index;
    Optional<size_t> m_hovered_close_button_index;
    Optional<size_t> m_pressed_close_button_index;
    GUI::Margins m_container_margins { 2, 2, 2, 2 };
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    bool m_uniform_tabs { false };
    bool m_bar_visible { true };
    bool m_close_button_enabled { false };
};

}
