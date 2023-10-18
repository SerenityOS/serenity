/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Singleton.h>
#include <AK/Vector.h>
#include <Kernel/Driver.h>
#include <Kernel/FileSystem/FileSystem.h>

namespace Kernel::FS {

#define FS_DRIVER(driver_name) REGISTER_DRIVER(driver_name)

class Driver;
extern Singleton<Vector<AK::NonnullLockRefPtr<Driver>>> file_system_drivers;

class Driver : public Kernel::Driver {
public:
    Driver(StringView name)
        : Kernel::Driver(name)
    {
        file_system_drivers->append(*this);
    }

    virtual ErrorOr<NonnullRefPtr<FileSystem>> probe(OpenFileDescription&, ReadonlyBytes) = 0;

    virtual ~Driver() = default;
};

}
