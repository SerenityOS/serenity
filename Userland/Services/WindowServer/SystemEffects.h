/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace WindowServer {

enum class ShowGeometry : u8 {
    OnMoveAndResize,
    OnMoveOnly,
    OnResizeOnly,
    Never
};

enum class Effects : size_t {
    AnimateMenus,
    FlashMenus,
    AnimateWindows,
    SmoothScrolling,
    TabAccents,
    SplitterKnurls,
    Tooltips,
    MenuShadow,
    WindowShadow,
    TooltipShadow,
    __Count
};

namespace ShowGeometryTools {

[[maybe_unused]] static StringView enum_to_string(ShowGeometry geometry)
{
    switch (geometry) {
    case ShowGeometry::OnMoveAndResize:
        return "OnMoveAndResize"sv;
    case ShowGeometry::OnMoveOnly:
        return "OnMoveOnly"sv;
    case ShowGeometry::OnResizeOnly:
        return "OnResizeOnly"sv;
    case ShowGeometry::Never:
        return "Never"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

[[maybe_unused]] static ShowGeometry string_to_enum(StringView geometry)
{
    if (geometry == "OnMoveAndResize"sv)
        return ShowGeometry::OnMoveAndResize;
    else if (geometry == "OnMoveOnly"sv)
        return ShowGeometry::OnMoveOnly;
    else if (geometry == "OnResizeOnly"sv)
        return ShowGeometry::OnResizeOnly;
    else if (geometry == "Never"sv)
        return ShowGeometry::Never;
    VERIFY_NOT_REACHED();
}

};

class SystemEffects {
public:
    SystemEffects() = default;
    SystemEffects(Vector<bool> effects, ShowGeometry show)
        : m_effects(effects)
        , m_geometry(show)
    {
    }
    SystemEffects(Vector<bool> effects)
        : m_effects(effects)
    {
    }
    ~SystemEffects() = default;

    Vector<bool>& effects() { return m_effects; }
    bool set_effect(Effects effect, bool value)
    {
        VERIFY(effect < Effects::__Count);
        auto& effect_value = m_effects[to_underlying(effect)];
        if (effect_value == value)
            return false;
        effect_value = value;
        return true;
    }

    bool animate_menus() const { return m_effects[to_underlying(Effects::AnimateMenus)]; }
    bool flash_menus() const { return m_effects[to_underlying(Effects::FlashMenus)]; }
    bool animate_windows() const { return m_effects[to_underlying(Effects::AnimateWindows)]; }
    bool smooth_scrolling() const { return m_effects[to_underlying(Effects::SmoothScrolling)]; }

    bool tab_accents() const { return m_effects[to_underlying(Effects::TabAccents)]; }
    bool splitter_knurls() const { return m_effects[to_underlying(Effects::SplitterKnurls)]; }
    bool tooltips() const { return m_effects[to_underlying(Effects::Tooltips)]; }

    bool menu_shadow() const { return m_effects[to_underlying(Effects::MenuShadow)]; }
    bool window_shadow() const { return m_effects[to_underlying(Effects::WindowShadow)]; }
    bool tooltip_shadow() const { return m_effects[to_underlying(Effects::TooltipShadow)]; }

    void set_geometry(ShowGeometry g) { m_geometry = g; }
    ShowGeometry geometry() const { return m_geometry; }

    bool operator==(SystemEffects const& other) const
    {
        return m_effects == other.m_effects && m_geometry == other.m_geometry;
    }

private:
    Vector<bool> m_effects;
    ShowGeometry m_geometry { ShowGeometry::Never };
};

}
