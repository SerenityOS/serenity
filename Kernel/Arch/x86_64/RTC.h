/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::RTC {

void initialize();
time_t now();
UnixDateTime boot_time();

}
