/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSStorageDirectory;
class SysFSStorageLogicalDevicesDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "logical"sv; }
    static NonnullRefPtr<SysFSStorageLogicalDevicesDirectory> must_create(SysFSStorageDirectory const&);

private:
    explicit SysFSStorageLogicalDevicesDirectory(SysFSStorageDirectory const&);
};

}
