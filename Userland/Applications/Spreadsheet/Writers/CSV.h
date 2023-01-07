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

template<typename ContainerType>
class CSV : public XSV<ContainerType> {
public:
    CSV(Core::Stream::Handle<Core::Stream::Stream> output, ContainerType const& data, Vector<StringView> headers = {}, WriterBehavior behaviors = default_behaviors())
        : XSV<ContainerType>(move(output), data, { ",", "\"", WriterTraits::Repeat }, move(headers), behaviors)
    {
    }
};

}
