/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class BIOSSysFSDirectory : public SysFSDirectory {
public:
    virtual StringView name() const override { return "bios"sv; }
    static NonnullLockRefPtr<BIOSSysFSDirectory> must_create(FirmwareSysFSDirectory&);

    void create_components();

private:
    explicit BIOSSysFSDirectory(FirmwareSysFSDirectory&);

    void set_dmi_64_bit_entry_initialization_values();
    void set_dmi_32_bit_entry_initialization_values();
    void initialize_dmi_exposer();

    Optional<PhysicalAddress> find_dmi_entry64bit_point();
    Optional<PhysicalAddress> find_dmi_entry32bit_point();

    PhysicalAddress m_dmi_entry_point;
    PhysicalAddress m_smbios_structure_table;
    bool m_using_64bit_dmi_entry_point { false };
    size_t m_smbios_structure_table_length { 0 };
    size_t m_dmi_entry_point_length { 0 };
};

}
