/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

class DesktopStatusWidget;

class DesktopStatusWindow : public GUI::Window {
    C_OBJECT(DesktopStatusWindow);

public:
    virtual ~DesktopStatusWindow() override = default;

    virtual void wm_event(GUI::WMEvent&) override;

private:
    DesktopStatusWindow();

    DesktopStatusWidget* m_widget;
};
