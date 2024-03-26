/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WebContentView.h"
#include <QTimer>
#include <QWidget>

namespace Ladybird {

class TaskManagerWindow : public QWidget {
    Q_OBJECT

public:
    TaskManagerWindow();
    static TaskManagerWindow& the();

private:
    virtual void showEvent(QShowEvent*) override;
    virtual void hideEvent(QHideEvent*) override;

    void update_statistics();

    WebContentView* m_web_view { nullptr };
    QTimer m_update_timer;
};

void show_task_manager_window();
void close_task_manager_window();

}
