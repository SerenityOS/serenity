/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/SymbolicLinkLinkedGraphicsDeviceComponent.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSSymbolicLinkLinkedGraphicsDeviceComponent>> SysFSSymbolicLinkLinkedGraphicsDeviceComponent::try_create(GraphicsAdapterSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSSymbolicLinkLinkedGraphicsDeviceComponent(parent_directory, pointed_component));
}

SysFSSymbolicLinkLinkedGraphicsDeviceComponent::SysFSSymbolicLinkLinkedGraphicsDeviceComponent(GraphicsAdapterSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
{
}

}
