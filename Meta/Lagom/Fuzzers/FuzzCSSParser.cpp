/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Window.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    Core::EventLoop loop;
    auto vm = JS::VM::create();
    auto realm = JS::Realm::create(*vm);
    auto window = Web::HTML::Window::create(*realm);
    auto document = Web::DOM::Document::create(*window);
    (void)Web::parse_css_stylesheet(Web::CSS::Parser::ParsingContext(document), { data, size });
    return 0;
}
