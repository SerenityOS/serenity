/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSNetworkRouteStats final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override { return "route"sv; }
    static NonnullRefPtr<SysFSNetworkRouteStats> must_create(SysFSDirectory const&);

private:
    explicit SysFSNetworkRouteStats(SysFSDirectory const&);
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;

    virtual bool is_readable_by_jailed_processes() const override { return true; }
};

}
