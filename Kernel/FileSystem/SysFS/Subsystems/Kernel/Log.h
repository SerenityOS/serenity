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

class SysFSKernelLog final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override { return "dmesg"sv; }
    static NonnullRefPtr<SysFSKernelLog> must_create(SysFSDirectory const& parent_directory);

    virtual mode_t permissions() const override;

private:
    explicit SysFSKernelLog(SysFSDirectory const& parent_directory);
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;
};

}
