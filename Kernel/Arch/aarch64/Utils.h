/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Prekernel {

// FIXME: to be replaced by real implementation from AK/Format.h
void dbgln(const char* text);
void warnln(const char* text);

}
