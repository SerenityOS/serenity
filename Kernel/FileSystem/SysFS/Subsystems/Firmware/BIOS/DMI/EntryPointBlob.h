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
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/BIOS/DMI/Definitions.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class DMIEntryPointExposedBlob final : public BIOSSysFSComponent {
public:
    virtual StringView name() const override { return "smbios_entry_point"sv; }
    static NonnullRefPtr<DMIEntryPointExposedBlob> must_create(PhysicalAddress dmi_entry_point, size_t blob_size);

private:
    DMIEntryPointExposedBlob(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const override;

    virtual size_t size() const override { return m_dmi_entry_point_length; }

    PhysicalAddress m_dmi_entry_point;
    size_t const m_dmi_entry_point_length { 0 };
};

}
