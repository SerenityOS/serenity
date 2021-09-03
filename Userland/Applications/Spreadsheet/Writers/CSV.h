/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "XSV.h"
#include <YAK/Forward.h>
#include <YAK/StringView.h>

namespace Writer {

template<typename ContainerType>
class CSV : public XSV<ContainerType> {
public:
    CSV(OutputStream& output, const ContainerType& data, const Vector<StringView>& headers = {}, WriterBehaviour behaviours = default_behaviours())
        : XSV<ContainerType>(output, data, { ",", "\"", WriterTraits::Repeat }, headers, behaviours)
    {
    }
};

}
