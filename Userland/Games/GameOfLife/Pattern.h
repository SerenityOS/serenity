/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include <AK/Vector.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>

class Board;

class Pattern {
public:
    Pattern(Vector<String>);
    virtual ~Pattern();

    virtual void on_pattern_button_contextmenu(GUI::ContextMenuEvent&) { }
    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);
    Vector<String> m_pattern;

protected:
    RefPtr<GUI::Action> m_action;
};
