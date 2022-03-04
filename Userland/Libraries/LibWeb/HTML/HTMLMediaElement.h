/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMediaElement : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLMediaElementWrapper;

    HTMLMediaElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLMediaElement() override;

    Bindings::CanPlayTypeResult can_play_type(String const& type) const;
};

}
