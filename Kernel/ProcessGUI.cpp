#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <Widgets/AbstractScreen.h>
#include <Widgets/FrameBuffer.h>
#include <Widgets/EventLoop.h>
#include <Widgets/Font.h>
#include <Widgets/Button.h>
#include <Widgets/Label.h>
#include <Widgets/Widget.h>
#include <Widgets/Window.h>
#include <Widgets/WindowManager.h>

void Process::initialize_gui_statics()
{
    Font::initialize();
    FrameBuffer::initialize();
    EventLoop::initialize();
    WindowManager::initialize();
    AbstractScreen::initialize();

    new EventLoop;
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
    while (!EventLoop::main().running())
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

    ProcessPagingScope scope(EventLoop::main().server_process());

    int window_id = make_window_id();
    if (!window_id)
        return -ENOMEM;

    auto window = make<Window>(*this, window_id);
    if (!window)
        return -ENOMEM;

    window->setTitle(params.title);
    window->setRect(rect);

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
    WindowManager::the().invalidate(window);
    return 0;
}
