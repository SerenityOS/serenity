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

#include <AK/URL.h>
#include <LibGUI/ScrollableWidget.h>
#include <LibHTML/DOM/Document.h>

namespace Web {

class Frame;

class HtmlView : public GUI::ScrollableWidget {
    C_OBJECT(HtmlView)
public:
    virtual ~HtmlView() override;

    Document* document();
    const Document* document() const;
    void set_document(Document*);

    const LayoutDocument* layout_root() const;
    LayoutDocument* layout_root();

    Web::Frame& main_frame() { return *m_main_frame; }
    const Web::Frame& main_frame() const { return *m_main_frame; }

    void reload();
    void load(const URL&);
    void scroll_to_anchor(const StringView&);

    URL url() const;

    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Function<void(const String&)> on_link_click;
    Function<void(const String&)> on_link_hover;
    Function<void(const String&)> on_title_change;
    Function<void(const URL&)> on_load_start;

    virtual bool accepts_focus() const override { return true; }

protected:
    HtmlView();

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

private:
    virtual void did_scroll() override;

    void layout_and_sync_size();
    void dump_selection(const char* event_name);

    RefPtr<Web::Frame> m_main_frame;

    bool m_should_show_line_box_borders { false };
    bool m_in_mouse_selection { false };
};

}
