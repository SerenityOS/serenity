/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLBlinkElement final : public HTMLElement {
public:
    HTMLBlinkElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLBlinkElement() override;

private:
    void blink();

    NonnullRefPtr<Core::Timer> m_timer;
};

}
