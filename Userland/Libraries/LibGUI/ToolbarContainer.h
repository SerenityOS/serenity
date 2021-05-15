/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGUI/Toolbar.h>

namespace GUI {

class ToolbarContainer : public Frame {
    C_OBJECT(ToolbarContainer);

public:
private:
    explicit ToolbarContainer(Gfx::Orientation = Gfx::Orientation::Horizontal);

    virtual void paint_event(GUI::PaintEvent&) override;

    Gfx::Orientation m_orientation { Gfx::Orientation::Horizontal };
};

}
