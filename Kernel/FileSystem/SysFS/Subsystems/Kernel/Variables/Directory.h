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

class SysFSGlobalKernelVariablesDirectory : public SysFSDirectory {
public:
    static NonnullLockRefPtr<SysFSGlobalKernelVariablesDirectory> must_create(SysFSDirectory const&);
    virtual StringView name() const override { return "variables"sv; }

private:
    explicit SysFSGlobalKernelVariablesDirectory(SysFSDirectory const&);
};

}
