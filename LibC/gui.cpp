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

int gui_get_window_parameters(int window_id, GUI_WindowParameters* params)
{
    int rc = syscall(SC_gui_get_window_parameters, window_id, params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_set_window_parameters(int window_id, const GUI_WindowParameters* params)
{
    int rc = syscall(SC_gui_set_window_parameters, window_id, params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
