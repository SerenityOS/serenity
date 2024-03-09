/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

enum class UnshareType {
    ScopedProcessList = 1,
    VFSRootContext = 2,
    HostnameContext = 3,
};

}
