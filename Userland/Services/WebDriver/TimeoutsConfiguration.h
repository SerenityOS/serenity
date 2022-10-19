/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Optional.h>

namespace WebDriver {

// https://w3c.github.io/webdriver/#dfn-timeouts-configuration
struct TimeoutsConfiguration {
    Optional<u64> script_timeout { 30'000 };
    u64 page_load_timeout { 300'000 };
    u64 implicit_wait_timeout { 0 };
};

}
