#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWebContentView, ladybird_web_content_view, LADYBIRD, WEB_CONTENT_VIEW, GtkWidget)
#define LADYBIRD_TYPE_WEB_CONTENT_VIEW ladybird_web_content_view_get_type()

G_END_DECLS
