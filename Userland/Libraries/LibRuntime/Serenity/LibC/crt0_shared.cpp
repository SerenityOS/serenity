/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

extern "C" void* __dso_handle;
[[gnu::visibility("hidden"), gnu::weak]] void* __dso_handle = nullptr;
