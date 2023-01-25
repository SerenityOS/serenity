/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class PCIDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullLockRefPtr<PCIDeviceSysFSDirectory> create(SysFSDirectory const&, PCI::DeviceIdentifier const&);
    PCI::DeviceIdentifier& device_identifier() const { return *m_device_identifier; }

    virtual StringView name() const override { return m_device_directory_name->view(); }

private:
    PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, PCI::DeviceIdentifier const&);

    NonnullRefPtr<PCI::DeviceIdentifier> m_device_identifier;

    NonnullOwnPtr<KString> m_device_directory_name;
};

}
