/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Runtime {

void run_possibly_throwing_callback(void (*callback)()) noexcept;
void run_possibly_throwing_callback(void (*callback)(void* argument), void* argument) noexcept;

}
