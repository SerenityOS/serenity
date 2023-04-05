/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "devices"sv; }
    static NonnullRefPtr<SysFSDevicesDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSDevicesDirectory(SysFSRootDirectory const&);
};

}
