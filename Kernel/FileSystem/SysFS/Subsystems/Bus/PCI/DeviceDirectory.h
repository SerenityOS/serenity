/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class PCIDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullLockRefPtr<PCIDeviceSysFSDirectory> create(SysFSDirectory const&, PCI::Address);
    PCI::Address const& address() const { return m_address; }

    virtual StringView name() const override { return m_device_directory_name->view(); }

private:
    PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, PCI::Address);

    PCI::Address m_address;

    NonnullOwnPtr<KString> m_device_directory_name;
};

}
