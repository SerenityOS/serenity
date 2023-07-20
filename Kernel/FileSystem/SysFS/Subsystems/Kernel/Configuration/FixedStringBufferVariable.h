/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Function.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

class SysFSSystemFixedStringBufferVariable : public SysFSGlobalInformation {
protected:
    explicit SysFSSystemFixedStringBufferVariable(SysFSDirectory const& parent_directory, FixedArray<u8>&& write_storage)
        : SysFSGlobalInformation(parent_directory)
        , m_storage_during_write_buffer_size(write_storage.size())
        , m_storage_during_write(move(write_storage))
    {
    }
    virtual ErrorOr<NonnullOwnPtr<KString>> value() const = 0;
    virtual ErrorOr<void> set_value(StringView new_value) = 0;

private:
    // ^SysFSGlobalInformation
    virtual ErrorOr<void> try_generate(KBufferBuilder&) override final;

    // ^SysFSExposedComponent
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override final;
    virtual mode_t permissions() const override { return 0644; }
    virtual ErrorOr<void> truncate(u64) override final;

    size_t const m_storage_during_write_buffer_size { 0 };
    SpinlockProtected<FixedArray<u8>, LockRank::None> m_storage_during_write;
};

}
