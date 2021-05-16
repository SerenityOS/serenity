/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Pattern.h"
#include <LibGUI/Action.h>

Pattern::Pattern(Vector<String> pattern)
{
    m_pattern = pattern;
}

Pattern::~Pattern()
{
}

void Pattern::set_action(GUI::Action* action)
{
    m_action = action;
}
