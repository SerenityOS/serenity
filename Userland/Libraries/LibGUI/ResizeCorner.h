/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class ResizeCorner : public Widget {
    C_OBJECT(ResizeCorner)
public:
    virtual ~ResizeCorner() override;

protected:
    ResizeCorner();

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
};

}
