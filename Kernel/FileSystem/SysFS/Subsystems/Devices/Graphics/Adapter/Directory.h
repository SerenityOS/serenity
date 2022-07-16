/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class GraphicsAdapterSysFSDirectory;
class VirtIOGraphicsAdapter;
class PCIGraphicsAdapter;
class SysFSGraphicsDirectory;
class SysFSGraphicsAdaptersDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "adapters"sv; }
    static SysFSGraphicsAdaptersDirectory& the();
    static NonnullRefPtr<SysFSGraphicsAdaptersDirectory> must_create(SysFSGraphicsDirectory const&);

    void plug_pci_adapter(Badge<PCIGraphicsAdapter>, GraphicsAdapterSysFSDirectory&);
    void unplug_pci_adapter(Badge<PCIGraphicsAdapter>, SysFSDirectory&);

    // Note: We will need to eventually get rid of these methods once the VirtIO code
    // uses the right abstractions - inherting from PCIGraphicsAdapter or even better - some
    // other agnostic Badge (preferably like GenericGraphicsAdapter).
    void plug_virtio_adapter(Badge<VirtIOGraphicsAdapter>, GraphicsAdapterSysFSDirectory&);
    void unplug_virtio_adapter(Badge<VirtIOGraphicsAdapter>, SysFSDirectory&);

private:
    explicit SysFSGraphicsAdaptersDirectory(SysFSGraphicsDirectory const&);
};

}
