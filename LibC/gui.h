#pragma once

#include <sys/cdefs.h>
#include <Kernel/GUITypes.h>

__BEGIN_DECLS

int gui_create_window(const GUI_CreateWindowParameters* params);
int gui_invalidate_window(int window_id, const GUI_Rect*);
int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo* info);

__END_DECLS

