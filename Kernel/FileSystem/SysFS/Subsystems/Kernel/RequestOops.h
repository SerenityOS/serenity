/*
 * Copyright (c) 2026, bogdan-glitchm <github.34373732+copyright@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSSystemRequestOops final : public SysFSComponent {
public:
    static NonnullRefPtr<SysFSSystemRequestOops> must_create(SysFSDirectory const& parent_directory);
    virtual StringView name() const override { return "request_oops"sv; }

private:
    explicit SysFSSystemRequestOops(SysFSDirectory const& parent_directory)
        : SysFSComponent(parent_directory)
    {
    }

    // ^SysFSComponent
    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override;
    virtual mode_t permissions() const override { return 3; }
    virtual ErrorOr<void> truncate(u64) override;
};

}
