/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>

class Pattern {
public:
    Pattern(Vector<String>);
    virtual ~Pattern();
    Vector<String> pattern() { return m_pattern; };
    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);
    void rotate_clockwise();

private:
    RefPtr<GUI::Action> m_action;
    Vector<String> m_pattern;
};
