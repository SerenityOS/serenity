/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTrackElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTrackElement, HTMLElement);

public:
    virtual ~HTMLTrackElement() override;

private:
    HTMLTrackElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
