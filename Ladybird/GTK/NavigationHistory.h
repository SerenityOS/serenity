#pragma once

#include <gtk/gtk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdNavigationHistory, ladybird_navigation_history, LADYBIRD, NAVIGATION_HISTORY, GObject)
#define LADYBIRD_TYPE_NAVIGATION_HISTORY ladybird_navigation_history_get_type()

LadybirdNavigationHistory* ladybird_navigation_history_new(void);

typedef struct _LadybirdHistoryEntry LadybirdHistoryEntry;

void ladybird_navigation_history_push(LadybirdNavigationHistory* self, LadybirdHistoryEntry* entry);

size_t ladybird_navigation_history_get_current_position(LadybirdNavigationHistory* self);
LadybirdHistoryEntry* ladybird_navigation_history_get_current_entry(LadybirdNavigationHistory* self);

bool ladybird_navigation_history_can_navigate_back(LadybirdNavigationHistory* self);
bool ladybird_navigation_history_can_navigate_forward(LadybirdNavigationHistory* self);
void ladybird_navigation_history_navigate(LadybirdNavigationHistory* self, int delta);

G_END_DECLS
