#pragma once

#include <sys/cdefs.h>
#include <Kernel/GUITypes.h>

__BEGIN_DECLS

int gui_create_window(const GUI_WindowParameters*);
int gui_invalidate_window(int window_id, const GUI_Rect*);
int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo*);
int gui_release_window_backing_store(void* backing_store_id);
int gui_get_window_title(int window_id, char*, size_t);
int gui_set_window_title(int window_id, const char*, size_t);
int gui_get_window_rect(int window_id, GUI_Rect*);
int gui_set_window_rect(int window_id, const GUI_Rect*);

__END_DECLS

