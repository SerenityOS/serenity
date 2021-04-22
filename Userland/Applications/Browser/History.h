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

    void go_back();
    void go_forward();

    bool can_go_back() { return m_current > 0; }
    bool can_go_forward() { return m_current + 1 < static_cast<int>(m_items.size()); }

    void clear();

private:
    Vector<URL> m_items;
    int m_current { -1 };
};

}
