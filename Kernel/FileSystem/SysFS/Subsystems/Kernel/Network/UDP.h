/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSNetworkUDPStats final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override { return "udp"sv; }
    static NonnullLockRefPtr<SysFSNetworkUDPStats> must_create(SysFSDirectory const&);

private:
    explicit SysFSNetworkUDPStats(SysFSDirectory const&);
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;
};

}
