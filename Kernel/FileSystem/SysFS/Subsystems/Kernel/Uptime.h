/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSUptime final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override { return "uptime"sv; }
    static NonnullRefPtr<SysFSUptime> must_create(SysFSDirectory const& parent_directory);

private:
    explicit SysFSUptime(SysFSDirectory const& parent_directory);
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;

    virtual bool is_readable_by_jailed_processes() const override { return true; }
};

}
