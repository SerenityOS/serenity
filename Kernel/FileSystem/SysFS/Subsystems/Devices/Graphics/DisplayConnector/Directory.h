/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class DisplayConnectorSysFSDirectory;
class DisplayConnector;
class SysFSGraphicsDirectory;
class SysFSDisplayConnectorsDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "connectors"sv; }
    static SysFSDisplayConnectorsDirectory& the();
    static NonnullRefPtr<SysFSDisplayConnectorsDirectory> must_create(SysFSGraphicsDirectory const&);

    void plug(Badge<DisplayConnector>, DisplayConnectorSysFSDirectory&);
    void unplug(Badge<DisplayConnector>, SysFSDirectory&);

private:
    explicit SysFSDisplayConnectorsDirectory(SysFSGraphicsDirectory const&);
};

}
