#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>

//#define LOG_GUI_SYSCALLS

void Process::initialize_gui_statics()
{
    Font::initialize();
    WSEventLoop::initialize();
    WSWindowManager::initialize();
    WSScreen::initialize();

    new WSEventLoop;
}

int Process::make_window_id()
{
    int new_id = m_next_window_id++;
    while (!new_id || m_windows.contains(new_id)) {
        new_id = m_next_window_id++;
        if (new_id < 0)
            new_id = 1;
    }
    return new_id;
}

static void wait_for_gui_server()
{
    // FIXME: Time out after a while and return an error.
    while (!WSEventLoop::the().running())
        sleep(10);
}

int Process::gui$create_window(const GUI_WindowParameters* user_params)
{
    wait_for_gui_server();

    if (!validate_read_typed(user_params))
        return -EFAULT;

    auto params = *user_params;
    Rect rect = params.rect;

    if (rect.is_empty())
        return -EINVAL;

    ProcessPagingScope scope(WSEventLoop::the().server_process());

    int window_id = make_window_id();
    if (!window_id)
        return -ENOMEM;

    auto window = make<WSWindow>(*this, window_id);
    if (!window)
        return -ENOMEM;

    window->set_title(params.title);
    window->set_rect(rect);

    m_windows.set(window_id, move(window));
#ifdef LOG_GUI_SYSCALLS
    dbgprintf("%s<%u> gui$create_window: %d with rect {%d,%d %dx%d}\n", name().characters(), pid(), window_id, rect.x(), rect.y(), rect.width(), rect.height());
#endif
    return window_id;
}

int Process::gui$destroy_window(int window_id)
{
#ifdef LOG_GUI_SYSCALLS
    dbgprintf("%s<%u> gui$destroy_window (window_id=%d)\n", name().characters(), pid(), window_id);
#endif
    if (window_id < 0)
        return -EINVAL;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    m_windows.remove(window_id);
    return 0;
}

int Process::gui$get_window_backing_store(int window_id, GUI_WindowBackingStoreInfo* info)
{
#ifdef LOG_GUI_SYSCALLS
    dbgprintf("%s<%u> gui$get_window_backing_store (window_id=%d, info=%p)\n", name().characters(), pid(), window_id, info);
#endif
    if (!validate_write_typed(info))
        return -EFAULT;
    if (window_id < 0)
        return -EINVAL;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    info->bpp = sizeof(RGBA32);
    info->pitch = window.backing()->pitch();
    info->size = window.backing()->size();
    info->pixels = reinterpret_cast<RGBA32*>(window.backing()->client_region()->linearAddress.asPtr());
    return 0;
}

int Process::gui$invalidate_window(int window_id, const GUI_Rect* rect)
{
    if (window_id < 0)
        return -EINVAL;
    if (rect && !validate_read_typed(rect))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
#ifdef LOG_GUI_SYSCALLS
    if (!rect)
        dbgprintf("%s<%u> gui$invalidate_window (window_id=%d, rect=(entire))\n", name().characters(), pid(), window_id);
    else
        dbgprintf("%s<%u> gui$invalidate_window (window_id=%d, rect={%d,%d %dx%d})\n", name().characters(), pid(), window_id, rect->location.x, rect->location.y, rect->size.width, rect->size.height);
#endif
    auto& window = *(*it).value;
    auto event = make<WSEvent>(WSEvent::WM_Invalidate);
    if (rect)
        event->set_rect(*rect);
    WSEventLoop::the().post_event(&window, move(event));
    WSEventLoop::the().server_process().request_wakeup();
    return 0;
}

int Process::gui$get_window_parameters(int window_id, GUI_WindowParameters* params)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_write_typed(params))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    params->rect = window.rect();
    strcpy(params->title, window.title().characters());
    return 0;
}

int Process::gui$set_window_parameters(int window_id, const GUI_WindowParameters* params)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_read_typed(params))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    window.set_rect(params->rect);
    window.set_title(params->title);
    return 0;
}
