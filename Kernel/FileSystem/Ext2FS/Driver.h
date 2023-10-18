/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystemDriver.h>

namespace Kernel::FS {

class Ext2FSDriver : public Driver {
public:
    Ext2FSDriver()
        : Driver("Ext2FS"sv)
    {
    }

    static void init();

    virtual ErrorOr<NonnullRefPtr<FileSystem>> probe(OpenFileDescription&, ReadonlyBytes) override;

    virtual ~Ext2FSDriver() = default;
};

}
