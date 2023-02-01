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
    return realm.heap().allocate<TextMetrics>(realm, realm).release_allocated_value_but_fixme_should_propagate_errors();
}

TextMetrics::TextMetrics(JS::Realm& realm)
    : PlatformObject(realm)
{
}

TextMetrics::~TextMetrics() = default;

JS::ThrowCompletionOr<void> TextMetrics::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TextMetricsPrototype>(realm, "TextMetrics"));

    return {};
}

}
