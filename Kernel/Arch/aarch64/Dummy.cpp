/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <AK/SetOnce.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/kstdio.h>

// Delay.cpp
namespace Kernel {

void microseconds_delay(u32)
{
    TODO_AARCH64();
}

}
