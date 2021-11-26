/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/UnixTypes.h>

namespace RTC {

void initialize();
time_t now();
time_t boot_time();

}
