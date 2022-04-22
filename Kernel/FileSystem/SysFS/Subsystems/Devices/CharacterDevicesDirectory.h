/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>

namespace Kernel {

class SysFSCharacterDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "char"sv; }
    static NonnullRefPtr<SysFSCharacterDevicesDirectory> must_create(SysFSDevicesDirectory const&);
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSCharacterDevicesDirectory(SysFSDevicesDirectory const&);
};

}
