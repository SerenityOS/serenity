/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/TextMetrics.h>

namespace Web::HTML {

JS::NonnullGCPtr<TextMetrics> TextMetrics::create(JS::Realm& realm)
{
    return *realm.heap().allocate<TextMetrics>(realm, realm);
}

TextMetrics::TextMetrics(JS::Realm& realm)
    : PlatformObject(realm)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "TextMetrics"));
}

TextMetrics::~TextMetrics() = default;

}
