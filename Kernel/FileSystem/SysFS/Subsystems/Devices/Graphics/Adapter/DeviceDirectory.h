/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class GraphicsAdapterAttributeSysFSComponent;
class GraphicsAdapterSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<GraphicsAdapterSysFSDirectory> create(SysFSDirectory const& parent_directory, u32 adapter_index);

    virtual StringView name() const override { return m_device_directory_name->view(); }

private:
    GraphicsAdapterSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory);
    NonnullOwnPtr<KString> m_device_directory_name;
};

}
