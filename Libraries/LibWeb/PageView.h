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
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Forward.h>

namespace Web {

class PageView : public GUI::ScrollableWidget {
    C_OBJECT(PageView);

public:
    virtual ~PageView() override;

    // FIXME: Remove this once the new parser is ready.
    void set_use_old_parser(bool use_old_parser);

    void load_empty_document();

    Document* document();
    const Document* document() const;

    void set_document(Document*);

    const LayoutDocument* layout_root() const;
    LayoutDocument* layout_root();

    Web::Frame& main_frame() { return *m_main_frame; }
    const Web::Frame& main_frame() const { return *m_main_frame; }

    void reload();
    bool load(const URL&);
    void scroll_to_anchor(const StringView&);

    URL url() const;

    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Function<void(const String& href, const String& target, unsigned modifiers)> on_link_click;
    Function<void(const String& href, const Gfx::Point& screen_position)> on_link_context_menu_request;
    Function<void(const String& href)> on_link_middle_click;
    Function<void(const String&)> on_link_hover;
    Function<void(const String&)> on_title_change;
    Function<void(const URL&)> on_load_start;
    Function<void(const Gfx::Bitmap&)> on_favicon_change;
    Function<void(const URL&)> on_url_drop;
    Function<void(Document*)> on_set_document;

    virtual bool accepts_focus() const override { return true; }

    void notify_link_click(Badge<EventHandler>, Web::Frame&, const String& href, const String& target, unsigned modifiers);
    void notify_link_middle_click(Badge<EventHandler>, Web::Frame&, const String& href, const String& target, unsigned modifiers);
    void notify_link_context_menu_request(Badge<EventHandler>, Web::Frame&, const Gfx::Point& content_position, const String& href, const String& target, unsigned modifiers);
    void notify_link_hover(Badge<EventHandler>, Web::Frame&, const String& href);
    void notify_tooltip_area_enter(Badge<EventHandler>, Web::Frame&, const Gfx::Point& content_position, const String& title);
    void notify_tooltip_area_leave(Badge<EventHandler>, Web::Frame&);
    void notify_needs_display(Badge<Web::Frame>, Web::Frame&, const Gfx::Rect&);

protected:
    PageView();

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

private:
    virtual void did_scroll() override;

    Gfx::Point to_screen_position(const Web::Frame&, const Gfx::Point&) const;
    Gfx::Rect to_widget_rect(const Web::Frame&, const Gfx::Rect&) const;

    void layout_and_sync_size();

    RefPtr<Web::Frame> m_main_frame;

    bool m_should_show_line_box_borders { false };
};

}
