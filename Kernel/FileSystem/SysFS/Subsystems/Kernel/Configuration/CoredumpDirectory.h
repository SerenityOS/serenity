/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/StringVariable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSCoredumpDirectory final : public SysFSSystemStringVariable {
public:
    virtual StringView name() const override { return "coredump_directory"sv; }
    static NonnullRefPtr<SysFSCoredumpDirectory> must_create(SysFSDirectory const&);

private:
    virtual ErrorOr<NonnullOwnPtr<KString>> value() const override;
    virtual void set_value(NonnullOwnPtr<KString> new_value) override;

    explicit SysFSCoredumpDirectory(SysFSDirectory const&);

    virtual mode_t permissions() const override;
};

}
