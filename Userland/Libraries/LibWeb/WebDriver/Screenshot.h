/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebDriver/Response.h>

namespace Web::WebDriver {

using Painter = Function<void(Gfx::IntRect const&, Gfx::Bitmap&)>;
Response capture_element_screenshot(Painter const& painter, Page& page, DOM::Element& element, Gfx::IntRect& rect);

}
