/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/time.h>

namespace Core {

class ElapsedTimer {
public:
    ElapsedTimer(bool precise = false)
        : m_precise(precise)
    {
    }

    bool is_valid() const { return m_valid; }
    void start();
    int elapsed() const;

    const struct timeval& origin_time() const { return m_origin_time; }

private:
    bool m_precise { false };
    bool m_valid { false };
    struct timeval m_origin_time {
        0, 0
    };
};

}
