/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Color.h>

class Palette {
public:
    enum class FireColor : int {
        Orange,
        Green,
        Purple
    };
    Palette(FireColor);
    virtual ~Palette();
    Vector<Color> palette() const { return m_palette; }

private:
    FireColor m_fire_color;
    Vector<Color> m_palette;
};
