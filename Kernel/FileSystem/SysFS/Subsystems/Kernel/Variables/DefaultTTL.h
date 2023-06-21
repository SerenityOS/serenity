/*
 * Copyright (c) 2023, Mark Douglas <dmarkd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/IntegerVariable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSDefaultTTL final : public SysFSSystemIntegerVariable {
public:
    virtual StringView name() const override { return "default_ttl"sv; }
    static NonnullRefPtr<SysFSDefaultTTL> must_create(SysFSDirectory const&);

private:
    virtual i32 value() const override;
    virtual void set_value(i32 new_value) override;

    explicit SysFSDefaultTTL(SysFSDirectory const&);
};

}
