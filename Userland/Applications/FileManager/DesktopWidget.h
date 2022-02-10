/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace FileManager {

class DesktopWidget final : public GUI::Widget {
    C_OBJECT(DesktopWidget);

public:
    virtual ~DesktopWidget() override = default;

private:
    virtual void paint_event(GUI::PaintEvent&) override;

    DesktopWidget() = default;
};

}
