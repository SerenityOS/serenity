/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSBusDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "bus"sv; }
    static NonnullRefPtr<SysFSBusDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSBusDirectory(SysFSRootDirectory const&);
};

}
