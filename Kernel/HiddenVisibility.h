/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// This header is injected into every source file via -include.
// It gives all declarations hidden visibility, preventing global variable accesses from going through the global offset table (GOT).
// GOT indirections are unnecessary in the kernel since it isn't dynamically linked.
// "-fvisibility=hidden" isn't sufficient because it doesn't make `extern` declarations hidden.

#pragma GCC visibility push(hidden)
