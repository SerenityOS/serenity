/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class SysFSDeviceIdentifiersDirectory;
class SysFSSymbolicLinkDeviceComponent final
    : public SysFSSymbolicLink
    , public Weakable<SysFSSymbolicLinkDeviceComponent> {
    friend class SysFSComponentRegistry;

public:
    static ErrorOr<NonnullRefPtr<SysFSSymbolicLinkDeviceComponent>> try_create(SysFSDeviceIdentifiersDirectory const& parent_directory, Device const&, SysFSComponent const& pointed_component);
    virtual StringView name() const override { return m_major_minor_formatted_device_name->view(); }
    bool is_block_device() const { return m_block_device; }

private:
    SysFSSymbolicLinkDeviceComponent(SysFSDeviceIdentifiersDirectory const& parent_directory, NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const&, SysFSComponent const& pointed_component);
    IntrusiveListNode<SysFSSymbolicLinkDeviceComponent, NonnullRefPtr<SysFSSymbolicLinkDeviceComponent>> m_list_node;
    bool const m_block_device { false };
    NonnullOwnPtr<KString> m_major_minor_formatted_device_name;
};

}
