/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/TextMetrics.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

JS::NonnullGCPtr<TextMetrics> TextMetrics::create(HTML::Window& window)
{
    return *window.heap().allocate<TextMetrics>(window.realm(), window);
}

TextMetrics::TextMetrics(HTML::Window& window)
    : PlatformObject(window.realm())
{
    set_prototype(&window.cached_web_prototype("TextMetrics"));
}

TextMetrics::~TextMetrics() = default;

}
