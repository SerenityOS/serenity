/*
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Widget.h>

namespace ImageViewer {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget)

public:
    virtual ~MainWidget() override = default;

private:
    MainWidget();
    virtual void keydown_event(GUI::KeyEvent& event) final;
};

}
