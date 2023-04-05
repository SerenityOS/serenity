/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>

namespace Kernel {

class SysFSGlobalKernelStatsDirectory : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSGlobalKernelStatsDirectory> must_create(SysFSRootDirectory const&);
    virtual StringView name() const override { return "kernel"sv; }

private:
    explicit SysFSGlobalKernelStatsDirectory(SysFSDirectory const&);
};

}
