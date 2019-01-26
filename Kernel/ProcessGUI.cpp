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
    WSWindowLocker locker(window);
    auto* backing_store = window.backing();
#ifdef BACKING_STORE_DEBUG
    dbgprintf("%s<%u> +++ %p[%d] (%dx%d)\n", name().characters(), pid(), backing_store, backing_store->width(), backing_store->height());
#endif
    m_retained_backing_stores.append(backing_store);
    info->backing_store_id = backing_store;
    info->bpp = sizeof(RGBA32);
    info->pitch = backing_store->pitch();
    info->size = backing_store->size();
    info->pixels = reinterpret_cast<RGBA32*>(backing_store->client_region()->laddr().as_ptr());
    return 0;
}

int Process::gui$release_window_backing_store(void* backing_store_id)
{
    for (size_t i = 0; i < m_retained_backing_stores.size(); ++i) {
        if (m_retained_backing_stores[i].ptr() == backing_store_id) {
#ifdef BACKING_STORE_DEBUG
            auto* backing_store = m_retained_backing_stores[i].ptr();
            dbgprintf("%s<%u> --- %p (%dx%d)\n", name().characters(), pid(), backing_store, backing_store->width(), backing_store->height());
#endif
            m_retained_backing_stores.remove(i);
            return 0;
        }
    }
    return -EBADBACKING;
}

int Process::gui$invalidate_window(int window_id, const GUI_Rect* a_rect)
{
    if (window_id < 0)
        return -EINVAL;
    if (a_rect && !validate_read_typed(a_rect))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
#ifdef LOG_GUI_SYSCALLS
    if (!a_rect)
        dbgprintf("%s<%u> gui$invalidate_window (window_id=%d, rect=(entire))\n", name().characters(), pid(), window_id);
    else
        dbgprintf("%s<%u> gui$invalidate_window (window_id=%d, rect={%d,%d %dx%d})\n", name().characters(), pid(), window_id, a_rect->location.x, a_rect->location.y, a_rect->size.width, a_rect->size.height);
#endif
    auto& window = *(*it).value;
    Rect rect;
    if (a_rect)
        rect = *a_rect;
    WSEventLoop::the().post_event(&window, make<WSPaintEvent>(rect));
    WSEventLoop::the().server_process().request_wakeup();
    return 0;
}

int Process::gui$notify_paint_finished(int window_id, const GUI_Rect* a_rect)
{
    if (window_id < 0)
        return -EINVAL;
    if (a_rect && !validate_read_typed(a_rect))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
#ifdef LOG_GUI_SYSCALLS
    if (!a_rect)
        dbgprintf("%s<%u> gui$notify_paint_finished (window_id=%d, rect=(entire))\n", name().characters(), pid(), window_id);
    else
        dbgprintf("%s<%u> gui$notify_paint_finished (window_id=%d, rect={%d,%d %dx%d})\n", name().characters(), pid(), window_id, a_rect->location.x, a_rect->location.y, a_rect->size.width, a_rect->size.height);
#endif
    auto& window = *(*it).value;
    Rect rect;
    if (a_rect)
        rect = *a_rect;
    WSEventLoop::the().post_event(&window, make<WSWindowInvalidationEvent>(rect));
    WSEventLoop::the().server_process().request_wakeup();
    return 0;
}

int Process::gui$get_window_title(int window_id, char* buffer, size_t size)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    String title;
    {
        WSWindowLocker locker(window);
        title = window.title();
    }
    if (title.length() > size)
        return -ERANGE;
    memcpy(buffer, title.characters(), title.length());
    return title.length();

}

int Process::gui$set_window_title(int window_id, const char* title, size_t size)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_read(title, size))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    String new_title(title, size);
    WSEventLoop::the().post_event(&window, make<WSSetWindowTitle>(move(new_title)));
    WSEventLoop::the().server_process().request_wakeup();
    return 0;
}

int Process::gui$get_window_rect(int window_id, GUI_Rect* rect)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_write_typed(rect))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    {
        WSWindowLocker locker(window);
        *rect = window.rect();
    }
    return 0;
}

int Process::gui$set_window_rect(int window_id, const GUI_Rect* rect)
{
    if (window_id < 0)
        return -EINVAL;
    if (!validate_read_typed(rect))
        return -EFAULT;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    Rect new_rect = *rect;
    WSEventLoop::the().post_event(&window, make<WSSetWindowRect>(new_rect));
    WSEventLoop::the().server_process().request_wakeup();
    return 0;
}
