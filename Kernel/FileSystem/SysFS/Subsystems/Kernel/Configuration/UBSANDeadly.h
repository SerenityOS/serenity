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

namespace Kernel {

class SysFSUBSANDeadly final : public SysFSSystemBooleanVariable {
public:
    virtual StringView name() const override { return "ubsan_is_deadly"sv; }
    static NonnullRefPtr<SysFSUBSANDeadly> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSUBSANDeadly(SysFSDirectory const&);
};

}
