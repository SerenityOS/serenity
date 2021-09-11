/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS.h>

namespace Kernel {

class FirmwareSysFSDirectory : public SysFSDirectory {
public:
    static void initialize();

    void create_components();

private:
    FirmwareSysFSDirectory();
};

}
