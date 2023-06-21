/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "XSV.h"
#include <AK/Forward.h>
#include <AK/StringView.h>

namespace Writer {

class CSV {
public:
    template<typename ContainerType>
    static ErrorOr<void> generate(Stream& output, ContainerType const& data, Vector<StringView> headers = {}, WriterBehavior behaviors = default_behaviors())
    {
        return XSV<ContainerType>::generate(output, data, { ",", "\"", WriterTraits::Repeat }, move(headers), behaviors);
    }

    template<typename ContainerType>
    static ErrorOr<void> generate_preview(Stream& output, ContainerType const& data, Vector<StringView> headers = {}, WriterBehavior behaviors = default_behaviors())
    {
        return XSV<ContainerType>::generate_preview(output, data, { ",", "\"", WriterTraits::Repeat }, move(headers), behaviors);
    }
};

}
