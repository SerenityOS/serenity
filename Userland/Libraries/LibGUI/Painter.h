/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Forward.h>
#include <LibGfx/Painter.h>

namespace GUI {

class Painter : public Gfx::Painter {
public:
    explicit Painter(Widget&);
    explicit Painter(Gfx::Bitmap&);
};

}
