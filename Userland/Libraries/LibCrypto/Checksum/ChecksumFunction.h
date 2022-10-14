/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>

namespace Crypto::Checksum {

template<typename ChecksumType>
class ChecksumFunction {
public:
    virtual void update(ReadonlyBytes data) = 0;
    virtual ChecksumType digest() = 0;

protected:
    virtual ~ChecksumFunction() = default;
};

}
