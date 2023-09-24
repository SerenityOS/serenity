#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdHistoryEntry, ladybird_history_entry, LADYBIRD, HISTORY_ENTRY, GInitiallyUnowned)
#define LADYBIRD_TYPE_HISTORY_ENTRY ladybird_history_entry_get_type()

LadybirdHistoryEntry* ladybird_history_entry_new(char const* url);

char const* ladybird_history_entry_get_url(LadybirdHistoryEntry* self);
void ladybird_history_entry_set_url(LadybirdHistoryEntry* self, char const* url);

char const* ladybird_history_entry_get_title(LadybirdHistoryEntry* self);
void ladybird_history_entry_set_title(LadybirdHistoryEntry* self, char const* title);

G_END_DECLS
