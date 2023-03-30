/*
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LibGfx/Filters/GenericConvolutionFilter.h"
#include <Applications/PixelPaint/Filters/Filter.h>

namespace PixelPaint::Filters {

class ConvolutionFilter : public Filter {
public:
    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    ConvolutionFilter(ImageEditor* editor)
        : Filter(editor) {};

protected:
    Gfx::ConvolutionFilterOptions m_filter_options = Gfx::ConvolutionFilterOptions { true };
    inline Gfx::ConvolutionFilterOptions get_filter_options() const { return m_filter_options; };
};

}
