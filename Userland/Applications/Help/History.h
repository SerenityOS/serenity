/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Vector.h>

class History final {
public:
    void push(StringView history_item);
    ByteString current();

    void go_back();
    void go_forward();

    bool can_go_back() { return m_current_history_item > 0; }
    bool can_go_forward() { return m_current_history_item + 1 < static_cast<int>(m_items.size()); }

    void clear();

private:
    Vector<ByteString> m_items;
    int m_current_history_item { -1 };
};
