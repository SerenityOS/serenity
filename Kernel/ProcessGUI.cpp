#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>
#include <Kernel/BochsVGADevice.h>

//#define LOG_GUI_SYSCALLS

void Process::initialize_gui_statics()
{
    Font::initialize();
    WSMessageLoop::initialize();
    WSWindowManager::initialize();
    WSScreen::initialize();

    new WSMessageLoop;
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
    while (!WSMessageLoop::the().running())
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

    ProcessPagingScope scope(WSMessageLoop::the().server_process());

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
    auto message = make<WSMessage>(WSMessage::WM_DestroyWindow);
    WSMessageLoop::the().post_message((*it).value.leak_ptr(), move(message), true);
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
    WSMessageLoop::the().post_message(&window, make<WSClientWantsToPaintMessage>(rect));
    WSMessageLoop::the().server_process().request_wakeup();
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
    WSMessageLoop::the().post_message(&window, make<WSClientFinishedPaintMessage>(rect));
    WSMessageLoop::the().server_process().request_wakeup();
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
    WSMessageLoop::the().post_message(&window, make<WSSetWindowTitleMessage>(move(new_title)));
    WSMessageLoop::the().server_process().request_wakeup();
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
    WSMessageLoop::the().post_message(&window, make<WSSetWindowRectMessage>(new_rect));
    WSMessageLoop::the().server_process().request_wakeup();
    return 0;
}

int Process::gui$set_global_cursor_tracking_enabled(int window_id, bool enabled)
{
    if (window_id < 0)
        return -EINVAL;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    WSWindowLocker locker(window);
    window.set_global_cursor_tracking_enabled(enabled);
    return 0;
}

void Process::destroy_all_windows()
{
    InterruptFlagSaver saver;
    sti();
    for (auto& it : m_windows) {
        auto message = make<WSMessage>(WSMessage::WM_DestroyWindow);
        it.value->notify_process_died(Badge<Process>());
        WSMessageLoop::the().post_message(it.value.leak_ptr(), move(message), true);
    }
    m_windows.clear();
}


DisplayInfo Process::set_video_resolution(int width, int height)
{
    DisplayInfo info;
    info.width = width;
    info.height = height;
    info.bpp = 32;
    info.pitch = width * 4;
    size_t framebuffer_size = width * height * 4;
    if (!m_display_framebuffer_region) {
        auto framebuffer_vmo = VMObject::create_framebuffer_wrapper(BochsVGADevice::the().framebuffer_address(), framebuffer_size);
        m_display_framebuffer_region = allocate_region_with_vmo(LinearAddress(0xe0000000), framebuffer_size, move(framebuffer_vmo), 0, "framebuffer", true, true);
    }
    info.framebuffer = m_display_framebuffer_region->laddr().as_ptr();

    BochsVGADevice::the().set_resolution(width, height);
    return info;
}
