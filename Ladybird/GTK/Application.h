#pragma once

#include <adwaita.h>
#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdApplication, ladybird_application, LADYBIRD, APPLICATION, AdwApplication)
#define LADYBIRD_TYPE_APPLICATION ladybird_application_get_type()

LadybirdApplication* ladybird_application_new(void);

GFile* ladybird_application_get_webdriver_content_ipc_path(LadybirdApplication* self);

namespace WebView {
class CookieJar;
}

WebView::CookieJar* ladybird_application_get_cookie_jar(LadybirdApplication* self);
WebView::CookieJar* ladybird_application_get_incognito_cookie_jar(LadybirdApplication* self);

G_END_DECLS
