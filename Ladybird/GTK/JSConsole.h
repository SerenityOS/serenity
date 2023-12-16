/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdJSConsole, ladybird_js_console, LADYBIRD, JS_CONSOLE, GtkWidget)
#define LADYBIRD_TYPE_JS_CONSOLE ladybird_js_console_get_type()

typedef struct _LadybirdWebView LadybirdWebView;
LadybirdJSConsole* ladybird_js_console_new(LadybirdWebView* web_view);
LadybirdWebView* ladybird_js_console_get_web_view(LadybirdJSConsole* self);

G_END_DECLS
