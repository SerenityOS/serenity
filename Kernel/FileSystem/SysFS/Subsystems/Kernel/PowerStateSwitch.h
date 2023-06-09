/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Directory.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Memory/MappedROM.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VirtualAddress.h>

namespace Kernel {

class SysFSPowerStateSwitchNode final : public SysFSComponent {
public:
    virtual StringView name() const override { return "power_state"sv; }
    static NonnullRefPtr<SysFSPowerStateSwitchNode> must_create(SysFSDirectory const&);
    virtual mode_t permissions() const override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override;
    virtual ErrorOr<void> truncate(u64) override;

private:
    explicit SysFSPowerStateSwitchNode(SysFSDirectory const&);
};

}
