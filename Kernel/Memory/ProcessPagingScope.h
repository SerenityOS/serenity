/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Types.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ProcessPagingScope {
public:
    explicit ProcessPagingScope(Process&);
    ~ProcessPagingScope();

private:
    u32 m_previous_cr3 { 0 };
};

}
