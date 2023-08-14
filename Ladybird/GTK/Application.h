#pragma once

#include <adwaita.h>
#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdApplication, ladybird_application, LADYBIRD, APPLICATION, AdwApplication)
#define LADYBIRD_TYPE_APPLICATION ladybird_application_get_type()

LadybirdApplication* ladybird_application_new(void);

G_END_DECLS
