#pragma once

#include <adwaita.h>
#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWindow, ladybird_window, LADYBIRD, WINDOW, AdwApplicationWindow)
#define LADYBIRD_TYPE_WINDOW ladybird_window_get_type()

LadybirdWindow* ladybird_window_new(GtkApplication* app, bool add_initial_tab);

G_END_DECLS
