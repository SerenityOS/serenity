/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/UnveilNode.h>

namespace Kernel {

enum class VeilState {
    None,
    Dropped,
    Locked,
    LockedInherited,
};

struct UnveilData {
    explicit UnveilData(UnveilNode&& p)
        : paths(move(p))
    {
    }
    VeilState state { VeilState::None };
    UnveilNode paths;
};

}
