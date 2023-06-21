/*
 * Copyright (c) 2023, Mark Douglas <dmarkd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

class SysFSSystemIntegerVariable : public SysFSGlobalInformation {
protected:
    explicit SysFSSystemIntegerVariable(SysFSDirectory const& parent_directory)
        : SysFSGlobalInformation(parent_directory)
    {
    }
    virtual i32 value() const = 0;
    virtual void set_value(i32 new_value) = 0;

private:
    // ^SysFSGlobalInformation
    virtual ErrorOr<void> try_generate(KBufferBuilder&) override final;

    // ^SysFSExposedComponent
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override final;
    virtual mode_t permissions() const override final { return 0644; }
    virtual ErrorOr<void> truncate(u64) override final;
};

}
