/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace WebDriver {

struct HttpError {
    unsigned http_status;
    String error;
    String message;
};

}
