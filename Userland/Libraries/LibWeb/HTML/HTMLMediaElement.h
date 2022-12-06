/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMediaElement : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMediaElement, HTMLElement);

public:
    virtual ~HTMLMediaElement() override;

    Bindings::CanPlayTypeResult can_play_type(DeprecatedString const& type) const;

    void load() const;

protected:
    HTMLMediaElement(DOM::Document&, DOM::QualifiedName);
};

}
