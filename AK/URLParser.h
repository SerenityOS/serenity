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

class URLParser {
public:
    enum class State {
        SchemeStart,
        Scheme,
        NoScheme,
        SpecialRelativeOrAuthority,
        PathOrAuthority,
        Relative,
        RelativeSlash,
        SpecialAuthoritySlashes,
        SpecialAuthorityIgnoreSlashes,
        Authority,
        Host,
        Hostname,
        Port,
        File,
        FileSlash,
        FileHost,
        PathStart,
        Path,
        CannotBeABaseUrlPath,
        Query,
        Fragment
    };

    static URL parse(Badge<URL>, const StringView& input, const URL* base_url = nullptr);

private:
    static Optional<URL> parse_data_url(const StringView& raw_input);
};

}

using AK::URLParser;
