/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct Selection {
    size_t start { 0 };
    size_t end { 0 };

    void clear()
    {
        start = 0;
        end = 0;
    }
    bool is_empty() const { return start == end; }
    size_t size() const { return (start < end) ? (end - start) : (start - end); }
    bool contains(size_t position) const { return (min(start, end) <= position) && (position < max(start, end)); }
};
