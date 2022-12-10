/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Keegan Saunders <keegan@undefinedbehaviour.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/BooleanVariable.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSKASANDeadly final : public SysFSSystemBooleanVariable {
public:
    virtual StringView name() const override { return "kasan_is_deadly"sv; }
    static NonnullLockRefPtr<SysFSKASANDeadly> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSKASANDeadly(SysFSDirectory const&);
};

}
