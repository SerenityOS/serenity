#pragma once

#include <sys/cdefs.h>
#include <Kernel/GUITypes.h>

__BEGIN_DECLS

int gui_create_window(const GUI_WindowParameters*);
int gui_invalidate_window(int window_id, const GUI_Rect*);
int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo*);
int gui_get_window_parameters(int window_id, GUI_WindowParameters*);
int gui_set_window_parameters(int window_id, const GUI_WindowParameters*);

__END_DECLS

