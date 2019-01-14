#include "gui.h"
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>
#include <errno.h>

int gui_create_window(const GUI_CreateWindowParameters* params)
{
    int rc = syscall(SC_gui_create_window, params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_invalidate_window(int window_id)
{
    int rc = syscall(SC_gui_invalidate_window, window_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gui_get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo* info)
{
    int rc = syscall(SC_gui_get_window_backing_store, window_id, info);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
