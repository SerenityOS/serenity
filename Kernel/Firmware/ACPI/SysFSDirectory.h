/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>

#pragma once

namespace Kernel::ACPI {

class ACPISysFSDirectory : public SysFSDirectory {
public:
    virtual StringView name() const override { return "acpi"sv; }
    static NonnullLockRefPtr<ACPISysFSDirectory> must_create(SysFSFirmwareDirectory& firmware_directory);

private:
    void find_tables_and_register_them_as_components();
    explicit ACPISysFSDirectory(SysFSFirmwareDirectory& firmware_directory);
};

}
