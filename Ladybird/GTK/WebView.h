#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWebView, ladybird_web_view, LADYBIRD, WEB_VIEW, GtkWidget)
#define LADYBIRD_TYPE_WEB_VIEW ladybird_web_view_get_type()

G_END_DECLS
