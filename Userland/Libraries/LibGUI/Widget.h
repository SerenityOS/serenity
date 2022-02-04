/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/JsonObject.h>
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibCore/Object.h>
#include <LibGUI/Event.h>
#include <LibGUI/FocusPolicy.h>
#include <LibGUI/Forward.h>
#include <LibGUI/GML/AST.h>
#include <LibGUI/Margins.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>

namespace Core {
namespace Registration {
extern Core::ObjectClassRegistration registration_Widget;
}
}

#define REGISTER_WIDGET(namespace_, class_name)                                                                                                   \
    namespace Core {                                                                                                                              \
    namespace Registration {                                                                                                                      \
    Core::ObjectClassRegistration registration_##class_name(                                                                                      \
        #namespace_ "::" #class_name, []() { return static_ptr_cast<Core::Object>(namespace_::class_name::construct()); }, &registration_Widget); \
    }                                                                                                                                             \
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

enum class AllowCallback {
    No,
    Yes
};

class Widget : public Core::Object {
    C_OBJECT(Widget)
public:
    virtual ~Widget() override;

    Layout* layout() { return m_layout.ptr(); }
    const Layout* layout() const { return m_layout.ptr(); }
    void set_layout(NonnullRefPtr<Layout>);

    template<class T, class... Args>
    ErrorOr<NonnullRefPtr<T>> try_set_layout(Args&&... args)
    {
        auto layout = TRY(T::try_create(forward<Args>(args)...));
        set_layout(*layout);
        return layout;
    }

    template<class T, class... Args>
    inline T& set_layout(Args&&... args)
    {
        auto layout = T::construct(forward<Args>(args)...);
        set_layout(*layout);
        return layout;
    }

    Gfx::IntSize min_size() const { return m_min_size; }
    void set_min_size(const Gfx::IntSize&);
    void set_min_size(int width, int height) { set_min_size({ width, height }); }

    int min_width() const { return m_min_size.width(); }
    int min_height() const { return m_min_size.height(); }
    void set_min_width(int width) { set_min_size(width, min_height()); }
    void set_min_height(int height) { set_min_size(min_width(), height); }

    Gfx::IntSize max_size() const { return m_max_size; }
    void set_max_size(const Gfx::IntSize&);
    void set_max_size(int width, int height) { set_max_size({ width, height }); }

    int max_width() const { return m_max_size.width(); }
    int max_height() const { return m_max_size.height(); }
    void set_max_width(int width) { set_max_size(width, max_height()); }
    void set_max_height(int height) { set_max_size(max_width(), height); }

    void set_fixed_size(const Gfx::IntSize& size)
    {
        set_min_size(size);
        set_max_size(size);
    }

    void set_fixed_size(int width, int height) { set_fixed_size({ width, height }); }

    void set_fixed_width(int width)
    {
        set_min_width(width);
        set_max_width(width);
    }

    void set_fixed_height(int height)
    {
        set_min_height(height);
        set_max_height(height);
    }

    virtual bool is_visible_for_timer_purposes() const override;

    bool has_tooltip() const { return !m_tooltip.is_empty(); }
    String tooltip() const { return m_tooltip; }
    void set_tooltip(String);

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
    Gfx::IntRect content_rect() const { return this->content_margins().applied_to(rect()); };
    Gfx::IntSize content_size() const { return this->content_rect().size(); };

    // Invalidate the widget (or an area thereof), causing a repaint to happen soon.
    void update();
    void update(const Gfx::IntRect&);

    // Repaint the widget (or an area thereof) immediately.
    void repaint();
    void repaint(Gfx::IntRect const&);

    bool is_focused() const;
    void set_focus(bool, FocusSource = FocusSource::Programmatic);

    Function<void(const bool, const FocusSource)> on_focus_change;

    // Returns true if this widget or one of its descendants is focused.
    bool has_focus_within() const;

    Widget* focus_proxy() { return m_focus_proxy; }
    const Widget* focus_proxy() const { return m_focus_proxy; }
    void set_focus_proxy(Widget*);

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
    HitTestResult hit_test(const Gfx::IntPoint&, ShouldRespectGreediness = ShouldRespectGreediness::Yes);
    Widget* child_at(const Gfx::IntPoint&) const;

    void set_relative_rect(const Gfx::IntRect&);
    void set_relative_rect(int x, int y, int width, int height) { set_relative_rect({ x, y, width, height }); }

    void set_x(int x) { set_relative_rect(x, y(), width(), height()); }
    void set_y(int y) { set_relative_rect(x(), y, width(), height()); }
    void set_width(int width) { set_relative_rect(x(), y(), width, height()); }
    void set_height(int height) { set_relative_rect(x(), y(), width(), height); }

    void move_to(const Gfx::IntPoint& point) { set_relative_rect({ point, relative_rect().size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }
    void resize(const Gfx::IntSize& size) { set_relative_rect({ relative_rect().location(), size }); }
    void resize(int width, int height) { resize({ width, height }); }

    void move_by(int x, int y) { move_by({ x, y }); }
    void move_by(const Gfx::IntPoint& delta) { set_relative_rect({ relative_position().translated(delta), size() }); }

    Gfx::ColorRole background_role() const { return m_background_role; }
    void set_background_role(Gfx::ColorRole);

    Gfx::ColorRole foreground_role() const { return m_foreground_role; }
    void set_foreground_role(Gfx::ColorRole);

    void set_autofill(bool b) { set_fill_with_background_color(b); }

    Window* window()
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    const Window* window() const
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    void set_window(Window*);

    Widget* parent_widget();
    const Widget* parent_widget() const;

    void set_fill_with_background_color(bool b) { m_fill_with_background_color = b; }
    bool fill_with_background_color() const { return m_fill_with_background_color; }

    const Gfx::Font& font() const { return *m_font; }

    void set_font(const Gfx::Font*);
    void set_font(const Gfx::Font& font) { set_font(&font); }

    void set_font_family(const String&);
    void set_font_size(unsigned);
    void set_font_weight(unsigned);
    void set_font_fixed_width(bool);

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

    Action* action_for_key_event(const KeyEvent&);

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
    void set_palette(const Gfx::Palette&);

    const Margins& grabbable_margins() const { return m_grabbable_margins; }
    void set_grabbable_margins(const Margins&);

    Gfx::IntRect relative_non_grabbable_rect() const;

    void set_accepts_emoji_input(bool b) { m_accepts_emoji_input = b; }
    bool accepts_emoji_input() const { return m_accepts_emoji_input; }

    void set_accepts_command_palette(bool b) { m_accepts_command_palette = b; }
    bool accepts_command_palette() const { return m_accepts_command_palette; }

    virtual Gfx::IntRect children_clip_rect() const;

    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> override_cursor() const { return m_override_cursor; }
    void set_override_cursor(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>>);

    bool load_from_gml(StringView);
    bool load_from_gml(StringView, RefPtr<Core::Object> (*unregistered_child_handler)(const String&));

    void set_shrink_to_fit(bool);
    bool is_shrink_to_fit() const { return m_shrink_to_fit; }

    bool has_pending_drop() const;

    // In order for others to be able to call this, it needs to be public.
    virtual bool load_from_gml_ast(NonnullRefPtr<GUI::GML::Node> ast, RefPtr<Core::Object> (*unregistered_child_handler)(const String&));

protected:
    Widget();

    virtual void event(Core::Event&) override;

    // This is called after children have been painted.
    virtual void second_paint_event(PaintEvent&);

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

    virtual void did_begin_inspection() override;
    virtual void did_end_inspection() override;

    void show_or_hide_tooltip();

private:
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

    Window* m_window { nullptr };
    RefPtr<Layout> m_layout;

    Gfx::IntRect m_relative_rect;
    Gfx::ColorRole m_background_role;
    Gfx::ColorRole m_foreground_role;
    NonnullRefPtr<Gfx::Font> m_font;
    String m_tooltip;

    Gfx::IntSize m_min_size { -1, -1 };
    Gfx::IntSize m_max_size { -1, -1 };
    Margins m_grabbable_margins;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
    bool m_greedy_for_hits { false };
    bool m_auto_focusable { true };
    bool m_enabled { true };
    bool m_updates_enabled { true };
    bool m_accepts_emoji_input { false };
    bool m_accepts_command_palette { true };
    bool m_shrink_to_fit { false };
    bool m_default_font { true };

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;

    WeakPtr<Widget> m_focus_proxy;
    FocusPolicy m_focus_policy { FocusPolicy::NoFocus };

    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> m_override_cursor { Gfx::StandardCursor::None };
};

inline Widget* Widget::parent_widget()
{
    if (parent() && is<Widget>(*parent()))
        return &verify_cast<Widget>(*parent());
    return nullptr;
}
inline const Widget* Widget::parent_widget() const
{
    if (parent() && is<Widget>(*parent()))
        return &verify_cast<const Widget>(*parent());
    return nullptr;
}
}

template<>
struct AK::Formatter<GUI::Widget> : AK::Formatter<Core::Object> {
};
