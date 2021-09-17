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
    explicit SVGFormattingContext(Box&, FormattingContext* parent);
    ~SVGFormattingContext();

    virtual void run(Box&, LayoutMode) override;
};

}
