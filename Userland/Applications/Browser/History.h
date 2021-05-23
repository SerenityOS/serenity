/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>

namespace Browser {

class History {
public:
    void dump() const;

    void push(const URL&);
    URL current() const;
    const Vector<URL> get_back_history();
    const Vector<URL> get_forward_history();

    void go_back(int steps = 1);
    void go_forward(int steps = 1);

    bool can_go_back(int steps = 1) { return (m_current - steps) >= 0; }
    bool can_go_forward(int steps = 1) { return (m_current + steps) < static_cast<int>(m_items.size()); }
    void clear();

private:
    Vector<URL> m_items;
    int m_current { -1 };
};

}
