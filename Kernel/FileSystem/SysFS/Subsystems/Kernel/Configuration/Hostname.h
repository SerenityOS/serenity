/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/FixedStringBufferVariable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSHostnameString final : public SysFSSystemFixedStringBufferVariable {
public:
    virtual StringView name() const override { return "hostname"sv; }
    static NonnullRefPtr<SysFSHostnameString> must_create(SysFSDirectory const&);

private:
    virtual ErrorOr<NonnullOwnPtr<KString>> value() const override;
    virtual ErrorOr<void> set_value(StringView new_value) override;

    SysFSHostnameString(SysFSDirectory const&, FixedArray<u8>&& write_storage);

    virtual mode_t permissions() const override;
};

}
