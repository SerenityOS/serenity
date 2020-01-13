#pragma once

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <LibDraw/SystemTheme.h>

class GApplication;

class PaletteImpl : public RefCounted<PaletteImpl> {
    AK_MAKE_NONCOPYABLE(PaletteImpl)
    AK_MAKE_NONMOVABLE(PaletteImpl)
public:
    static NonnullRefPtr<PaletteImpl> create_with_shared_buffer(SharedBuffer&);
    NonnullRefPtr<PaletteImpl> clone() const;

    Color color(ColorRole) const;
    const SystemTheme& theme() const;

    void replace_internal_buffer(Badge<GApplication>, SharedBuffer& buffer) { m_theme_buffer = buffer; }

private:
    explicit PaletteImpl(SharedBuffer&);

    RefPtr<SharedBuffer> m_theme_buffer;
};

class Palette {

public:
    explicit Palette(const PaletteImpl&);
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
    Color menu_base_text() const { return color(ColorRole::MenuBaseText); }
    Color menu_selection() const { return color(ColorRole::MenuSelection); }
    Color menu_selection_text() const { return color(ColorRole::MenuSelectionText); }
    Color base() const { return color(ColorRole::Base); }
    Color base_text() const { return color(ColorRole::BaseText); }
    Color button() const { return color(ColorRole::Button); }
    Color button_text() const { return color(ColorRole::ButtonText); }
    Color threed_highlight() const { return color(ColorRole::ThreedHighlight); }
    Color threed_shadow1() const { return color(ColorRole::ThreedShadow1); }
    Color threed_shadow2() const { return color(ColorRole::ThreedShadow2); }
    Color hover_highlight() const { return color(ColorRole::ThreedHighlight); }
    Color rubber_band_fill() const { return color(ColorRole::RubberBandFill); }
    Color rubber_band_border() const { return color(ColorRole::RubberBandBorder); }

    Color link() const { return color(ColorRole::Link); }
    Color active_link() const { return color(ColorRole::ActiveLink); }
    Color visited_link() const { return color(ColorRole::VisitedLink); }

    Color color(ColorRole role) const { return m_impl->color(role); }

    void set_color(ColorRole, Color);

    const SystemTheme& theme() const { return m_impl->theme(); }

    PaletteImpl& impl() { return *m_impl; }
    const PaletteImpl& impl() const { return *m_impl; }

private:
    NonnullRefPtr<PaletteImpl> m_impl;
};
