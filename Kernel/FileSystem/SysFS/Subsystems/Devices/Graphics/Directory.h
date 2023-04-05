/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSGraphicsDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "graphics"sv; }
    static NonnullRefPtr<SysFSGraphicsDirectory> must_create(SysFSDevicesDirectory const&);

private:
    explicit SysFSGraphicsDirectory(SysFSDevicesDirectory const&);
};

}
