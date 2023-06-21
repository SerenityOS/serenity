/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>

namespace Browser {

class CookieJar;
class Database;

struct CookieStorageKey;

}

namespace AK {

template<>
struct Traits<Browser::CookieStorageKey>;

}
