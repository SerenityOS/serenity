/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS.h>

namespace Kernel::PCI {

class PCIBusSysFSDirectory final : public SysFSDirectory {
public:
    static void initialize();
    virtual StringView name() const override { return "pci"sv; }

private:
    PCIBusSysFSDirectory();
};

class PCIDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<PCIDeviceSysFSDirectory> create(const SysFSDirectory&, Address);
    const Address& address() const { return m_address; }

    virtual StringView name() const override { return m_device_directory_name->view(); }

private:
    PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, const SysFSDirectory&, Address);

    Address m_address;

    NonnullOwnPtr<KString> m_device_directory_name;
};

class PCIDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<PCIDeviceAttributeSysFSComponent> create(const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~PCIDeviceAttributeSysFSComponent() {};

    virtual StringView name() const override;

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    PCIDeviceAttributeSysFSComponent(const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width);
    NonnullRefPtr<PCIDeviceSysFSDirectory> m_device;
    PCI::RegisterOffset m_offset;
    size_t m_field_bytes_width;
};

}
