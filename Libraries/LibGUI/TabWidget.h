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

    int active_tab_index() const;

    Widget* active_widget() { return m_active_widget.ptr(); }
    const Widget* active_widget() const { return m_active_widget.ptr(); }
    void set_active_widget(Widget*);

    int bar_height() const { return m_bar_visible ? 21 : 0; }

    int container_padding() const { return m_container_padding; }
    void set_container_padding(int padding) { m_container_padding = padding; }

    void add_widget(const StringView&, Widget&);
    void remove_widget(Widget&);

    template<class T, class... Args>
    T& add_tab(const StringView& title, Args&&... args)
    {
        auto t = T::construct(forward<Args>(args)...);
        add_widget(title, *t);
        return *t;
    }

    void remove_tab(Widget& tab) { remove_widget(tab); }

    void set_tab_title(Widget& tab, const StringView& title);
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

    Function<void(Widget&)> on_change;
    Function<void(Widget&)> on_middle_click;
    Function<void(Widget&, const ContextMenuEvent&)> on_context_menu_request;

protected:
    TabWidget();

    virtual void paint_event(PaintEvent&) override;
    virtual void child_event(Core::ChildEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

private:
    Gfx::IntRect child_rect_for_size(const Gfx::IntSize&) const;
    Gfx::IntRect button_rect(int index) const;
    Gfx::IntRect bar_rect() const;
    Gfx::IntRect container_rect() const;
    void update_bar();

    RefPtr<Widget> m_active_widget;

    struct TabData {
        int width(const Gfx::Font&) const;
        String title;
        RefPtr<Gfx::Bitmap> icon;
        Widget* widget { nullptr };
    };
    Vector<TabData> m_tabs;
    TabPosition m_tab_position { TabPosition::Top };
    int m_hovered_tab_index { -1 };
    int m_container_padding { 2 };
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    bool m_uniform_tabs { false };
    bool m_bar_visible { true };
};

}
