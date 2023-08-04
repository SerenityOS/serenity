#pragma once

#include "WebView.h"
#include <LibWebView/ViewImplementation.h>

class LadybirdViewImpl final
    : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<LadybirdViewImpl>> create(LadybirdWebView* widget);
    virtual ~LadybirdViewImpl() override;

    void set_viewport_rect(int x, int y, int width, int height);
    void scale_factor_changed();

    void mouse_down(int x, int y, unsigned button, unsigned buttons, unsigned modifiers);
    void mouse_move(int x, int y, unsigned buttons, unsigned modifiers);
    void mouse_up(int x, int y, unsigned button, unsigned buttons, unsigned modifiers);

private:
    LadybirdViewImpl(LadybirdWebView* widget);

    virtual void notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize content_size) override;
    virtual void notify_server_did_paint(Badge<WebView::WebContentClient>, i32 bitmap_id, Gfx::IntSize) override;
    virtual void notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_change_selection(Badge<WebView::WebContentClient>) override;
    virtual void notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor cursor) override;
    virtual void notify_server_did_request_scroll(Badge<WebView::WebContentClient>, i32, i32) override;
    virtual void notify_server_did_request_scroll_to(Badge<WebView::WebContentClient>, Gfx::IntPoint) override;
    virtual void notify_server_did_request_scroll_into_view(Badge<WebView::WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const&) override;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>) override;
    virtual void notify_server_did_request_alert(Badge<WebView::WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_confirm(Badge<WebView::WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_prompt(Badge<WebView::WebContentClient>, String const& message, String const& default_) override;
    virtual void notify_server_did_request_set_prompt_text(Badge<WebView::WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_accept_dialog(Badge<WebView::WebContentClient>) override;
    virtual void notify_server_did_request_dismiss_dialog(Badge<WebView::WebContentClient>) override;
    virtual void notify_server_did_request_file(Badge<WebView::WebContentClient>, DeprecatedString const& path, i32) override;
    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) override;

    virtual void update_zoom() override;
    virtual Gfx::IntRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    virtual void create_client(WebView::EnableCallgrindProfiling) override;

    void update_theme();

    Gfx::IntRect m_viewport_rect;
    LadybirdWebView* m_widget { nullptr };
    gulong m_update_style_id { 0 };
};
