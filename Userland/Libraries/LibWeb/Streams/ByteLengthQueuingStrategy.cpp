/*
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ByteLengthQueuingStrategyPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Streams/ByteLengthQueuingStrategy.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#blqs-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<ByteLengthQueuingStrategy>> ByteLengthQueuingStrategy::construct_impl(JS::Realm& realm, QueuingStrategyInit const& init)
{
    // The new ByteLengthQueuingStrategy(init) constructor steps are:
    // 1. Set this.[[highWaterMark]] to init["highWaterMark"].
    return MUST_OR_THROW_OOM(realm.heap().allocate<ByteLengthQueuingStrategy>(realm, realm, init.high_water_mark));
}

ByteLengthQueuingStrategy::ByteLengthQueuingStrategy(JS::Realm& realm, double high_water_mark)
    : PlatformObject(realm)
    , m_high_water_mark(high_water_mark)
{
}

ByteLengthQueuingStrategy::~ByteLengthQueuingStrategy() = default;

JS::ThrowCompletionOr<void> ByteLengthQueuingStrategy::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ByteLengthQueuingStrategyPrototype>(realm, "ByteLengthQueuingStrategy"));

    return {};
}

}
