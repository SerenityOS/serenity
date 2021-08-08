/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <Kernel/Bus/USB/USBController.h>

namespace Kernel::USB {

class USBManagement {
    AK_MAKE_ETERNAL;

public:
    USBManagement();
    static bool initialized();
    static void initialize();
    static USBManagement& the();

private:
    void enumerate_controllers();

    USBController::List m_controllers;
};

}
