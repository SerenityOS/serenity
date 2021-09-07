/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS.h>

namespace Kernel::PCI {

class PCIBusSysFSDirectory final : public SysFSDirectory {
public:
    static void initialize();

private:
    PCIBusSysFSDirectory();
};

class PCIDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<PCIDeviceSysFSDirectory> create(const SysFSDirectory&, Address);
    const Address& address() const { return m_address; }

private:
    PCIDeviceSysFSDirectory(const SysFSDirectory&, Address);

    Address m_address;
};

class PCIDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<PCIDeviceAttributeSysFSComponent> create(String name, const PCIDeviceSysFSDirectory& device, size_t offset, size_t field_bytes_width);

    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~PCIDeviceAttributeSysFSComponent() {};

protected:
    KResultOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    PCIDeviceAttributeSysFSComponent(String name, const PCIDeviceSysFSDirectory& device, size_t offset, size_t field_bytes_width);
    NonnullRefPtr<PCIDeviceSysFSDirectory> m_device;
    size_t m_offset;
    size_t m_field_bytes_width;
};

}
