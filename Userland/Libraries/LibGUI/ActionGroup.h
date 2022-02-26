/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/Weakable.h>
#include <LibGUI/Forward.h>

namespace GUI {

class ActionGroup : public Weakable<ActionGroup> {
public:
    ActionGroup() = default;
    ~ActionGroup() = default;

    void add_action(Action&);
    void remove_action(Action&);

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool exclusive) { m_exclusive = exclusive; }

    bool is_unchecking_allowed() const { return m_unchecking_allowed; }
    void set_unchecking_allowed(bool unchecking_allowed) { m_unchecking_allowed = unchecking_allowed; }

    template<typename C>
    void for_each_action(C callback)
    {
        for (auto& it : m_actions) {
            if (callback(*it) == IterationDecision::Break)
                break;
        }
    }

private:
    HashTable<Action*> m_actions;
    bool m_exclusive { false };
    bool m_unchecking_allowed { false };
};

}
