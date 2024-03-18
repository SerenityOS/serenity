/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibURL/URL.h>

namespace WebView {

class History {
public:
    struct URLTitlePair {
        URL::URL url;
        ByteString title;
    };
    void dump() const;
    Vector<URLTitlePair> get_all_history_entries();

    void push(const URL::URL& url, ByteString const& title);
    void replace_current(const URL::URL& url, ByteString const& title);
    void update_title(ByteString const& title);
    URLTitlePair current() const;

    Vector<StringView> const get_back_title_history();
    Vector<StringView> const get_forward_title_history();

    void go_back(int steps = 1);
    void go_forward(int steps = 1);

    bool can_go_back(int steps = 1) { return (m_current - steps) >= 0; }
    bool can_go_forward(int steps = 1) { return (m_current + steps) < static_cast<int>(m_items.size()); }
    void clear();

    bool is_empty() const { return m_items.is_empty(); }

private:
    Vector<URLTitlePair> m_items;
    int m_current { -1 };
};

}
