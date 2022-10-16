/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace Kernel {

struct RegisterState {
    FlatPtr userspace_sp() const { return 0; }
    FlatPtr ip() const { return 0; }
};

struct DebugRegisterState {
};

}
