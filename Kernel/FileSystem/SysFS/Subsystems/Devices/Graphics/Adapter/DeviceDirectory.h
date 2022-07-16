/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DisplayConnectorsDirectory.h>
#include <Kernel/KString.h>

namespace Kernel {

class GraphicsAdapterSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<GraphicsAdapterSysFSDirectory> create(SysFSDirectory const& parent_directory, PCI::Address const&, u32 adapter_index);

    virtual StringView name() const override { return m_device_directory_name->view(); }

    RefPtr<GraphicsAdapterDisplayConnectorsSysFSDirectory> display_connectors_symlinks_directory() const;

private:
    GraphicsAdapterSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory);
    NonnullOwnPtr<KString> m_device_directory_name;

    RefPtr<GraphicsAdapterDisplayConnectorsSysFSDirectory> m_display_connectors_symlinks_directory;
};

}
