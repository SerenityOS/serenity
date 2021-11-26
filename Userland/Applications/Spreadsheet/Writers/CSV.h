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
    CSV(OutputStream& output, const ContainerType& data, const Vector<StringView>& headers = {}, WriterBehavior behaviors = default_behaviors())
        : XSV<ContainerType>(output, data, { ",", "\"", WriterTraits::Repeat }, headers, behaviors)
    {
    }
};

}
