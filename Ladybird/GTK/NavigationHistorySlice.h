#pragma once

#include <gio/gio.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdNavigationHistorySlice, ladybird_navigation_history_slice, LADYBIRD, NAVIGATION_HISTORY_SLICE, GObject)
#define LADYBIRD_TYPE_NAVIGATION_HISTORY_SLICE ladybird_navigation_history_slice_get_type()

guint ladybird_navigation_history_slice_map_position(LadybirdNavigationHistorySlice* self, guint position);

typedef struct _LadybirdNavigationHistory LadybirdNavigationHistory;

GListModel* ladybird_navigation_history_slice_new_back(LadybirdNavigationHistory* history);
GListModel* ladybird_navigation_history_slice_new_forward(LadybirdNavigationHistory* history);

G_END_DECLS
