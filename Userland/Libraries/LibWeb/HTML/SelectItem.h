/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibIPC/Forward.h>

namespace Web::HTML {

struct SelectItem {
    enum class Type {
        OptionGroup,
        Option,
        Separator,
    };

    Type type;
    Optional<String> label = {};
    Optional<String> value = {};
    Optional<Vector<SelectItem>> items = {};
    bool selected = false;
};

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, Web::HTML::SelectItem const&);

template<>
ErrorOr<Web::HTML::SelectItem> decode(Decoder&);

}
