/*
 * Copyright (c) 2022, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

class ARIAMixin {

public:
    virtual ~ARIAMixin() = default;

    virtual DeprecatedString role() const = 0;
    virtual WebIDL::ExceptionOr<void> set_role(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_active_descendant() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_active_descendant(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_atomic() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_atomic(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_auto_complete() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_auto_complete(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_busy() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_busy(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_checked() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_checked(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_col_count() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_col_count(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_col_index() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_col_index(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_col_span() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_col_span(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_controls() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_controls(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_current() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_current(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_described_by() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_described_by(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_details() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_details(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_drop_effect() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_drop_effect(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_error_message() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_error_message(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_disabled() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_disabled(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_expanded() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_expanded(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_flow_to() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_flow_to(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_grabbed() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_grabbed(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_has_popup() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_has_popup(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_hidden() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_hidden(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_invalid() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_invalid(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_key_shortcuts() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_key_shortcuts(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_label() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_label(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_labelled_by() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_labelled_by(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_level() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_level(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_live() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_live(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_modal() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_modal(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_multi_line() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_multi_line(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_multi_selectable() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_multi_selectable(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_orientation() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_orientation(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_owns() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_owns(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_placeholder() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_placeholder(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_pos_in_set() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_pos_in_set(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_pressed() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_pressed(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_read_only() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_read_only(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_relevant() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_relevant(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_required() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_required(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_role_description() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_role_description(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_row_count() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_row_count(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_row_index() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_row_index(DeprecatedString const&) = 0;

    virtual DeprecatedString aria_row_span() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_row_span(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_selected() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_selected(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_set_size() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_set_size(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_sort() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_sort(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_value_max() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_value_max(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_value_min() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_value_min(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_value_now() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_value_now(DeprecatedString const&) = 0;
    virtual DeprecatedString aria_value_text() const = 0;
    virtual WebIDL::ExceptionOr<void> set_aria_value_text(DeprecatedString const&) = 0;

    // https://www.w3.org/TR/html-aria/#docconformance
    virtual FlyString default_role() const { return {}; };

    FlyString role_or_default() const;

    bool has_global_aria_attribute() const;

protected:
    ARIAMixin() = default;
};

}
