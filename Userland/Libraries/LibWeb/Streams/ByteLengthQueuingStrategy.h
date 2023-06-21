/*
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Streams/QueuingStrategyInit.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#bytelengthqueuingstrategy
class ByteLengthQueuingStrategy final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(ByteLengthQueuingStrategy, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ByteLengthQueuingStrategy>> construct_impl(JS::Realm&, QueuingStrategyInit const&);

    virtual ~ByteLengthQueuingStrategy() override;

    // https://streams.spec.whatwg.org/#blqs-high-water-mark
    double high_water_mark() const
    {
        // The highWaterMark getter steps are:
        // 1. Return this.[[highWaterMark]].
        return m_high_water_mark;
    }

private:
    explicit ByteLengthQueuingStrategy(JS::Realm&, double high_water_mark);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    // https://streams.spec.whatwg.org/#bytelengthqueuingstrategy-highwatermark
    double m_high_water_mark { 0 };
};

}
