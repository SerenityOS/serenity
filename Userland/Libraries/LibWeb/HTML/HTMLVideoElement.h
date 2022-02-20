/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

class HTMLVideoElement final : public HTMLMediaElement {
public:
    using WrapperType = Bindings::HTMLVideoElementWrapper;

    HTMLVideoElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLVideoElement() override;
};

}
