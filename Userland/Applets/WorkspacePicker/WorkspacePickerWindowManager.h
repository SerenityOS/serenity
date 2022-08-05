/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DesktopStatusWindow.h"
#include <LibGUI/Event.h>
#include <LibGUI/WindowManager.h>

class WorkspacePickerWindowManager : public GUI::WindowManager {
public:
    WorkspacePickerWindowManager(NonnullRefPtr<DesktopStatusWindow> window)
        : m_window(window) {};

private:
    NonnullRefPtr<DesktopStatusWindow> m_window;
    void event(Core::Event& event) override;
};

void WorkspacePickerWindowManager::event(Core::Event& event)
{
    m_window->wm_event(static_cast<GUI::WMEvent&>(event));
};
