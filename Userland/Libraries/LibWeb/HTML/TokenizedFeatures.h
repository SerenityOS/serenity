/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::HTML::TokenizedFeature {

#define TOKENIZED_FEATURE(Feature) \
    enum class Feature {           \
        Yes,                       \
        No,                        \
    }

TOKENIZED_FEATURE(NoOpener);

}
