/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/BooleanVariable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class SysFSDumpKmallocStacks final : public SysFSSystemBooleanVariable {
public:
    virtual StringView name() const override { return "kmalloc_stacks"sv; }
    static NonnullRefPtr<SysFSDumpKmallocStacks> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSDumpKmallocStacks(SysFSDirectory const&);

    mutable Spinlock<LockRank::None> m_lock {};
};

}
