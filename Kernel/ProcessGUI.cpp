#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <Widgets/AbstractScreen.h>
#include <Widgets/FrameBuffer.h>
#include <Widgets/EventLoop.h>
#include <Widgets/Font.h>
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

    GUI_CreateWindowParameters params = *user_params;
    Rect rect { params.rect.x, params.rect.y, params.rect.width, params.rect.height };

    if (rect.is_empty())
        return -EINVAL;

    ProcessPagingScope scope(EventLoop::main().server_process());

    auto* window = new Window;
    if (!window)
        return -ENOMEM;

    int window_id = m_windows.size();
    m_windows.append(window);

    window->setTitle(params.title);
    window->setRect(rect);

    auto* main_widget = new Widget;
    window->setMainWidget(main_widget);
    main_widget->setWindowRelativeRect({ 0, 0, rect.width(), rect.height() });
    main_widget->setBackgroundColor(params.background_color);
    main_widget->setFillWithBackgroundColor(true);
    dbgprintf("%s<%u> gui$create_window: %d with rect {%d,%d %dx%d}\n", name().characters(), pid(), window_id, rect.x(), rect.y(), rect.width(), rect.height());

    return window_id;
}

int Process::gui$destroy_window(int window_id)
{
    wait_for_gui_server();
    dbgprintf("%s<%u> gui$destroy_window (window_id=%d)\n", name().characters(), pid(), window_id);
    if (window_id < 0)
        return -EINVAL;
    if (window_id >= static_cast<int>(m_windows.size()))
        return -EBADWIN;
    auto* window = m_windows[window_id];
    m_windows.remove(window_id);
    window->deleteLater();
    return 0;
}
