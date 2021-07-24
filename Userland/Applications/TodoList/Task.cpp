/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Task.h"

AK::String Task::to_string() const
{
    return String::formatted("{} {}", state_to_string(state()), title());
}

Task::State Task::state() const
{
    return m_current_state;
}

void Task::set_state(State state)
{
    m_current_state = state;
}

AK::String Task::title() const
{
    return m_title;
}

AK::String Task::description() const
{
    return m_notes;
}
