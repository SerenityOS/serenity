/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class DashboardWindow final : public GUI::Window {
    C_OBJECT(DashboardWindow);

public:
    virtual ~DashboardWindow() override = default;

    virtual void event(Core::Event&) override;

private:
    explicit DashboardWindow();

    RefPtr<GUI::Widget> m_container;
};
