#pragma once

#include <sys/cdefs.h>
#include <Kernel/GUITypes.h>

__BEGIN_DECLS

int gui_create_window(const GUI_WindowParameters*);
int gui_destroy_window(int window_id);
int gui_invalidate_window(int window_id, const GUI_Rect*);
int gui_notify_paint_finished(int window_id, const GUI_Rect*);
int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo*);
int gui_release_window_backing_store(void* backing_store_id);
int gui_get_window_title(int window_id, char*, size_t);
int gui_set_window_title(int window_id, const char*, size_t);
int gui_get_window_rect(int window_id, GUI_Rect*);
int gui_set_window_rect(int window_id, const GUI_Rect*);
int gui_set_global_cursor_tracking_enabled(int window_id, bool);
int gui_menubar_add_menu(int menubar_id, int menu_id);
int gui_menu_create(const char* name);
int gui_menu_destroy(int menu_id);
int gui_menu_add_separator(int menu_id);
int gui_menu_add_item(int menu_id, unsigned identifier, const char* text);
int gui_app_set_menubar(int menubar_id);

__END_DECLS

