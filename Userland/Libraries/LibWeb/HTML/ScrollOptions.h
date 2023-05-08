/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/WindowGlobalMixin.h>

namespace Web::HTML {

// https://w3c.github.io/csswg-drafts/cssom-view/#dictdef-scrolloptions
struct ScrollOptions {
    Bindings::ScrollBehavior behavior { Bindings::ScrollBehavior::Auto };
};

}
