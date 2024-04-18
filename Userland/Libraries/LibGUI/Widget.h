/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/EnumBits.h>
#include <AK/JsonObject.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibCore/Timer.h>
#include <LibGUI/Event.h>
#include <LibGUI/FocusPolicy.h>
#include <LibGUI/Forward.h>
#include <LibGUI/GML/AST.h>
#include <LibGUI/Margins.h>
#include <LibGUI/Object.h>
#include <LibGUI/Property.h>
#include <LibGUI/UIDimensions.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>

namespace GUI::Registration {
extern GUI::ObjectClassRegistration registration_Widget;
}

#define REGISTER_WIDGET(namespace_, class_name)                                                                                                                                                   \
    namespace GUI::Registration {                                                                                                                                                                 \
    GUI::ObjectClassRegistration registration_##class_name(                                                                                                                                       \
        #namespace_ "::" #class_name##sv, []() -> ErrorOr<NonnullRefPtr<GUI::Object>> { return static_ptr_cast<GUI::Object>(TRY(namespace_::class_name::try_create())); }, &registration_Widget); \
    }

namespace GUI {

enum class HorizontalDirection {
    Left,
    Right
};

enum class VerticalDirection {
    Up,
    Down
};

constexpr VerticalDirection operator!(VerticalDirection const& other)
{
    if (other == VerticalDirection::Up)
        return VerticalDirection::Down;
    return VerticalDirection::Up;
}

constexpr VerticalDirection key_code_to_vertical_direction(KeyCode const& key)
{
    if (key == Key_Up)
        return VerticalDirection::Up;
    if (key == Key_Down)
        return VerticalDirection::Down;
    VERIFY_NOT_REACHED();
}

enum class AllowCallback {
    No,
    Yes
};

template<typename T>
ALWAYS_INLINE ErrorOr<void> initialize(T& object)
{
    if constexpr (requires { { object.initialize() } -> SameAs<ErrorOr<void>>; })
        return object.initialize();
    else
        return {};
}

class Widget : public GUI::Object {
    C_OBJECT(Widget)
public:
    virtual ~Widget() override;

    Layout* layout() { return m_layout.ptr(); }
    Layout const* layout() const { return m_layout.ptr(); }
    void set_layout(NonnullRefPtr<Layout>);

    template<class T, class... Args>
    inline void set_layout(Args&&... args)
    {
        auto layout = T::construct(forward<Args>(args)...);
        set_layout(*layout);
    }

    UISize min_size() const { return m_min_size; }
    void set_min_size(UISize const&);
    void set_min_size(UIDimension width, UIDimension height) { set_min_size({ width, height }); }

    UIDimension min_width() const { return m_min_size.width(); }
    UIDimension min_height() const { return m_min_size.height(); }
    void set_min_width(UIDimension width) { set_min_size(width, min_height()); }
    void set_min_height(UIDimension height) { set_min_size(min_width(), height); }

    UISize max_size() const { return m_max_size; }
    void set_max_size(UISize const&);
    void set_max_size(UIDimension width, UIDimension height) { set_max_size({ width, height }); }

    UIDimension max_width() const { return m_max_size.width(); }
    UIDimension max_height() const { return m_max_size.height(); }
    void set_max_width(UIDimension width) { set_max_size(width, max_height()); }
    void set_max_height(UIDimension height) { set_max_size(max_width(), height); }

    UISize preferred_size() const { return m_preferred_size; }
    void set_preferred_size(UISize const&);
    void set_preferred_size(UIDimension width, UIDimension height) { set_preferred_size({ width, height }); }

    UIDimension preferred_width() const { return m_preferred_size.width(); }
    UIDimension preferred_height() const { return m_preferred_size.height(); }
    void set_preferred_width(UIDimension width) { set_preferred_size(width, preferred_height()); }
    void set_preferred_height(UIDimension height) { set_preferred_size(preferred_width(), height); }

    virtual Optional<UISize> calculated_preferred_size() const;
    virtual Optional<UISize> calculated_min_size() const;

    UISize effective_preferred_size() const
    {
        auto effective_preferred_size = preferred_size();
        if (effective_preferred_size.either_is(SpecialDimension::Shrink))
            effective_preferred_size.replace_component_if_matching_with(SpecialDimension::Shrink, effective_min_size());
        if (effective_preferred_size.either_is(SpecialDimension::Fit) && calculated_preferred_size().has_value())
            effective_preferred_size.replace_component_if_matching_with(SpecialDimension::Fit, calculated_preferred_size().value());
        return effective_preferred_size;
    }

    UISize effective_min_size() const
    {
        auto effective_min_size = min_size();
        if (effective_min_size.either_is(SpecialDimension::Shrink) && calculated_min_size().has_value())
            effective_min_size.replace_component_if_matching_with(SpecialDimension::Shrink, calculated_min_size().value());
        return effective_min_size;
    }

    void set_fixed_size(UISize const& size)
    {
        VERIFY(size.has_only_int_values());
        set_min_size(size);
        set_max_size(size);
    }

    void set_fixed_size(UIDimension width, UIDimension height) { set_fixed_size({ width, height }); }

    void set_fixed_width(UIDimension width)
    {
        VERIFY(width.is_int());
        set_min_width(width);
        set_max_width(width);
    }

    void set_fixed_height(UIDimension height)
    {
        VERIFY(height.is_int());
        set_min_height(height);
        set_max_height(height);
    }

    virtual bool is_visible_for_timer_purposes() const override;

    bool has_tooltip() const { return !m_tooltip.is_empty(); }
    String tooltip() const { return m_tooltip; }
    void set_tooltip(String tooltip);

    bool is_auto_focusable() const { return m_auto_focusable; }
    void set_auto_focusable(bool auto_focusable) { m_auto_focusable = auto_focusable; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool updates_enabled() const { return m_updates_enabled; }
    void set_updates_enabled(bool);

    Gfx::IntRect relative_rect() const { return m_relative_rect; }
    Gfx::IntPoint relative_position() const { return m_relative_rect.location(); }

    Gfx::IntRect window_relative_rect() const;
    Gfx::IntRect screen_relative_rect() const;

    int x() const { return m_relative_rect.x(); }
    int y() const { return m_relative_rect.y(); }
    int width() const { return m_relative_rect.width(); }
    int height() const { return m_relative_rect.height(); }
    int length(Orientation orientation) const { return orientation == Orientation::Vertical ? height() : width(); }

    virtual Margins content_margins() const { return { 0 }; }

    Gfx::IntRect rect() const { return { 0, 0, width(), height() }; }
    Gfx::IntSize size() const { return m_relative_rect.size(); }
    Gfx::IntRect content_rect() const { return this->content_margins().applied_to(rect()); }
    Gfx::IntSize content_size() const { return this->content_rect().size(); }

    // Invalidate the widget (or an area thereof), causing a repaint to happen soon.
    void update();
    void update(Gfx::IntRect const&);

    // Repaint the widget (or an area thereof) immediately.
    void repaint();
    void repaint(Gfx::IntRect const&);

    bool is_focused() const;
    void set_focus(bool, FocusSource = FocusSource::Programmatic);

    bool focus_preempted() const { return m_focus_preempted; }
    void set_focus_preempted(bool b) { m_focus_preempted = b; }

    Function<void(bool const, FocusSource const)> on_focus_change;

    // Returns true if this widget or one of its descendants is focused.
    bool has_focus_within() const;

    Widget* focus_proxy() { return m_focus_proxy; }
    Widget const* focus_proxy() const { return m_focus_proxy; }
    void set_focus_proxy(Widget*);

    Vector<WeakPtr<Widget>>& focus_delegators() { return m_focus_delegators; }
    Vector<WeakPtr<Widget>> const& focus_delegators() const { return m_focus_delegators; }

    void set_focus_policy(FocusPolicy policy);
    FocusPolicy focus_policy() const;

    enum class ShouldRespectGreediness {
        No = 0,
        Yes
    };
    struct HitTestResult {
        WeakPtr<Widget> widget;
        Gfx::IntPoint local_position;
    };
    HitTestResult hit_test(Gfx::IntPoint, ShouldRespectGreediness = ShouldRespectGreediness::Yes);
    Widget* child_at(Gfx::IntPoint) const;

    void set_relative_rect(Gfx::IntRect const&);
    void set_relative_rect(int x, int y, int width, int height) { set_relative_rect({ x, y, width, height }); }

    void set_x(int x) { set_relative_rect(x, y(), width(), height()); }
    void set_y(int y) { set_relative_rect(x(), y, width(), height()); }
    void set_width(int width) { set_relative_rect(x(), y(), width, height()); }
    void set_height(int height) { set_relative_rect(x(), y(), width(), height); }

    void move_to(Gfx::IntPoint point) { set_relative_rect({ point, relative_rect().size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }
    void resize(Gfx::IntSize size) { set_relative_rect({ relative_rect().location(), size }); }
    void resize(int width, int height) { resize({ width, height }); }

    void move_by(int x, int y) { move_by({ x, y }); }
    void move_by(Gfx::IntPoint delta) { set_relative_rect({ relative_position().translated(delta), size() }); }

    Gfx::ColorRole background_role() const { return m_background_role; }
    void set_background_role(Gfx::ColorRole);

    Gfx::ColorRole foreground_role() const { return m_foreground_role; }
    void set_foreground_role(Gfx::ColorRole);

    void set_background_color(Gfx::Color);

    void set_autofill(bool b) { set_fill_with_background_color(b); }

    Window* window()
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    Window const* window() const
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    void set_window(Window*);

    Widget* parent_widget();
    Widget const* parent_widget() const;

    void set_fill_with_background_color(bool b) { m_fill_with_background_color = b; }
    bool fill_with_background_color() const { return m_fill_with_background_color; }

    Gfx::Font const& font() const { return *m_font; }

    void set_font(Gfx::Font const*);
    void set_font(Gfx::Font const& font) { set_font(&font); }

    void set_font_family(String const&);
    void set_font_size(unsigned);
    void set_font_weight(unsigned);
    void set_font_fixed_width(bool);
    bool is_font_fixed_width();

    void notify_layout_changed(Badge<Layout>);
    void invalidate_layout();

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool spans_entire_window_horizontally() const;

    bool is_greedy_for_hits() const { return m_greedy_for_hits; }
    void set_greedy_for_hits(bool b) { m_greedy_for_hits = b; }

    void move_to_front();
    void move_to_back();

    bool is_frontmost() const;
    bool is_backmost() const;

    Action* action_for_shortcut(Shortcut const&);

    template<typename Callback>
    void for_each_child_widget(Callback callback)
    {
        for_each_child([&](auto& child) {
            if (is<Widget>(child))
                return callback(verify_cast<Widget>(child));
            return IterationDecision::Continue;
        });
    }

    Vector<Widget&> child_widgets() const;

    void do_layout();

    Gfx::Palette palette() const;
    void set_palette(Gfx::Palette&);

    String title() const;
    void set_title(String);

    Margins const& grabbable_margins() const { return m_grabbable_margins; }
    void set_grabbable_margins(Margins const&);

    Gfx::IntRect relative_non_grabbable_rect() const;

    Function<void(StringView)> on_emoji_input;

    void set_accepts_command_palette(bool b) { m_accepts_command_palette = b; }
    bool accepts_command_palette() const { return m_accepts_command_palette; }

    virtual Gfx::IntRect children_clip_rect() const;

    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> const& override_cursor() const { return m_override_cursor; }
    void set_override_cursor(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>>);

    using UnregisteredChildHandler = ErrorOr<NonnullRefPtr<Core::EventReceiver>>(StringView);
    ErrorOr<void> load_from_gml(StringView);
    ErrorOr<void> load_from_gml(StringView, UnregisteredChildHandler);

    // FIXME: remove this when all uses of shrink_to_fit are eliminated
    void set_shrink_to_fit(bool);
    bool is_shrink_to_fit() const { return preferred_width().is_shrink() || preferred_height().is_shrink(); }

    bool has_pending_drop() const;

    // In order for others to be able to call this, it needs to be public.
    virtual ErrorOr<void> load_from_gml_ast(NonnullRefPtr<GUI::GML::Node const> ast, UnregisteredChildHandler);

    void add_spacer();

protected:
    Widget();

    virtual void event(Core::Event&) override;

    // This is called after children have been painted.
    virtual void second_paint_event(PaintEvent&);

    virtual void layout_relevant_change_occurred();
    virtual void custom_layout() { }
    virtual void did_change_font() { }
    virtual void did_layout() { }
    virtual void paint_event(PaintEvent&);
    virtual void resize_event(ResizeEvent&);
    virtual void show_event(ShowEvent&);
    virtual void hide_event(HideEvent&);
    virtual void keydown_event(KeyEvent&);
    virtual void keyup_event(KeyEvent&);
    virtual void mousemove_event(MouseEvent&);
    virtual void mousedown_event(MouseEvent&);
    virtual void mouseup_event(MouseEvent&);
    virtual void mousewheel_event(MouseEvent&);
    virtual void doubleclick_event(MouseEvent&);
    virtual void context_menu_event(ContextMenuEvent&);
    virtual void focusin_event(FocusEvent&);
    virtual void focusout_event(FocusEvent&);
    virtual void enter_event(Core::Event&);
    virtual void leave_event(Core::Event&);
    virtual void child_event(Core::ChildEvent&) override;
    virtual void change_event(Event&);
    virtual void drag_enter_event(DragEvent&);
    virtual void drag_move_event(DragEvent&);
    virtual void drag_leave_event(Event&);
    virtual void drop_event(DropEvent&);
    virtual void theme_change_event(ThemeChangeEvent&);
    virtual void fonts_change_event(FontsChangeEvent&);
    virtual void screen_rects_change_event(ScreenRectsChangeEvent&);
    virtual void applet_area_rect_change_event(AppletAreaRectChangeEvent&);

    void show_or_hide_tooltip();

    void add_focus_delegator(Widget*);
    void remove_focus_delegator(Widget*);

private:
    virtual bool is_widget() const final { return true; }

    void handle_paint_event(PaintEvent&);
    void handle_resize_event(ResizeEvent&);
    void handle_mousedown_event(MouseEvent&);
    void handle_mousedoubleclick_event(MouseEvent&);
    void handle_mouseup_event(MouseEvent&);
    void handle_keydown_event(KeyEvent&);
    void handle_enter_event(Core::Event&);
    void handle_leave_event(Core::Event&);
    void focus_previous_widget(FocusSource, bool siblings_only);
    void focus_next_widget(FocusSource, bool siblings_only);

    // HACK: These are used as property getters for the fixed_* size property aliases.
    int dummy_fixed_width() { return 0; }
    int dummy_fixed_height() { return 0; }
    Gfx::IntSize dummy_fixed_size() { return {}; }

    // HACK: Used as property getter for the font_family property, can be removed when Font is migrated from ByteString.
    String font_family() const;

    Window* m_window { nullptr };
    RefPtr<Layout> m_layout;

    Gfx::IntRect m_relative_rect;
    Gfx::ColorRole m_background_role;
    Gfx::ColorRole m_foreground_role;
    NonnullRefPtr<Gfx::Font const> m_font;
    String m_tooltip;

    UISize m_min_size { SpecialDimension::Shrink };
    UISize m_max_size { SpecialDimension::Grow };
    UISize m_preferred_size { SpecialDimension::Grow };
    Margins m_grabbable_margins;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
    bool m_greedy_for_hits { false };
    bool m_auto_focusable { true };
    bool m_focus_preempted { false };
    bool m_enabled { true };
    bool m_updates_enabled { true };
    bool m_accepts_command_palette { true };
    bool m_default_font { true };

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;
    String m_title;

    WeakPtr<Widget> m_focus_proxy;
    Vector<WeakPtr<Widget>> m_focus_delegators;
    FocusPolicy m_focus_policy { FocusPolicy::NoFocus };

    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> m_override_cursor { Gfx::StandardCursor::None };
};

inline Widget* Widget::parent_widget()
{
    if (parent() && is<Widget>(*parent()))
        return &verify_cast<Widget>(*parent());
    return nullptr;
}
inline Widget const* Widget::parent_widget() const
{
    if (parent() && is<Widget>(*parent()))
        return &verify_cast<Widget const>(*parent());
    return nullptr;
}
}

template<>
inline bool Core::EventReceiver::fast_is<GUI::Widget>() const { return is_widget(); }

template<>
struct AK::Formatter<GUI::Widget> : AK::Formatter<Core::EventReceiver> {
};
