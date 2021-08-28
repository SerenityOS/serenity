/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// This is a temporary file to get a non-empty Kernel binary on aarch64.
// The prekernel currently never jumps to the kernel. This is dead code.
void dummy();
void dummy() { }
