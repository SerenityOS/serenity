/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Pattern.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/Action.h>
#include <stdio.h>

Pattern::Pattern(StringView pattern, unsigned int width)
{
    m_width = width;
    m_pattern.clear_with_capacity();

    for (auto s : pattern)
        m_pattern.append(s);
}

void Pattern::set_action(GUI::Action* action)
{
    m_action = action;
}

void Pattern::rotate_clockwise()
{
    Vector<char> rotated;
    rotated.resize(m_pattern.size());

    for (size_t x = 0; x < m_width; x++) {
        for (int y = height() - 1; y >= 0; y--) {
            size_t new_x = height() - 1 - y;
            size_t new_y = x;

            rotated[new_y * height() + new_x] = at(x, y);
        }
    }

    m_width = height();
    m_pattern = move(rotated);
}
