/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

enum class PCIVendorID {
    VirtIO = 0x1af4,
    Intel = 0x8086,
};

enum class PCIDeviceID {
    VirtIOConsole = 0x1003,
    VirtIOEntropy = 0x1005,
};

}
