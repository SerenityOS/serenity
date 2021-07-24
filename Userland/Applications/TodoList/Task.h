/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

class Task {
public:
    enum class State {
        todo,
        done
    };

    static String state_to_string(const State& s)
    {
        switch (s) {
        case State::todo:
            return "TODO";
        case State::done:
            return "DONE";
        };

        return "";
    }

    Task(String title, String notes)
        : m_title { move(title) }
        , m_notes { move(notes) }
    {
    }
    Task(String title)
        : Task(title, "")
    {
    }

    Task() = default;

    State state() const;
    void set_state(State state);

    String title() const;
    String description() const;
    String to_string() const;

private:
    State m_current_state = State::todo;
    String m_title {};
    String m_notes {};
};
