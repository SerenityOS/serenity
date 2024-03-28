/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskManagerWindow.h"
#include <LibWebView/ProcessManager.h>
#include <QVBoxLayout>

namespace Ladybird {

void show_task_manager_window()
{
    auto& window = TaskManagerWindow::the();
    window.show();
    window.activateWindow();
    window.raise();
}

void close_task_manager_window()
{
    TaskManagerWindow::the().close();
}

TaskManagerWindow::TaskManagerWindow()
    : m_web_view(new WebContentView(this, {}, {}))
{
    setLayout(new QVBoxLayout);
    layout()->addWidget(m_web_view);

    setWindowTitle("Task Manager");
    resize(400, 300);

    m_update_timer.setInterval(1000);

    QObject::connect(&m_update_timer, &QTimer::timeout, [this] {
        this->update_statistics();
    });
}

TaskManagerWindow& TaskManagerWindow::the()
{
    static TaskManagerWindow instance;
    return instance;
}

void TaskManagerWindow::showEvent(QShowEvent*)
{
    m_update_timer.start();
}

void TaskManagerWindow::hideEvent(QHideEvent*)
{
    m_update_timer.stop();
}

void TaskManagerWindow::update_statistics()
{

    WebView::ProcessManager::the().update_all_processes();
    m_web_view->load_html(WebView::ProcessManager::the().generate_html());
}

}
