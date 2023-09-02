/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDialogElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDialogElement, HTMLElement);

public:
    virtual ~HTMLDialogElement() override;

    String return_value() const;
    void set_return_value(String);

    void show();
    void show_modal();
    void close(Optional<String> return_value);

    // https://www.w3.org/TR/html-aria/#el-dialog
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::dialog; }

private:
    HTMLDialogElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    String m_return_value;
};

}
