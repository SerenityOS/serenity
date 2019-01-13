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

    if (params.rect.is_empty())
        return -EINVAL;

    ProcessPagingScope scope(EventLoop::main().server_process());

    auto* window = new Window;
    if (!window)
        return -ENOMEM;

    int window_id = m_windows.size();
    m_windows.append(window->makeWeakPtr());

    window->setTitle(params.title);
    window->setRect(params.rect);

    auto* main_widget = new Widget;
    window->setMainWidget(main_widget);
    main_widget->setWindowRelativeRect({ 0, 0, params.rect.width(), params.rect.height() });
    main_widget->setBackgroundColor(params.background_color);
    main_widget->setFillWithBackgroundColor(true);
    dbgprintf("%s<%u> gui$create_window: %d with rect {%d,%d %dx%d}\n", name().characters(), pid(), window_id, params.rect.x(), params.rect.y(), params.rect.width(), params.rect.height());

    return window_id;
}

int Process::gui$destroy_window(int window_id)
{
    dbgprintf("%s<%u> gui$destroy_window (window_id=%d)\n", name().characters(), pid(), window_id);
    if (window_id < 0)
        return -EINVAL;
    if (window_id >= static_cast<int>(m_windows.size()))
        return -EBADWINDOW;
    auto* window = m_windows[window_id].ptr();
    if (!window)
        return -EBADWINDOW;
    window->deleteLater();
    return 0;
}

int Process::gui$create_widget(int window_id, const GUI_CreateWidgetParameters* user_params)
{
    if (!validate_read_typed(user_params))
        return -EFAULT;

    if (window_id < 0)
        return -EINVAL;
    if (window_id >= static_cast<int>(m_windows.size()))
        return -EINVAL;
    if (!m_windows[window_id])
        return -EINVAL;
    auto& window = *m_windows[window_id];

    auto params = *user_params;

    if (params.rect.is_empty())
        return -EINVAL;

    Widget* widget = nullptr;
    switch (params.type) {
    case GUI_WidgetType::Label:
        widget = new Label(window.mainWidget());
        static_cast<Label*>(widget)->setText(params.text);
        break;
    case GUI_WidgetType::Button:
        widget = new Button(window.mainWidget());
        static_cast<Button*>(widget)->setCaption(params.text);
        break;
    }

    int widget_id = m_widgets.size();
    m_widgets.append(widget->makeWeakPtr());

    widget->setWindowRelativeRect(params.rect);
    widget->setBackgroundColor(params.background_color);
    widget->setFillWithBackgroundColor(params.opaque);
    dbgprintf("%s<%u> gui$create_widget: %d with rect {%d,%d %dx%d}\n", name().characters(), pid(), widget_id, params.rect.x(), params.rect.y(), params.rect.width(), params.rect.height());

    return window_id;
}

int Process::gui$destroy_widget(int widget_id)
{
    dbgprintf("%s<%u> gui$destroy_widget (widget_id=%d)\n", name().characters(), pid(), widget_id);
    if (widget_id < 0)
        return -EINVAL;
    if (widget_id >= static_cast<int>(m_widgets.size()))
        return -EBADWINDOW;
    auto* widget = m_widgets[widget_id].ptr();
    if (!widget)
        return -EBADWIDGET;
    widget->deleteLater();
    return 0;
}

