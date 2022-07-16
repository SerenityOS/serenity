/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/SymbolicLinkLinkedDisplayConnectorComponent.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/KString.h>

namespace Kernel {

class GraphicsManagement;
class GraphicsAdapterDisplayConnectorsSysFSDirectory final : public SysFSDirectory {
public:
    static ErrorOr<NonnullRefPtr<GraphicsAdapterDisplayConnectorsSysFSDirectory>> try_create(SysFSDirectory const&);

    virtual StringView name() const override { return "connectors"sv; }

    void plug_symlink_for_device(Badge<GraphicsManagement>, SysFSSymbolicLinkLinkedDisplayConnectorComponent&);
    void unplug_symlink_for_device(Badge<GraphicsManagement>, SysFSSymbolicLinkLinkedDisplayConnectorComponent&);

private:
    explicit GraphicsAdapterDisplayConnectorsSysFSDirectory(SysFSDirectory const&);
};

}
