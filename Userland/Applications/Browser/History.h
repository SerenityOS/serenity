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
    struct URLTitlePair {
        URL url;
        String title;
    };
    void dump() const;

    void push(const URL& url, String const& title);
    void update_title(String const& title);
    URLTitlePair current() const;

    Vector<StringView> const get_back_title_history();
    Vector<StringView> const get_forward_title_history();

    void go_back(int steps = 1);
    void go_forward(int steps = 1);

    bool can_go_back(int steps = 1) { return (m_current - steps) >= 0; }
    bool can_go_forward(int steps = 1) { return (m_current + steps) < static_cast<int>(m_items.size()); }
    void clear();

private:
    Vector<URLTitlePair> m_items;
    int m_current { -1 };
};

}
