/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdLocationEntry, ladybird_location_entry, LADYBIRD, LOCATION_ENTRY, GtkEntry)
#define LADYBIRD_TYPE_LOCATION_ENTRY ladybird_location_entry_get_type()

G_END_DECLS
