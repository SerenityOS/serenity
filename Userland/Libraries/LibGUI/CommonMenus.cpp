/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/ColorFilterer.h>
#include <LibGUI/Menu.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>

namespace GUI {

namespace CommonMenus {

NonnullRefPtr<Menu> make_accessibility_menu(ColorFilterer& filterer)
{
    auto default_accessibility_action = Action::create_checkable("Unimpaired", { Mod_AltGr, Key_1 }, [&](auto&) {
        filterer.set_color_filter(nullptr);
    });
    auto pratanopia_accessibility_action = Action::create_checkable("Protanopia", { Mod_AltGr, Key_2 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_protanopia());
    });
    auto pratanomaly_accessibility_action = Action::create_checkable("Protanomaly", { Mod_AltGr, Key_3 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_protanomaly());
    });
    auto tritanopia_accessibility_action = Action::create_checkable("Tritanopia", { Mod_AltGr, Key_4 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_tritanopia());
    });
    auto tritanomaly_accessibility_action = Action::create_checkable("Tritanomaly", { Mod_AltGr, Key_5 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_tritanomaly());
    });
    auto deuteranopia_accessibility_action = Action::create_checkable("Deuteranopia", { Mod_AltGr, Key_6 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranopia());
    });
    auto deuteranomaly_accessibility_action = Action::create_checkable("Deuteranomaly", { Mod_AltGr, Key_7 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranomaly());
    });
    auto achromatopsia_accessibility_action = Action::create_checkable("Achromatopsia", { Mod_AltGr, Key_8 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_achromatopsia());
    });
    auto achromatomaly_accessibility_action = Action::create_checkable("Achromatomaly", { Mod_AltGr, Key_9 }, [&](auto&) {
        filterer.set_color_filter(Gfx::ColorBlindnessFilter::create_achromatomaly());
    });

    default_accessibility_action->set_checked(true);

    auto group = make<ActionGroup>();
    group->set_exclusive(true);
    group->add_action(*default_accessibility_action);
    group->add_action(*pratanopia_accessibility_action);
    group->add_action(*pratanomaly_accessibility_action);
    group->add_action(*tritanopia_accessibility_action);
    group->add_action(*tritanomaly_accessibility_action);
    group->add_action(*deuteranopia_accessibility_action);
    group->add_action(*deuteranomaly_accessibility_action);
    group->add_action(*achromatopsia_accessibility_action);
    group->add_action(*achromatomaly_accessibility_action);
    (void)group.leak_ptr();

    auto menu = Menu::construct("&Accessibility"_string);
    menu->add_action(default_accessibility_action);
    menu->add_action(pratanopia_accessibility_action);
    menu->add_action(pratanomaly_accessibility_action);
    menu->add_action(tritanopia_accessibility_action);
    menu->add_action(tritanomaly_accessibility_action);
    menu->add_action(deuteranopia_accessibility_action);
    menu->add_action(deuteranomaly_accessibility_action);
    menu->add_action(achromatopsia_accessibility_action);
    menu->add_action(achromatomaly_accessibility_action);

    return menu;
}

}

}
