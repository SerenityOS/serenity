/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include <AK/Vector.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>

class Pattern final {
public:
    Pattern(Vector<String>&&);
    Vector<String> pattern() const { return m_pattern; }
    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);
    void rotate_clockwise();

private:
    RefPtr<GUI::Action> m_action;
    Vector<String> m_pattern;
};
