/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskManagerWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <LibWebView/ProcessManager.h>

namespace Browser {

static RefPtr<GUI::Window> s_window;

static GUI::Window& task_manager_window()
{
    if (!s_window) {
        s_window = GUI::Window::construct();
        s_window->set_window_mode(GUI::WindowMode::Modeless);
        s_window->resize(400, 300);
        s_window->set_title("Task Manager");

        (void)s_window->set_main_widget<TaskManagerWidget>();
    }
    return *s_window;
}

void show_task_manager_window()
{
    auto& window = task_manager_window();
    window.show();
    window.move_to_front();
}

void close_task_manager_window()
{
    if (s_window) {
        task_manager_window().close();
    }
}

TaskManagerWidget::~TaskManagerWidget() = default;

TaskManagerWidget::TaskManagerWidget()
{
    m_update_timer = MUST(Core::Timer::create_repeating(1000, [this] {
        this->update_statistics();
    }));
    m_update_timer->start();

    m_web_view = add<WebView::OutOfProcessWebView>();

    set_layout<GUI::VerticalBoxLayout>(4);
    set_fill_with_background_color(true);

    m_web_view->set_focus(true);
}

void TaskManagerWidget::show_event(GUI::ShowEvent& event)
{
    m_update_timer->start();

    GUI::Widget::show_event(event);
}

void TaskManagerWidget::hide_event(GUI::HideEvent& event)
{
    m_update_timer->stop();

    GUI::Widget::hide_event(event);
}

void TaskManagerWidget::update_statistics()
{
    WebView::ProcessManager::the().update_all_processes();
    m_web_view->load_html(WebView::ProcessManager::the().generate_html());
}

}
