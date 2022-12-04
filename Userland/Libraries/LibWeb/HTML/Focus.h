/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

void run_focusing_steps(DOM::Node* new_focus_target, DOM::Node* fallback_target = nullptr, Optional<DeprecatedString> focus_trigger = {});
void run_unfocusing_steps(DOM::Node* old_focus_target);

}
