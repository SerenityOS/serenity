/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

using DriverInitFunction = void (*)();
extern "C" DriverInitFunction driver_init_table_start[];
extern "C" DriverInitFunction driver_init_table_end[];

#define DRIVER_INIT_FUNCTION(driver_name, driver_init_function) static Kernel::DriverInitFunction driver_init_function_ptr_##driver_name [[gnu::section(".driver_init"), gnu::used]] = &driver_init_function

}
