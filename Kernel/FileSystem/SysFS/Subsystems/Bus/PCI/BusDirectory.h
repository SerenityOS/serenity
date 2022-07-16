/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>

namespace Kernel {

class PCIBusSysFSDirectory final : public SysFSDirectory {
public:
    static void initialize();
    virtual StringView name() const override { return "pci"sv; }

private:
    void enumerate_all_devices_and_add_pci_device_directories();

    PCIBusSysFSDirectory();
};

}
