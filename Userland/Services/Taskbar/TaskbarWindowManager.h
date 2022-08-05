/*
 * Copyright (c) 2022, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "TaskbarWindow.h"
#include <AK/NonnullRefPtr.h>
#include <LibCore/Event.h>
#include <LibGUI/WindowManager.h>

class TaskbarWindowManager : public GUI::WindowManager {
public:
    TaskbarWindowManager(NonnullRefPtr<TaskbarWindow> taskbar)
        : m_taskbar(taskbar) {};

private:
    NonnullRefPtr<TaskbarWindow> m_taskbar;
    void event(Core::Event& event) override;
};

void TaskbarWindowManager::event(Core::Event& event)
{
    m_taskbar->wm_event(static_cast<GUI::WMEvent&>(event));
};
