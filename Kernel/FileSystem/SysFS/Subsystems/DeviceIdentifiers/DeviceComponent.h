/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class SysFSDeviceComponent final
    : public SysFSComponent
    , public LockWeakable<SysFSDeviceComponent> {
    friend class SysFSBlockDevicesDirectory;
    friend class SysFSCharacterDevicesDirectory;

public:
    static NonnullRefPtr<SysFSDeviceComponent> must_create(Device const&);
    virtual StringView name() const override { return m_major_minor_formatted_device_name->view(); }
    bool is_block_device() const { return m_block_device; }

private:
    SysFSDeviceComponent(NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const&);
    bool m_block_device;

    NonnullOwnPtr<KString> m_major_minor_formatted_device_name;
};

}
