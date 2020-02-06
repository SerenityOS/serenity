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

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Object.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibGfx/SystemTheme.h>
#include <LibGUI/Event.h>
#include <LibGUI/Shortcut.h>

#define REGISTER_GWIDGET(class_name)                          \
    extern WidgetClassRegistration registration_##class_name; \
    WidgetClassRegistration registration_##class_name(#class_name, [](Widget* parent) { return class_name::construct(parent); });

namespace Gfx {
class Bitmap;
}

namespace GUI {
class Widget;
}

template<>
inline bool Core::is<GUI::Widget>(const Core::Object& object)
{
    return object.is_widget();
}

namespace GUI {

class Action;
class Layout;
class Menu;
class Window;

enum class SizePolicy {
    Fixed,
    Fill
};
inline const char* to_string(SizePolicy policy)
{
    switch (policy) {
    case SizePolicy::Fixed:
        return "SizePolicy::Fixed";
    case SizePolicy::Fill:
        return "SizePolicy::Fill";
    }
    return "SizePolicy::(Invalid)";
}

enum class HorizontalDirection {
    Left,
    Right
};
enum class VerticalDirection {
    Up,
    Down
};

class Widget;

class WidgetClassRegistration {
    AK_MAKE_NONCOPYABLE(WidgetClassRegistration)
    AK_MAKE_NONMOVABLE(WidgetClassRegistration)
public:
    WidgetClassRegistration(const String& class_name, Function<NonnullRefPtr<Widget>(Widget*)> factory);
    ~WidgetClassRegistration();

    String class_name() const { return m_class_name; }
    NonnullRefPtr<Widget> construct(Widget* parent) const { return m_factory(parent); }

    static void for_each(Function<void(const WidgetClassRegistration&)>);
    static const WidgetClassRegistration* find(const String& class_name);

private:
    String m_class_name;
    Function<NonnullRefPtr<Widget>(Widget*)> m_factory;
};

class Widget : public Core::Object {
    C_OBJECT(Widget)
public:
    virtual ~Widget() override;

    Layout* layout() { return m_layout.ptr(); }
    const Layout* layout() const { return m_layout.ptr(); }
    void set_layout(OwnPtr<Layout>&&);

    SizePolicy horizontal_size_policy() const { return m_horizontal_size_policy; }
    SizePolicy vertical_size_policy() const { return m_vertical_size_policy; }
    SizePolicy size_policy(Orientation orientation) { return orientation == Orientation::Horizontal ? m_horizontal_size_policy : m_vertical_size_policy; }
    void set_size_policy(SizePolicy horizontal_policy, SizePolicy vertical_policy);
    void set_size_policy(Orientation, SizePolicy);

    Gfx::Size preferred_size() const { return m_preferred_size; }
    void set_preferred_size(const Gfx::Size&);
    void set_preferred_size(int width, int height) { set_preferred_size({ width, height }); }

    bool has_tooltip() const { return !m_tooltip.is_empty(); }
    String tooltip() const { return m_tooltip; }
    void set_tooltip(const StringView& tooltip) { m_tooltip = tooltip; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool updates_enabled() const { return m_updates_enabled; }
    void set_updates_enabled(bool);

    virtual void event(Core::Event&) override;

    // This is called after children have been painted.
    virtual void second_paint_event(PaintEvent&);

    Gfx::Rect relative_rect() const { return m_relative_rect; }
    Gfx::Point relative_position() const { return m_relative_rect.location(); }

    Gfx::Rect window_relative_rect() const;
    Gfx::Rect screen_relative_rect() const;

    int x() const { return m_relative_rect.x(); }
    int y() const { return m_relative_rect.y(); }
    int width() const { return m_relative_rect.width(); }
    int height() const { return m_relative_rect.height(); }
    int length(Orientation orientation) const { return orientation == Orientation::Vertical ? height() : width(); }

    Gfx::Rect rect() const { return { 0, 0, width(), height() }; }
    Gfx::Size size() const { return m_relative_rect.size(); }

    void update();
    void update(const Gfx::Rect&);

    virtual bool accepts_focus() const { return false; }

    bool is_focused() const;
    void set_focus(bool);

    enum class ShouldRespectGreediness { No = 0,
        Yes };
    struct HitTestResult {
        Widget* widget { nullptr };
        Gfx::Point local_position;
    };
    HitTestResult hit_test(const Gfx::Point&, ShouldRespectGreediness = ShouldRespectGreediness::Yes);
    Widget* child_at(const Gfx::Point&) const;

    void set_relative_rect(const Gfx::Rect&);
    void set_relative_rect(int x, int y, int width, int height) { set_relative_rect({ x, y, width, height }); }

    void set_x(int x) { set_relative_rect(x, y(), width(), height()); }
    void set_y(int y) { set_relative_rect(x(), y, width(), height()); }
    void set_width(int width) { set_relative_rect(x(), y(), width, height()); }
    void set_height(int height) { set_relative_rect(x(), y(), width(), height); }

    void move_to(const Gfx::Point& point) { set_relative_rect({ point, relative_rect().size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }
    void resize(const Gfx::Size& size) { set_relative_rect({ relative_rect().location(), size }); }
    void resize(int width, int height) { resize({ width, height }); }

    void move_by(int x, int y) { move_by({ x, y }); }
    void move_by(const Gfx::Point& delta) { set_relative_rect({ relative_position().translated(delta), size() }); }

    ColorRole background_role() const { return m_background_role; }
    void set_background_role(ColorRole role) { m_background_role = role; }

    ColorRole foreground_role() const { return m_foreground_role; }
    void set_foreground_role(ColorRole role) { m_foreground_role = role; }

    Color background_color() const { return m_background_color; }
    Color foreground_color() const { return m_foreground_color; }

    void set_background_color(Color color) { m_background_color = color; }
    void set_foreground_color(Color color) { m_foreground_color = color; }

    void set_backcolor(const StringView&);
    void set_forecolor(const StringView&);

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
            if (Core::is<Widget>(child))
                return callback(Core::to<Widget>(child));
            return IterationDecision::Continue;
        });
    }

    Vector<Widget*> child_widgets() const;

    virtual bool is_radio_button() const { return false; }
    virtual bool is_abstract_button() const { return false; }

    virtual void save_to(AK::JsonObject&) override;

    void do_layout();

    Palette palette() const { return Palette(*m_palette); }
    void set_palette(const Palette&);

protected:
    explicit Widget(Widget* parent = nullptr);

    virtual void custom_layout() {}
    virtual void did_change_font() {}
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
    virtual void click_event(MouseEvent&);
    virtual void doubleclick_event(MouseEvent&);
    virtual void context_menu_event(ContextMenuEvent&);
    virtual void focusin_event(Core::Event&);
    virtual void focusout_event(Core::Event&);
    virtual void enter_event(Core::Event&);
    virtual void leave_event(Core::Event&);
    virtual void child_event(Core::ChildEvent&) override;
    virtual void change_event(Event&);
    virtual void drop_event(DropEvent&);

private:
    void handle_paint_event(PaintEvent&);
    void handle_resize_event(ResizeEvent&);
    void handle_mousedown_event(MouseEvent&);
    void handle_mousedoubleclick_event(MouseEvent&);
    void handle_mouseup_event(MouseEvent&);
    void handle_enter_event(Core::Event&);
    void handle_leave_event(Core::Event&);
    void focus_previous_widget();
    void focus_next_widget();

    Window* m_window { nullptr };
    OwnPtr<Layout> m_layout;

    Gfx::Rect m_relative_rect;
    ColorRole m_background_role { ColorRole::Window };
    ColorRole m_foreground_role { ColorRole::WindowText };
    Color m_background_color;
    Color m_foreground_color;
    NonnullRefPtr<Gfx::Font> m_font;
    String m_tooltip;

    SizePolicy m_horizontal_size_policy { SizePolicy::Fill };
    SizePolicy m_vertical_size_policy { SizePolicy::Fill };
    Gfx::Size m_preferred_size;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
    bool m_greedy_for_hits { false };
    bool m_enabled { true };
    bool m_updates_enabled { true };

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;
};

inline Widget* Widget::parent_widget()
{
    if (parent() && Core::is<Widget>(*parent()))
        return &Core::to<Widget>(*parent());
    return nullptr;
}
inline const Widget* Widget::parent_widget() const
{
    if (parent() && Core::is<Widget>(*parent()))
        return &Core::to<const Widget>(*parent());
    return nullptr;
}
}
