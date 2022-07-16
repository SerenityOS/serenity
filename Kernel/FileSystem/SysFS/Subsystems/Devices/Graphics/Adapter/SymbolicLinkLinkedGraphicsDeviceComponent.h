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

class GraphicsAdapterSysFSDirectory;
class SysFSSymbolicLinkLinkedGraphicsDeviceComponent final
    : public SysFSSymbolicLink
    , public Weakable<SysFSSymbolicLinkLinkedGraphicsDeviceComponent> {
    friend class SysFSComponentRegistry;

public:
    static ErrorOr<NonnullRefPtr<SysFSSymbolicLinkLinkedGraphicsDeviceComponent>> try_create(GraphicsAdapterSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);
    virtual StringView name() const override { return "linked_device"sv; }

private:
    SysFSSymbolicLinkLinkedGraphicsDeviceComponent(GraphicsAdapterSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);
};

}
