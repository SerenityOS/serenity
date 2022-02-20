/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTrackElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTrackElementWrapper;

    HTMLTrackElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLTrackElement() override;
};

}
