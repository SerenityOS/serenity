/*
 * Copyright (c) 2022, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/ARIARoleNames.h>

namespace Web::DOM::ARIARoleNames {

#define __ENUMERATE_ARIA_ROLE(name) DeprecatedFlyString name;
ENUMERATE_ARIA_ROLES
#undef __ENUMERATE_ARIA_ROLE

[[gnu::constructor]] static void initialize()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

#define __ENUMERATE_ARIA_ROLE(name) name = #name;
    ENUMERATE_ARIA_ROLES
#undef __ENUMERATE_ARIA_ROLE

    // Special case for C++ keyword
    switch_ = "switch";
    s_initialized = true;
}

// https://www.w3.org/TR/wai-aria-1.2/#abstract_roles
bool is_abstract_aria_role(DeprecatedFlyString const& role)
{
    return role.is_one_of(
        ARIARoleNames::command,
        ARIARoleNames::composite,
        ARIARoleNames::input,
        ARIARoleNames::landmark,
        ARIARoleNames::range,
        ARIARoleNames::roletype,
        ARIARoleNames::section,
        ARIARoleNames::sectionhead,
        ARIARoleNames::select,
        ARIARoleNames::structure,
        ARIARoleNames::widget,
        ARIARoleNames::window);
}

// https://www.w3.org/TR/wai-aria-1.2/#widget_roles
bool is_widget_aria_role(DeprecatedFlyString const& role)
{
    return role.to_lowercase().is_one_of(
        ARIARoleNames::button,
        ARIARoleNames::checkbox,
        ARIARoleNames::gridcell,
        ARIARoleNames::link,
        ARIARoleNames::menuitem,
        ARIARoleNames::menuitemcheckbox,
        ARIARoleNames::option,
        ARIARoleNames::progressbar,
        ARIARoleNames::radio,
        ARIARoleNames::scrollbar,
        ARIARoleNames::searchbox,
        ARIARoleNames::separator, // TODO: Only when focusable
        ARIARoleNames::slider,
        ARIARoleNames::spinbutton,
        ARIARoleNames::switch_,
        ARIARoleNames::tab,
        ARIARoleNames::tabpanel,
        ARIARoleNames::textbox,
        ARIARoleNames::treeitem,
        ARIARoleNames::combobox,
        ARIARoleNames::grid,
        ARIARoleNames::listbox,
        ARIARoleNames::menu,
        ARIARoleNames::menubar,
        ARIARoleNames::radiogroup,
        ARIARoleNames::tablist,
        ARIARoleNames::tree,
        ARIARoleNames::treegrid);
}

// https://www.w3.org/TR/wai-aria-1.2/#document_structure_roles
bool is_document_structure_aria_role(DeprecatedFlyString const& role)
{
    return role.to_lowercase().is_one_of(
        ARIARoleNames::application,
        ARIARoleNames::article,
        ARIARoleNames::blockquote,
        ARIARoleNames::caption,
        ARIARoleNames::cell,
        ARIARoleNames::columnheader,
        ARIARoleNames::definition,
        ARIARoleNames::deletion,
        ARIARoleNames::directory,
        ARIARoleNames::document,
        ARIARoleNames::emphasis,
        ARIARoleNames::feed,
        ARIARoleNames::figure,
        ARIARoleNames::generic,
        ARIARoleNames::group,
        ARIARoleNames::heading,
        ARIARoleNames::img,
        ARIARoleNames::insertion,
        ARIARoleNames::list,
        ARIARoleNames::listitem,
        ARIARoleNames::math,
        ARIARoleNames::meter,
        ARIARoleNames::none,
        ARIARoleNames::note,
        ARIARoleNames::paragraph,
        ARIARoleNames::presentation,
        ARIARoleNames::row,
        ARIARoleNames::rowgroup,
        ARIARoleNames::rowheader,
        ARIARoleNames::separator, // TODO: Only when not focusable
        ARIARoleNames::strong,
        ARIARoleNames::subscript,
        ARIARoleNames::table,
        ARIARoleNames::term,
        ARIARoleNames::time,
        ARIARoleNames::toolbar,
        ARIARoleNames::tooltip);
}

// https://www.w3.org/TR/wai-aria-1.2/#landmark_roles
bool is_landmark_aria_role(DeprecatedFlyString const& role)
{
    return role.to_lowercase().is_one_of(
        ARIARoleNames::banner,
        ARIARoleNames::complementary,
        ARIARoleNames::contentinfo,
        ARIARoleNames::form,
        ARIARoleNames::main,
        ARIARoleNames::navigation,
        ARIARoleNames::region,
        ARIARoleNames::search);
}

// https://www.w3.org/TR/wai-aria-1.2/#live_region_roles
bool is_live_region_aria_role(DeprecatedFlyString const& role)
{
    return role.to_lowercase().is_one_of(
        ARIARoleNames::alert,
        ARIARoleNames::log,
        ARIARoleNames::marquee,
        ARIARoleNames::status,
        ARIARoleNames::timer);
}

// https://www.w3.org/TR/wai-aria-1.2/#window_roles
bool is_windows_aria_role(DeprecatedFlyString const& role)
{
    return role.to_lowercase().is_one_of(
        ARIARoleNames::alertdialog,
        ARIARoleNames::dialog);
}

bool is_non_abstract_aria_role(DeprecatedFlyString const& role)
{
    return is_widget_aria_role(role)
        || is_document_structure_aria_role(role)
        || is_landmark_aria_role(role)
        || is_live_region_aria_role(role)
        || is_windows_aria_role(role);
}

}
