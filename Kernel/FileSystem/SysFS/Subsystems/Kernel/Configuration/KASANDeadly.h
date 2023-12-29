/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/BooleanVariable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSKASANDeadly final : public SysFSSystemBooleanVariable {
public:
    virtual StringView name() const override { return "kasan_is_deadly"sv; }
    static NonnullRefPtr<SysFSKASANDeadly> must_create(SysFSDirectory const&);

private:
    virtual bool value() const override;
    virtual void set_value(bool new_value) override;

    explicit SysFSKASANDeadly(SysFSDirectory const&);
};

}
