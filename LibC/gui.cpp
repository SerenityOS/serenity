#include "gui.h"
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>
#include <errno.h>

int gui_create_window(const GUI_WindowParameters* params)
{
    int rc = syscall(SC_gui_create_window, params);
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
