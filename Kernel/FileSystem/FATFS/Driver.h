/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystemDriver.h>

namespace Kernel::FS {

class FATFSDriver : public Driver {
public:
    FATFSDriver()
        : Driver("FATFS"sv)
    {
    }

    static void init();

    virtual ErrorOr<NonnullRefPtr<FileSystem>> probe(OpenFileDescription&, ReadonlyBytes) override;

    virtual ~FATFSDriver() = default;
};

}
