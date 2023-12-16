/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Application.h"
#include "EventLoopImplementationGLib.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>
#include <glib/gi18n.h>

#define GETTEXT_PACKAGE "ladybird-gtk4"

int main(int argc, char* argv[])
{
    bind_textdomain_codeset(GETTEXT_PACKAGE, "UTF-8");
    bindtextdomain(GETTEXT_PACKAGE, LOCALEDIR);
    textdomain(GETTEXT_PACKAGE);

    Core::EventLoopManager::install(*new EventLoopManagerGLib);
    [[maybe_unused]] Core::EventLoop serenity_event_loop;

    LadybirdApplication* app = ladybird_application_new();
    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}
