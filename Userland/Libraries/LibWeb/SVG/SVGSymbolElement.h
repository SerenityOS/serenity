/*
 * Copyright (c) 2023, Preston Taylor <95388976+PrestonLTaylor@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGSymbolElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGSymbolElement, SVGGraphicsElement);

public:
    virtual ~SVGSymbolElement() override = default;

    void apply_presentational_hints(CSS::StyleProperties& style) const override;

private:
    SVGSymbolElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    bool is_direct_child_of_use_shadow_tree() const;
};

}
