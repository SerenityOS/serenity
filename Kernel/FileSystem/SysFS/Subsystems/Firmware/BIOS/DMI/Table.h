/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/BIOS/Component.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class SMBIOSExposedTable final : public BIOSSysFSComponent {
public:
    virtual StringView name() const override { return "DMI"sv; }
    static NonnullRefPtr<SMBIOSExposedTable> must_create(PhysicalAddress, size_t blob_size);

private:
    SMBIOSExposedTable(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const override;

    virtual size_t size() const override { return m_smbios_structure_table_length; }

    PhysicalAddress m_smbios_structure_table;
    size_t const m_smbios_structure_table_length { 0 };
};

}
