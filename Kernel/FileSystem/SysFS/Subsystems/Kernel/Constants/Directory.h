/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>

namespace Kernel {

class SysFSGlobalKernelConstantsDirectory : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSGlobalKernelConstantsDirectory> must_create(SysFSDirectory const&);
    virtual StringView name() const override { return "constants"sv; }

private:
    explicit SysFSGlobalKernelConstantsDirectory(SysFSDirectory const&);
};

}
