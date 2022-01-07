/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <sys/stat.h>
#include <sys/types.h>

#pragma once

namespace Core {

class UmaskScope {
public:
    explicit UmaskScope(mode_t mask)
    {
        m_old_mask = umask(mask);
    }

    ~UmaskScope()
    {
        umask(m_old_mask);
    }

private:
    mode_t m_old_mask {};
};

}
