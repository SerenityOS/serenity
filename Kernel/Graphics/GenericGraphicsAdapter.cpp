/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DisplayConnectorsDirectory.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

GenericGraphicsAdapter::GenericGraphicsAdapter()
    : m_adapter_id(GraphicsManagement::generate_adapter_id())
{
}

RefPtr<GraphicsAdapterDisplayConnectorsSysFSDirectory> GenericGraphicsAdapter::graphics_adapter_display_connector_symlinks_sysfs_directory() const
{
    VERIFY(m_sysfs_directory);
    return m_sysfs_directory->display_connectors_symlinks_directory();
}

}
