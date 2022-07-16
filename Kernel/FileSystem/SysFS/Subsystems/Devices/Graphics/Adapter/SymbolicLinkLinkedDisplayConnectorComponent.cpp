/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DisplayConnectorsDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/SymbolicLinkLinkedDisplayConnectorComponent.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSSymbolicLinkLinkedDisplayConnectorComponent>> SysFSSymbolicLinkLinkedDisplayConnectorComponent::try_create(SysFSDirectory const& parent_directory, size_t display_connector_index, SysFSComponent const& pointed_component)
{
    auto node_name = MUST(KString::formatted("{}", display_connector_index));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSSymbolicLinkLinkedDisplayConnectorComponent(move(node_name), parent_directory, pointed_component));
}

SysFSSymbolicLinkLinkedDisplayConnectorComponent::SysFSSymbolicLinkLinkedDisplayConnectorComponent(NonnullOwnPtr<KString> node_name, SysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
    , m_symlink_name(move(node_name))
{
}

}
