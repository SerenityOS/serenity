/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceDirectory.h>

namespace Kernel {

class PCIDeviceExpansionROMSysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<PCIDeviceExpansionROMSysFSComponent> create(PCIDeviceSysFSDirectory const& device);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~PCIDeviceExpansionROMSysFSComponent() {};

    virtual StringView name() const override { return "rom"sv; }

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer(size_t offset_in_rom, size_t count) const;
    PCIDeviceExpansionROMSysFSComponent(PCIDeviceSysFSDirectory const& device, size_t option_rom_size);
    NonnullRefPtr<PCIDeviceSysFSDirectory> m_device;
    size_t const m_option_rom_size { 0 };
};

}
