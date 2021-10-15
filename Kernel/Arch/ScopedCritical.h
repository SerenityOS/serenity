/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>

namespace Kernel {

class ScopedCritical {
    AK_MAKE_NONCOPYABLE(ScopedCritical);

public:
    ScopedCritical();
    ~ScopedCritical();
    ScopedCritical(ScopedCritical&& from);

    ScopedCritical& operator=(ScopedCritical&& from);

    void leave();
    void enter();

private:
    bool m_valid { false };
};

}
