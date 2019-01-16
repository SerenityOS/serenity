#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <Widgets/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSFrameBuffer.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>

void Process::initialize_gui_statics()
{
    Font::initialize();
    WSFrameBuffer::initialize();
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

int Process::gui$create_window(const GUI_CreateWindowParameters* user_params)
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
    dbgprintf("%s<%u> gui$create_window: %d with rect {%d,%d %dx%d}\n", name().characters(), pid(), window_id, rect.x(), rect.y(), rect.width(), rect.height());

    return window_id;
}

int Process::gui$destroy_window(int window_id)
{
    dbgprintf("%s<%u> gui$destroy_window (window_id=%d)\n", name().characters(), pid(), window_id);
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
    dbgprintf("%s<%u> gui$get_window_backing_store (window_id=%d, info=%p)\n", name().characters(), pid(), window_id, info);
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

int Process::gui$invalidate_window(int window_id)
{
    dbgprintf("%s<%u> gui$invalidate_window (window_id=%d)\n", name().characters(), pid(), window_id);
    if (window_id < 0)
        return -EINVAL;
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return -EBADWINDOW;
    auto& window = *(*it).value;
    // FIXME: This should queue up a message that the window server process can read.
    //        Poking into its data structures is not good.
    WSEventLoop::the().post_event(&window, make<WSEvent>(WSEvent::WM_Invalidate));
    return 0;
}
