/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Pattern.h"
#include <YAK/String.h>
#include <YAK/StringBuilder.h>
#include <YAK/Vector.h>
#include <LibGUI/Action.h>
#include <stdio.h>

Pattern::Pattern(Vector<String> pattern)
{
    m_pattern = move(pattern);
}

Pattern::~Pattern()
{
}

void Pattern::set_action(GUI::Action* action)
{
    m_action = action;
}

void Pattern::rotate_clockwise()
{
    Vector<String> rotated;
    for (size_t i = 0; i < m_pattern.first().length(); i++) {
        StringBuilder builder;
        for (int j = m_pattern.size() - 1; j >= 0; j--) {
            builder.append(m_pattern.at(j).substring(i, 1));
        }
        rotated.append(builder.to_string());
    }
    m_pattern = move(rotated);
}
