/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DisplayConnectorsDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/SymbolicLinkLinkedDisplayConnectorComponent.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Sections.h>

namespace Kernel {

void GraphicsAdapterDisplayConnectorsSysFSDirectory::plug_symlink_for_device(Badge<GraphicsManagement>, SysFSSymbolicLinkLinkedDisplayConnectorComponent& new_display_connector_symlink)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_display_connector_symlink);
        return {};
    }));
}
void GraphicsAdapterDisplayConnectorsSysFSDirectory::unplug_symlink_for_device(Badge<GraphicsManagement>, SysFSSymbolicLinkLinkedDisplayConnectorComponent& removed_display_connector_symlink)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_display_connector_symlink);
        return {};
    }));
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<GraphicsAdapterDisplayConnectorsSysFSDirectory>> GraphicsAdapterDisplayConnectorsSysFSDirectory::try_create(SysFSDirectory const& parent_directory)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) GraphicsAdapterDisplayConnectorsSysFSDirectory(parent_directory));
}

UNMAP_AFTER_INIT GraphicsAdapterDisplayConnectorsSysFSDirectory::GraphicsAdapterDisplayConnectorsSysFSDirectory(SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
