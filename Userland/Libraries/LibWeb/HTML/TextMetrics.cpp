/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/TextMetrics.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

JS::NonnullGCPtr<TextMetrics> TextMetrics::create(JS::Realm& realm)
{
    return realm.heap().allocate<TextMetrics>(realm, realm);
}

TextMetrics::TextMetrics(JS::Realm& realm)
    : PlatformObject(realm)
{
}

TextMetrics::~TextMetrics() = default;

void TextMetrics::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TextMetricsPrototype>(realm, "TextMetrics"));
}

}
