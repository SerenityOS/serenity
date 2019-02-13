#include "gui.h"
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>
#include <errno.h>

int gui_create_window(const GUI_WindowParameters* params)
{
    int rc = syscall(SC_gui_create_window, params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_destroy_window(int window_id)
{
    int rc = syscall(SC_gui_destroy_window, window_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_invalidate_window(int window_id, const GUI_Rect* rect)
{
    int rc = syscall(SC_gui_invalidate_window, window_id, rect);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo* info)
{
    int rc = syscall(SC_gui_get_window_backing_store, window_id, info);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_release_window_backing_store(void* backing_store_id)
{
    int rc = syscall(SC_gui_release_window_backing_store, backing_store_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_get_window_title(int window_id, char* buffer, size_t size)
{
    int rc = syscall(SC_gui_get_window_title, window_id, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_set_window_title(int window_id, const char* title, size_t length)
{
    int rc = syscall(SC_gui_set_window_title, window_id, title, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_get_window_rect(int window_id, GUI_Rect* rect)
{
    int rc = syscall(SC_gui_get_window_rect, window_id, rect);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_set_window_rect(int window_id, const GUI_Rect* rect)
{
    int rc = syscall(SC_gui_set_window_rect, window_id, rect);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_notify_paint_finished(int window_id, const GUI_Rect* rect)
{
    int rc = syscall(SC_gui_notify_paint_finished, window_id, rect);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_set_global_cursor_tracking_enabled(int window_id, bool enabled)
{
    int rc = syscall(SC_gui_set_global_cursor_tracking_enabled, window_id, enabled);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_menubar_add_menu(int menubar_id, int menu_id)
{
    int rc = syscall(SC_gui_menubar_add_menu, menubar_id, menu_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_menu_create(const char* name)
{
    int rc = syscall(SC_gui_menu_create, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_menu_destroy(int menu_id)
{
    int rc = syscall(SC_gui_menu_destroy, menu_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_menu_add_separator(int menu_id)
{
    int rc = syscall(SC_gui_menu_add_separator, menu_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_menu_add_item(int menu_id, unsigned identifier, const char* text)
{
    int rc = syscall(SC_gui_menu_add_item, menu_id, identifier, text);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_app_set_menubar(int menubar_id)
{
    int rc = syscall(SC_gui_app_set_menubar, menubar_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
