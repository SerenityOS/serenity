/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Desktop.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/WebViewHooks.h>

namespace Web {

class InProcessWebView final
    : public GUI::AbstractScrollableWidget
    , public WebViewHooks
    , public PageClient {
    C_OBJECT(InProcessWebView);

public:
    virtual ~InProcessWebView() override;

    void load_html(StringView const&, URL const&);
    void load_empty_document();

    DOM::Document* document();
    const DOM::Document* document() const;

    void set_document(DOM::Document*);

    const Layout::InitialContainingBlockBox* layout_root() const;
    Layout::InitialContainingBlockBox* layout_root();

    void reload();
    bool load(URL const&);

    URL url() const;

    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    String selected_text() const;
    void select_all();

private:
    InProcessWebView();

    Page& page() { return *m_page; }
    Page const& page() const { return *m_page; }

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    virtual void did_scroll() override;

    // ^Web::PageClient
    virtual bool is_multi_process() const override { return false; }
    virtual Gfx::Palette palette() const override { return GUI::AbstractScrollableWidget::palette(); }
    virtual Gfx::IntRect screen_rect() const override { return GUI::Desktop::the().rect(); }
    virtual void page_did_change_title(String const&) override;
    virtual void page_did_set_document_in_top_level_browsing_context(DOM::Document*) override;
    virtual void page_did_start_loading(URL const&) override;
    virtual void page_did_finish_loading(URL const&) override;
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_request_context_menu(const Gfx::IntPoint&) override;
    virtual void page_did_request_link_context_menu(const Gfx::IntPoint&, URL const&, String const& target, unsigned modifiers) override;
    virtual void page_did_request_image_context_menu(const Gfx::IntPoint&, URL const&, String const& target, unsigned modifiers, const Gfx::Bitmap*) override;
    virtual void page_did_click_link(URL const&, String const& target, unsigned modifiers) override;
    virtual void page_did_middle_click_link(URL const&, String const& target, unsigned modifiers) override;
    virtual void page_did_enter_tooltip_area(const Gfx::IntPoint&, String const&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(URL const&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_invalidate(const Gfx::IntRect&) override;
    virtual void page_did_change_favicon(const Gfx::Bitmap&) override;
    virtual void page_did_layout() override;
    virtual void page_did_request_scroll_into_view(const Gfx::IntRect&) override;
    virtual void page_did_request_alert(String const&) override;
    virtual bool page_did_request_confirm(String const&) override;
    virtual String page_did_request_prompt(String const&, String const&) override;
    virtual String page_did_request_cookie(URL const&, Cookie::Source) override;
    virtual void page_did_set_cookie(URL const&, const Cookie::ParsedCookie&, Cookie::Source) override;

    void layout_and_sync_size();

    bool m_should_show_line_box_borders { false };

    NonnullOwnPtr<Page> m_page;
};

}
