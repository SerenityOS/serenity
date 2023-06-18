/*
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::Streams {

// https://streams.spec.whatwg.org/#dictdef-queuingstrategyinit
struct QueuingStrategyInit {
    double high_water_mark;
};

}
