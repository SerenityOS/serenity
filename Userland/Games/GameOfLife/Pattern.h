/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>

class Pattern final {
public:
    Pattern(StringView pattern, unsigned int width);
    char at(unsigned int x, unsigned int y) const { return m_pattern[y * m_width + x]; }

    GUI::Action* action() { return m_action; }
    unsigned height() const { return m_pattern.size() / m_width; }
    void rotate_clockwise();
    void set_action(GUI::Action*);
    unsigned int width() const { return m_width; }

private:
    RefPtr<GUI::Action> m_action;
    Vector<char> m_pattern;
    unsigned int m_width;
};
