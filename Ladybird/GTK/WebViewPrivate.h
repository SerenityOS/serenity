#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

typedef struct _LadybirdWebView LadybirdWebView;

void ladybird_web_view_set_page_size(LadybirdWebView* self, int width, int height);
void ladybird_web_view_set_page_title(LadybirdWebView* self, char const* title);
void ladybird_web_view_set_page_url(LadybirdWebView* self, char const* url);
void ladybird_web_view_set_hovered_link(LadybirdWebView* self, char const* hovered_link);
void ladybird_web_view_set_loading(LadybirdWebView* self, bool loading);
GdkPaintable* ladybird_web_view_get_bitmap_paintable(LadybirdWebView* self);

void ladybird_web_view_set_prompt_text(LadybirdWebView* self, char const* text);
void ladybird_web_view_request_alert(LadybirdWebView* self, char const* message);
void ladybird_web_view_request_confirm(LadybirdWebView* self, char const* message);
void ladybird_web_view_request_prompt(LadybirdWebView* self, char const* message, char const* text);
void ladybird_web_view_request_dismiss_dialog(LadybirdWebView* self);
void ladybird_web_view_request_accept_dialog(LadybirdWebView* self);

class LadybirdViewImpl;
LadybirdViewImpl* ladybird_web_view_get_impl(LadybirdWebView* self);

G_END_DECLS
