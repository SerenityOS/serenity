/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class SysFSGlobalInformation : public SysFSComponent {
public:
    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const override;

protected:
    explicit SysFSGlobalInformation(SysFSDirectory const& parent_directory);
    virtual ErrorOr<void> refresh_data(OpenFileDescription&) const override;
    virtual ErrorOr<void> try_generate(KBufferBuilder&) = 0;

    virtual bool is_readable_by_jailed_processes() const { return false; }

    mutable Mutex m_refresh_lock;
};

}
