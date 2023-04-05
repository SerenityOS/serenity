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

class SysFSGlobalNetworkStatsDirectory : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSGlobalNetworkStatsDirectory> must_create(SysFSDirectory const&);
    virtual StringView name() const override { return "net"sv; }

private:
    explicit SysFSGlobalNetworkStatsDirectory(SysFSDirectory const&);
};

}
