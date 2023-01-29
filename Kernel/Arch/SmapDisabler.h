/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

class SmapDisabler {
public:
    SmapDisabler();
    ~SmapDisabler();

private:
    const FlatPtr m_flags;
};

}
