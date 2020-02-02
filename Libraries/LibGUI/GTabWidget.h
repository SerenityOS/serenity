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

#include <LibGUI/GWidget.h>

class GTabWidget : public GWidget {
    C_OBJECT(GTabWidget)
public:
    enum TabPosition {
        Top,
        Bottom,
    };

    explicit GTabWidget(GWidget* parent);
    virtual ~GTabWidget() override;

    TabPosition tab_position() const { return m_tab_position; }
    void set_tab_position(TabPosition);

    int active_tab_index() const;

    GWidget* active_widget() { return m_active_widget.ptr(); }
    const GWidget* active_widget() const { return m_active_widget.ptr(); }
    void set_active_widget(GWidget*);

    int bar_height() const { return 21; }
    int container_padding() const { return 2; }

    void add_widget(const StringView&, GWidget*);

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void child_event(Core::ChildEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void leave_event(Core::Event&) override;

private:
    Rect child_rect_for_size(const Size&) const;
    Rect button_rect(int index) const;
    Rect bar_rect() const;
    Rect container_rect() const;
    void update_bar();

    RefPtr<GWidget> m_active_widget;

    struct TabData {
        Rect rect(const Font&) const;
        int width(const Font&) const;
        String title;
        GWidget* widget { nullptr };
    };
    Vector<TabData> m_tabs;
    TabPosition m_tab_position { TabPosition::Top };
    int m_hovered_tab_index { -1 };
};
