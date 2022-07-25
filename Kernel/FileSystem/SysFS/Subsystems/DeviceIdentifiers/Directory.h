/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSDeviceIdentifiersDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "dev"sv; }
    static NonnullRefPtr<SysFSDeviceIdentifiersDirectory> must_create(SysFSRootDirectory const&);

    static SysFSDeviceIdentifiersDirectory& the();

private:
    explicit SysFSDeviceIdentifiersDirectory(SysFSRootDirectory const&);
};

}
