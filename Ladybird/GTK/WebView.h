#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWebView, ladybird_web_view, LADYBIRD, WEB_VIEW, GtkWidget)
#define LADYBIRD_TYPE_WEB_VIEW ladybird_web_view_get_type()

void ladybird_web_view_set_page_size(LadybirdWebView* self, int width, int height);

char const* ladybird_web_view_get_page_title(LadybirdWebView* self);
void ladybird_web_view_set_page_title(LadybirdWebView* self, char const* title);

char const* ladybird_web_view_get_page_url(LadybirdWebView* self);
void ladybird_web_view_set_page_url(LadybirdWebView* self, char const* url);

void ladybird_web_view_load_url(LadybirdWebView* self, char const* url);

char const* ladybird_web_view_get_hovered_link(LadybirdWebView* self);
void ladybird_web_view_set_hovered_link(LadybirdWebView* self, char const* hovered_link);

bool ladybird_web_view_get_loading(LadybirdWebView* self);
void ladybird_web_view_set_loading(LadybirdWebView* self, bool loading);

GdkPaintable* ladybird_web_view_get_bitmap_paintable(LadybirdWebView* self);
GdkPaintable* ladybird_web_view_get_favicon(LadybirdWebView* self);

void ladybird_web_view_zoom_in(LadybirdWebView* self);
void ladybird_web_view_zoom_out(LadybirdWebView* self);
void ladybird_web_view_zoom_reset(LadybirdWebView* self);
guint ladybird_web_view_get_zoom_percent(LadybirdWebView* self);

void ladybird_web_view_scroll_by(LadybirdWebView* self, int page_x_delta, int page_y_delta);
void ladybird_web_view_scroll_to(LadybirdWebView* self, int page_x, int page_y);
void ladybird_web_view_scroll_into_view(LadybirdWebView* self, int page_x, int page_y, int page_width, int page_height);

namespace Browser {
class CookieJar;
};

Browser::CookieJar* ladybird_web_view_get_cookie_jar(LadybirdWebView* self);
void ladybird_web_view_set_cookie_jar(LadybirdWebView* self, Browser::CookieJar* cookie_jar);

G_END_DECLS
