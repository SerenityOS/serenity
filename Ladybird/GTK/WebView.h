#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWebView, ladybird_web_view, LADYBIRD, WEB_VIEW, GtkWidget)
#define LADYBIRD_TYPE_WEB_VIEW ladybird_web_view_get_type()

char const* ladybird_web_view_get_page_title(LadybirdWebView* self);
char const* ladybird_web_view_get_page_url(LadybirdWebView* self);
void ladybird_web_view_load_url(LadybirdWebView* self, char const* url);
char const* ladybird_web_view_get_hovered_link(LadybirdWebView* self);
bool ladybird_web_view_get_loading(LadybirdWebView* self);
GdkPaintable* ladybird_web_view_get_favicon(LadybirdWebView* self);

void ladybird_web_view_zoom_in(LadybirdWebView* self);
void ladybird_web_view_zoom_out(LadybirdWebView* self);
void ladybird_web_view_zoom_reset(LadybirdWebView* self);
guint ladybird_web_view_get_zoom_percent(LadybirdWebView* self);

char const* ladybird_web_view_get_prompt_text(LadybirdWebView* self);
void ladybird_web_view_alert_closed(LadybirdWebView* self);
void ladybird_web_view_confirm_closed(LadybirdWebView* self, bool confirmed);
void ladybird_web_view_prompt_closed(LadybirdWebView* self, char const* input);

void ladybird_web_view_scroll_by(LadybirdWebView* self, int page_x_delta, int page_y_delta);
void ladybird_web_view_scroll_to(LadybirdWebView* self, int page_x, int page_y);
void ladybird_web_view_scroll_into_view(LadybirdWebView* self, int page_x, int page_y, int page_width, int page_height);

namespace WebView {
class CookieJar;
}

WebView::CookieJar* ladybird_web_view_get_cookie_jar(LadybirdWebView* self);
void ladybird_web_view_set_cookie_jar(LadybirdWebView* self, WebView::CookieJar* cookie_jar);

GFile* ladybird_web_view_get_webdriver_content_ipc_path(LadybirdWebView* self);
void ladybird_web_view_set_webdriver_content_ipc_path(LadybirdWebView* self, GFile* webdriver_content_ipc_path);

G_END_DECLS
