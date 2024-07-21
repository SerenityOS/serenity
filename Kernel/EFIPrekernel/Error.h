/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

#include <Kernel/Firmware/EFI/EFI.h>

namespace Kernel {

template<typename T>
using EFIErrorOr = ErrorOr<T, EFI::Status>;

}
