#pragma once

#include <AK/Badge.h>
#include <LibDraw/SystemTheme.h>

class GApplication;

class Palette : public RefCounted<Palette> {
public:
    static NonnullRefPtr<Palette> create_with_shared_buffer(SharedBuffer&);
    ~Palette();

    Color window() const { return color(ColorRole::Window); }
    Color window_text() const { return color(ColorRole::WindowText); }
    Color selection() const { return color(ColorRole::Selection); }
    Color selection_text() const { return color(ColorRole::SelectionText); }
    Color desktop_background() const { return color(ColorRole::DesktopBackground); }
    Color active_window_border1() const { return color(ColorRole::ActiveWindowBorder1); }
    Color active_window_border2() const { return color(ColorRole::ActiveWindowBorder2); }
    Color active_window_title() const { return color(ColorRole::ActiveWindowTitle); }
    Color inactive_window_border1() const { return color(ColorRole::InactiveWindowBorder1); }
    Color inactive_window_border2() const { return color(ColorRole::InactiveWindowBorder2); }
    Color inactive_window_title() const { return color(ColorRole::InactiveWindowTitle); }
    Color moving_window_border1() const { return color(ColorRole::MovingWindowBorder1); }
    Color moving_window_border2() const { return color(ColorRole::MovingWindowBorder2); }
    Color moving_window_title() const { return color(ColorRole::MovingWindowTitle); }
    Color highlight_window_border1() const { return color(ColorRole::HighlightWindowBorder1); }
    Color highlight_window_border2() const { return color(ColorRole::HighlightWindowBorder2); }
    Color highlight_window_title() const { return color(ColorRole::HighlightWindowTitle); }
    Color menu_stripe() const { return color(ColorRole::MenuStripe); }
    Color menu_base() const { return color(ColorRole::MenuBase); }
    Color menu_selection() const { return color(ColorRole::MenuSelection); }
    Color base() const { return color(ColorRole::Base); }
    Color base_text() const { return color(ColorRole::BaseText); }
    Color button() const { return color(ColorRole::Button); }
    Color button_text() const { return color(ColorRole::ButtonText); }
    Color threed_highlight() const { return color(ColorRole::ThreedHighlight); }
    Color threed_shadow1() const { return color(ColorRole::ThreedShadow1); }
    Color threed_shadow2() const { return color(ColorRole::ThreedShadow2); }
    Color hover_highlight() const { return color(ColorRole::ThreedHighlight); }

    Color color(ColorRole) const;

    const SystemTheme& theme() const;

    void replace_internal_buffer(Badge<GApplication>, SharedBuffer& buffer) { m_theme_buffer = buffer; }

private:
    explicit Palette(SharedBuffer&);

    RefPtr<SharedBuffer> m_theme_buffer;
};
