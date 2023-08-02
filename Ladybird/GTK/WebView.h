#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWebView, ladybird_web_view, LADYBIRD, WEB_VIEW, GtkWidget)
#define LADYBIRD_TYPE_WEB_VIEW ladybird_web_view_get_type()

void ladybird_web_view_set_page_size(LadybirdWebView* self, int width, int height);

char const* ladybird_web_view_get_page_title(LadybirdWebView* self);
void ladybird_web_view_set_page_title(LadybirdWebView* self, char const* title);

G_END_DECLS
