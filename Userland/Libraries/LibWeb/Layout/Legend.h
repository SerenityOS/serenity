/*
 * Copyright (c) 2021 Tobias Christiansen <tobi@tobyase.de>
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/Layout/BlockBox.h>

namespace Web::Layout {

class Legend : public BlockBox {
public:
    Legend(DOM::Document&, HTML::HTMLLegendElement*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Legend() override;
};

}
