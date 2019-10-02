#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

class History final {
public:
    void push(const StringView& history_item);
    String current();

    void go_back();
    void go_forward();

    bool can_go_back() { return m_current_history_item > 0; }
    bool can_go_forward() { return m_current_history_item + 1 < m_items.size(); }

    void clear();

private:
    Vector<String> m_items;
    int m_current_history_item { -1 };
};
