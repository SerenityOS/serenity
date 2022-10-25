/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/SafeFunction.h>

namespace Web::Fetch::Infrastructure {

void queue_fetch_task(JS::Object&, JS::SafeFunction<void()>);

}
