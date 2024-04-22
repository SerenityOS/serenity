/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/SetOnce.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

class SysFSBIOSDirectory : public SysFSDirectory {
public:
    virtual StringView name() const override { return "bios"sv; }
    static NonnullRefPtr<SysFSBIOSDirectory> must_create(SysFSFirmwareDirectory&);

    void create_components();

private:
    explicit SysFSBIOSDirectory(SysFSFirmwareDirectory&);

    void set_dmi_64_bit_entry_initialization_values();
    void set_dmi_32_bit_entry_initialization_values();
    void initialize_dmi_exposer();

    Optional<PhysicalAddress> find_dmi_entry64bit_point();
    Optional<PhysicalAddress> find_dmi_entry32bit_point();

    PhysicalAddress m_dmi_entry_point;
    PhysicalAddress m_smbios_structure_table;
    SetOnce m_using_64bit_dmi_entry_point;
    size_t m_smbios_structure_table_length { 0 };
    size_t m_dmi_entry_point_length { 0 };
};

}
