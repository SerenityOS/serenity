/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class Filter {
public:
    class Parameters {
    public:
        virtual bool is_generic_convolution_filter() const { return false; }

        virtual ~Parameters() = default;
    };
    virtual ~Filter() = default;

    virtual StringView class_name() const = 0;

    virtual void apply(Bitmap&, IntRect const&, Bitmap const&, IntRect const&, Parameters const&) {};
    virtual void apply(Bitmap&, IntRect const&, Bitmap const&, IntRect const&) {};

protected:
    Filter() = default;
};

}
