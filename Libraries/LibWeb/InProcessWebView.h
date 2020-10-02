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
#include <LibWeb/Page/Page.h>
#include <LibWeb/WebViewHooks.h>

namespace Web {

class InProcessWebView final
    : public GUI::ScrollableWidget
    , public WebViewHooks
    , public PageClient {
    C_OBJECT(InProcessWebView);

public:
    virtual ~InProcessWebView() override;

    void load_html(const StringView&, const URL&);
    void load_empty_document();

    DOM::Document* document();
    const DOM::Document* document() const;

    void set_document(DOM::Document*);

    const LayoutDocument* layout_root() const;
    LayoutDocument* layout_root();

    void reload();
    bool load(const URL&);

    URL url() const;

    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    virtual bool accepts_focus() const override { return true; }

    GUI::Action& select_all_action() { return *m_select_all_action; }
    GUI::Action& copy_action() { return *m_copy_action; }

    String selected_text() const;
    void select_all();

private:
    InProcessWebView();

    Page& page() { return *m_page; }
    const Page& page() const { return *m_page; }

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    virtual void did_scroll() override;

    // ^Web::PageClient
    virtual Gfx::Palette palette() const override { return GUI::ScrollableWidget::palette(); }
    virtual void page_did_change_title(const String&) override;
    virtual void page_did_set_document_in_main_frame(DOM::Document*) override;
    virtual void page_did_start_loading(const URL&) override;
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_request_context_menu(const Gfx::IntPoint&) override;
    virtual void page_did_request_link_context_menu(const Gfx::IntPoint&, const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_request_image_context_menu(const Gfx::IntPoint&, const URL&, const String& target, unsigned modifiers, const Gfx::Bitmap*) override;
    virtual void page_did_click_link(const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_middle_click_link(const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_enter_tooltip_area(const Gfx::IntPoint&, const String&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(const URL&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_invalidate(const Gfx::IntRect&) override;
    virtual void page_did_change_favicon(const Gfx::Bitmap&) override;
    virtual void page_did_layout() override;
    virtual void page_did_request_scroll_into_view(const Gfx::IntRect&) override;
    virtual void page_did_request_alert(const String&) override;

    void layout_and_sync_size();

    bool m_should_show_line_box_borders { false };

    NonnullOwnPtr<Page> m_page;

    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_select_all_action;
};

}
