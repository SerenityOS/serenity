/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {
class GenericGraphicsAdapter
    : public RefCounted<GenericGraphicsAdapter>
    , public Weakable<GenericGraphicsAdapter> {
    friend class GraphicsManagement;

public:
    virtual ~GenericGraphicsAdapter() = default;

    virtual ErrorOr<void> initialize_after_sysfs_directory_creation() = 0;

    u32 adapter_id() const { return m_adapter_id; }

protected:
    GenericGraphicsAdapter();

    virtual void after_inserting() = 0;
    RefPtr<GraphicsAdapterSysFSDirectory> m_sysfs_directory;

private:
    u32 const m_adapter_id { 0 };
};

}
