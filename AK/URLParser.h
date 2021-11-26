/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/URL.h>

namespace AK {

#define ENUMERATE_STATES                 \
    STATE(SchemeStart)                   \
    STATE(Scheme)                        \
    STATE(NoScheme)                      \
    STATE(SpecialRelativeOrAuthority)    \
    STATE(PathOrAuthority)               \
    STATE(Relative)                      \
    STATE(RelativeSlash)                 \
    STATE(SpecialAuthoritySlashes)       \
    STATE(SpecialAuthorityIgnoreSlashes) \
    STATE(Authority)                     \
    STATE(Host)                          \
    STATE(Hostname)                      \
    STATE(Port)                          \
    STATE(File)                          \
    STATE(FileSlash)                     \
    STATE(FileHost)                      \
    STATE(PathStart)                     \
    STATE(Path)                          \
    STATE(CannotBeABaseUrlPath)          \
    STATE(Query)                         \
    STATE(Fragment)

class URLParser {
public:
    enum class State {
#define STATE(state) state,
        ENUMERATE_STATES
#undef STATE
    };

    static char const* state_name(State const& state)
    {
        switch (state) {
#define STATE(state)   \
    case State::state: \
        return #state;
            ENUMERATE_STATES
#undef STATE
        }
        VERIFY_NOT_REACHED();
    }

    static URL parse(StringView input, URL const* base_url = nullptr, Optional<URL> url = {}, Optional<State> state_override = {});

private:
    static Optional<URL> parse_data_url(StringView raw_input);
};

#undef ENUMERATE_STATES

}

using AK::URLParser;
