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

class PCIDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<PCIDeviceAttributeSysFSComponent> create(PCIDeviceSysFSDirectory const& device, PCI::RegisterOffset offset, size_t field_bytes_width);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~PCIDeviceAttributeSysFSComponent() {};

    virtual StringView name() const override;

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    PCIDeviceAttributeSysFSComponent(PCIDeviceSysFSDirectory const& device, PCI::RegisterOffset offset, size_t field_bytes_width);
    NonnullRefPtr<PCIDeviceSysFSDirectory> m_device;
    PCI::RegisterOffset m_offset;
    size_t m_field_bytes_width;
};

}
