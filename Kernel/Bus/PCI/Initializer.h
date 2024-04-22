/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SetOnce.h>

namespace Kernel::PCI {

extern SetOnce g_pci_access_io_probe_failed;
extern SetOnce g_pci_access_is_disabled_from_commandline;

void initialize();

}
