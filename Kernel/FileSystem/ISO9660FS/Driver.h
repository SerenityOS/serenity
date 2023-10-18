/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystemDriver.h>

namespace Kernel::FS {

class ISO9660FSDriver : public Driver {
public:
    ISO9660FSDriver()
        : Driver("ISO9660FS"sv)
    {
    }

    static void init();

    virtual ErrorOr<NonnullRefPtr<FileSystem>> probe(OpenFileDescription&, ReadonlyBytes) override;

    virtual ~ISO9660FSDriver() = default;
};

}
