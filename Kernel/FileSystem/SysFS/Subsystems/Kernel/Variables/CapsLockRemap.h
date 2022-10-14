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

class SysFSCapsLockRemap final : public SysFSSystemBoolean {
public:
    virtual StringView name() const override { return "caps_lock_to_ctrl"sv; }
    static NonnullLockRefPtr<SysFSCapsLockRemap> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSCapsLockRemap(SysFSDirectory const&);

    mutable Mutex m_lock;
};

}
