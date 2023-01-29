/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>

namespace Core {

class ElapsedTimer {
public:
    static ElapsedTimer start_new();

    ElapsedTimer(bool precise = false)
        : m_precise(precise)
    {
    }

    bool is_valid() const { return m_valid; }
    void start();
    void reset();

    i64 elapsed() const; // milliseconds
    Time elapsed_time() const;

    Time const& origin_time() const { return m_origin_time; }

private:
    Time m_origin_time {};
    bool m_precise { false };
    bool m_valid { false };
};

}
