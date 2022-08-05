/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeymapStatusWindow.h"
#include <LibGUI/Event.h>
#include <LibGUI/WindowManager.h>

class KeymapWindowManager : public GUI::WindowManager {
public:
    KeymapWindowManager(NonnullRefPtr<KeymapStatusWindow> window)
        : m_window(window) {};

private:
    NonnullRefPtr<KeymapStatusWindow> m_window;
    void event(Core::Event& event) override;
};

void KeymapWindowManager::event(Core::Event& event)
{
    m_window->wm_event(static_cast<GUI::WMEvent&>(event));
};
