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

class SysFSKernelLoadBase final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override { return "load_base"sv; }

    static NonnullLockRefPtr<SysFSKernelLoadBase> must_create(SysFSDirectory const& parent_directory);

private:
    explicit SysFSKernelLoadBase(SysFSDirectory const& parent_directory);

    virtual mode_t permissions() const override;

    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;
};

}
