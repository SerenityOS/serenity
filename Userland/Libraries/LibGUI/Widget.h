/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Margins.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>

#define REGISTER_WIDGET(namespace_, class_name)                                                                                                 \
    namespace {                                                                                                                                 \
    GUI::WidgetClassRegistration registration_##class_name(#namespace_ "::" #class_name, []() { return namespace_::class_name::construct(); }); \
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

class WidgetClassRegistration {
    AK_MAKE_NONCOPYABLE(WidgetClassRegistration);
    AK_MAKE_NONMOVABLE(WidgetClassRegistration);

public:
    WidgetClassRegistration(const String& class_name, Function<NonnullRefPtr<Widget>()> factory);
    ~WidgetClassRegistration();

    String class_name() const { return m_class_name; }
    NonnullRefPtr<Widget> construct() const { return m_factory(); }

    static void for_each(Function<void(const WidgetClassRegistration&)>);
    static const WidgetClassRegistration* find(const String& class_name);

private:
    String m_class_name;
    Function<NonnullRefPtr<Widget>()> m_factory;
};

enum class FocusPolicy {
    NoFocus = 0,
    TabFocus = 0x1,
    ClickFocus = 0x2,
    StrongFocus = TabFocus | ClickFocus,
};

class Widget : public Core::Object {
    C_OBJECT(Widget)
public:
    virtual ~Widget() override;

    Layout* layout() { return m_layout.ptr(); }
    const Layout* layout() const { return m_layout.ptr(); }
    void set_layout(NonnullRefPtr<Layout>);

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

    bool has_tooltip() const { return !m_tooltip.is_empty(); }
    String tooltip() const { return m_tooltip; }
    void set_tooltip(const StringView&);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool updates_enabled() const { return m_updates_enabled; }
    void set_updates_enabled(bool);

    virtual void event(Core::Event&) override;

    // This is called after children have been painted.
    virtual void second_paint_event(PaintEvent&);

    Gfx::IntRect relative_rect() const { return m_relative_rect; }
    Gfx::IntPoint relative_position() const { return m_relative_rect.location(); }

    Gfx::IntRect window_relative_rect() const;
    Gfx::IntRect screen_relative_rect() const;

    int x() const { return m_relative_rect.x(); }
    int y() const { return m_relative_rect.y(); }
    int width() const { return m_relative_rect.width(); }
    int height() const { return m_relative_rect.height(); }
    int length(Orientation orientation) const { return orientation == Orientation::Vertical ? height() : width(); }

    Gfx::IntRect rect() const { return { 0, 0, width(), height() }; }
    Gfx::IntSize size() const { return m_relative_rect.size(); }

    void update();
    void update(const Gfx::IntRect&);

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

    void set_global_cursor_tracking(bool);
    bool global_cursor_tracking() const;

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
                return callback(downcast<Widget>(child));
            return IterationDecision::Continue;
        });
    }

    Vector<Widget*> child_widgets() const;

    void do_layout();

    Gfx::Palette palette() const;
    void set_palette(const Gfx::Palette&);

    const Margins& content_margins() const { return m_content_margins; }
    void set_content_margins(const Margins&);

    Gfx::IntRect content_rect() const;

    void set_accepts_emoji_input(bool b) { m_accepts_emoji_input = b; }
    bool accepts_emoji_input() const { return m_accepts_emoji_input; }

    virtual Gfx::IntRect children_clip_rect() const;

    Gfx::StandardCursor override_cursor() const { return m_override_cursor; }
    void set_override_cursor(Gfx::StandardCursor);

    bool load_from_gml(const StringView&);
    bool load_from_gml(const StringView&, RefPtr<Widget> (*unregistered_child_handler)(const String&));

    void set_shrink_to_fit(bool);
    bool is_shrink_to_fit() const { return m_shrink_to_fit; }

    bool has_pending_drop() const;

protected:
    Widget();

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

    virtual void did_begin_inspection() override;
    virtual void did_end_inspection() override;

    void show_or_hide_tooltip();

private:
    void handle_paint_event(PaintEvent&);
    void handle_resize_event(ResizeEvent&);
    void handle_mousedown_event(MouseEvent&);
    void handle_mousedoubleclick_event(MouseEvent&);
    void handle_mouseup_event(MouseEvent&);
    void handle_enter_event(Core::Event&);
    void handle_leave_event(Core::Event&);
    void focus_previous_widget(FocusSource, bool siblings_only);
    void focus_next_widget(FocusSource, bool siblings_only);

    bool load_from_json(const JsonObject&, RefPtr<Widget> (*unregistered_child_handler)(const String&));

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
    Margins m_content_margins;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
    bool m_greedy_for_hits { false };
    bool m_enabled { true };
    bool m_updates_enabled { true };
    bool m_accepts_emoji_input { false };
    bool m_shrink_to_fit { false };

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;

    WeakPtr<Widget> m_focus_proxy;
    FocusPolicy m_focus_policy { FocusPolicy::NoFocus };

    Gfx::StandardCursor m_override_cursor { Gfx::StandardCursor::None };
};

inline Widget* Widget::parent_widget()
{
    if (parent() && is<Widget>(*parent()))
        return &downcast<Widget>(*parent());
    return nullptr;
}
inline const Widget* Widget::parent_widget() const
{
    if (parent() && is<Widget>(*parent()))
        return &downcast<const Widget>(*parent());
    return nullptr;
}
}

template<>
struct AK::Formatter<GUI::Widget> : AK::Formatter<Core::Object> {
};
