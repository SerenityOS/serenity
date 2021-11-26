/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace FileManager {

class DesktopWidget final : public GUI::Widget {
    C_OBJECT(DesktopWidget);

public:
    virtual ~DesktopWidget() override;

private:
    virtual void paint_event(GUI::PaintEvent&) override;

    DesktopWidget();
};

}
