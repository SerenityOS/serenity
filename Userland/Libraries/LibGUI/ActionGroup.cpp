/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>

namespace GUI {

void ActionGroup::add_action(Action& action)
{
    action.set_group({}, this);
    m_actions.set(&action);
}

void ActionGroup::remove_action(Action& action)
{
    action.set_group({}, nullptr);
    m_actions.remove(&action);
}

}
