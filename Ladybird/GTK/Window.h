#pragma once

#include <adwaita.h>
#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdWindow, ladybird_window, LADYBIRD, WINDOW, AdwApplicationWindow)
#define LADYBIRD_TYPE_WINDOW ladybird_window_get_type()

typedef struct _LadybirdApplication LadybirdApplication;
LadybirdWindow* ladybird_window_new(LadybirdApplication* app, bool add_initial_tab, bool incognito);

void ladybird_window_open_file(LadybirdWindow* self, GFile* file);

G_END_DECLS
