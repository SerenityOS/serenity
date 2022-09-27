/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class SVGFormattingContext : public FormattingContext {
public:
    explicit SVGFormattingContext(LayoutState&, Box const&, FormattingContext* parent);
    ~SVGFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual float automatic_content_height() const override;
};

}
