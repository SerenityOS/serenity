/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/Directory.h>

namespace Kernel {

class SysFSRootDirectory final : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "."sv; }
    static NonnullRefPtr<SysFSRootDirectory> create();

private:
    virtual bool is_root_directory() const override final { return true; }
    SysFSRootDirectory();
    NonnullRefPtr<SysFSBusDirectory> const m_buses_directory;
};

}
