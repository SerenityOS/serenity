/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/BooleanVariable.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSUBSANDeadly final : public SysFSSystemBoolean {
public:
    virtual StringView name() const override { return "ubsan_is_deadly"sv; }
    static NonnullLockRefPtr<SysFSUBSANDeadly> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSUBSANDeadly(SysFSDirectory const&);
};

}
