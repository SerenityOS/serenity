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

TOKENIZED_FEATURE(Location);
TOKENIZED_FEATURE(Menubar);
TOKENIZED_FEATURE(NoOpener);
TOKENIZED_FEATURE(NoReferrer);
TOKENIZED_FEATURE(Popup);
TOKENIZED_FEATURE(Resizable);
TOKENIZED_FEATURE(Scrollbars);
TOKENIZED_FEATURE(Status);
TOKENIZED_FEATURE(Toolbar);

}
