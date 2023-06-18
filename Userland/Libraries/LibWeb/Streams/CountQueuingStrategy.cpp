/*
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CountQueuingStrategyPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Streams/CountQueuingStrategy.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#blqs-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<CountQueuingStrategy>> CountQueuingStrategy::construct_impl(JS::Realm& realm, QueuingStrategyInit const& init)
{
    // The new CountQueuingStrategy(init) constructor steps are:
    // 1. Set this.[[highWaterMark]] to init["highWaterMark"].
    return MUST_OR_THROW_OOM(realm.heap().allocate<CountQueuingStrategy>(realm, realm, init.high_water_mark));
}

CountQueuingStrategy::CountQueuingStrategy(JS::Realm& realm, double high_water_mark)
    : PlatformObject(realm)
    , m_high_water_mark(high_water_mark)
{
}

CountQueuingStrategy::~CountQueuingStrategy() = default;

JS::ThrowCompletionOr<void> CountQueuingStrategy::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CountQueuingStrategyPrototype>(realm, "CountQueuingStrategy"));

    return {};
}

}
