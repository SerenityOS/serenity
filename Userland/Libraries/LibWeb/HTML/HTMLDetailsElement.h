/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDetailsElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDetailsElement, HTMLElement);

public:
    virtual ~HTMLDetailsElement() override;

    // https://www.w3.org/TR/html-aria/#el-details
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::group; }

    // ^Element
    WebIDL::ExceptionOr<void> set_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;
    void remove_attribute(DeprecatedFlyString const& name) override;

    void run_details_notification_task_steps();

private:
    HTMLDetailsElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
