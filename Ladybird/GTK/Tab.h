#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdTab, ladybird_tab, LADYBIRD, TAB, GtkWidget)
#define LADYBIRD_TYPE_TAB ladybird_tab_get_type()

LadybirdTab* ladybird_tab_new(void);

typedef struct _LadybirdWebView LadybirdWebView;
LadybirdWebView* ladybird_tab_get_web_view(LadybirdTab* self);

void ladybird_tab_open_js_console(LadybirdTab* self);

G_END_DECLS
