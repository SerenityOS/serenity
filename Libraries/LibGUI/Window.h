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

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/WindowType.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace GUI {

enum class StandardCursor {
    None = 0,
    Arrow,
    IBeam,
    ResizeHorizontal,
    ResizeVertical,
    ResizeDiagonalTLBR,
    ResizeDiagonalBLTR,
    Hand,
};

class Window : public Core::Object {
    C_OBJECT(Window)
public:
    virtual ~Window() override;

    static Window* from_window_id(int);

    bool is_modal() const { return m_modal; }
    void set_modal(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    bool is_resizable() const { return m_resizable; }
    void set_resizable(bool resizable) { m_resizable = resizable; }

    bool is_minimizable() const { return m_minimizable; }
    void set_minimizable(bool minimizable) { m_minimizable = minimizable; }

    void set_double_buffering_enabled(bool);
    void set_has_alpha_channel(bool);
    void set_opacity(float);
    void set_window_type(WindowType);

    int window_id() const { return m_window_id; }

    String title() const;
    void set_title(const StringView&);

    bool show_titlebar() const { return m_show_titlebar; };
    void set_show_titlebar(bool show) { m_show_titlebar = show; };

    Color background_color() const { return m_background_color; }
    void set_background_color(Color color) { m_background_color = color; }

    enum class CloseRequestDecision {
        StayOpen,
        Close,
    };

    Function<CloseRequestDecision()> on_close_request;

    int x() const { return rect().x(); }
    int y() const { return rect().y(); }
    int width() const { return rect().width(); }
    int height() const { return rect().height(); }

    Gfx::Rect rect() const;
    Gfx::Size size() const { return rect().size(); }
    void set_rect(const Gfx::Rect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }

    Gfx::Point position() const { return rect().location(); }

    void move_to(int x, int y) { move_to({ x, y }); }
    void move_to(const Gfx::Point& point) { set_rect({ point, size() }); }

    void resize(int width, int height) { resize({ width, height }); }
    void resize(const Gfx::Size& size) { set_rect({ position(), size }); }

    virtual void event(Core::Event&) override;

    bool is_visible() const;
    bool is_active() const { return m_is_active; }

    void show();
    void hide();
    virtual void close();
    void move_to_front();

    void start_wm_resize();

    Widget* main_widget() { return m_main_widget; }
    const Widget* main_widget() const { return m_main_widget; }
    void set_main_widget(Widget*);

    template<class T, class... Args>
    inline T& set_main_widget(Args&&... args)
    {
        auto widget = T::construct(forward<Args>(args)...);
        set_main_widget(widget.ptr());
        return *widget;
    }

    Widget* focused_widget() { return m_focused_widget; }
    const Widget* focused_widget() const { return m_focused_widget; }
    void set_focused_widget(Widget*);

    void update();
    void update(const Gfx::Rect&);

    void set_global_cursor_tracking_widget(Widget*);
    Widget* global_cursor_tracking_widget() { return m_global_cursor_tracking_widget.ptr(); }
    const Widget* global_cursor_tracking_widget() const { return m_global_cursor_tracking_widget.ptr(); }

    void set_automatic_cursor_tracking_widget(Widget*);
    Widget* automatic_cursor_tracking_widget() { return m_automatic_cursor_tracking_widget.ptr(); }
    const Widget* automatic_cursor_tracking_widget() const { return m_automatic_cursor_tracking_widget.ptr(); }

    Widget* hovered_widget() { return m_hovered_widget.ptr(); }
    const Widget* hovered_widget() const { return m_hovered_widget.ptr(); }
    void set_hovered_widget(Widget*);

    Gfx::Bitmap* front_bitmap() { return m_front_bitmap.ptr(); }
    Gfx::Bitmap* back_bitmap() { return m_back_bitmap.ptr(); }

    Gfx::Size size_increment() const { return m_size_increment; }
    void set_size_increment(const Gfx::Size&);
    Gfx::Size base_size() const { return m_base_size; }
    void set_base_size(const Gfx::Size&);

    void set_override_cursor(StandardCursor);

    void set_icon(const Gfx::Bitmap*);
    void apply_icon();
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }

    Vector<Widget*> focusable_widgets() const;

    virtual void save_to(AK::JsonObject&) override;

    void schedule_relayout();

    static void update_all_windows(Badge<WindowServerConnection>);
    void notify_state_changed(Badge<WindowServerConnection>, bool minimized, bool occluded);

    virtual bool is_visible_for_timer_purposes() const override { return m_visible_for_timer_purposes; }

    Action* action_for_key_event(const KeyEvent&);

protected:
    Window(Core::Object* parent = nullptr);
    virtual void wm_event(WMEvent&);

private:
    virtual bool is_window() const override final { return true; }

    NonnullRefPtr<Gfx::Bitmap> create_backing_bitmap(const Gfx::Size&);
    NonnullRefPtr<Gfx::Bitmap> create_shared_bitmap(Gfx::BitmapFormat, const Gfx::Size&);
    void set_current_backing_bitmap(Gfx::Bitmap&, bool flush_immediately = false);
    void flip(const Vector<Gfx::Rect, 32>& dirty_rects);
    void force_update();

    RefPtr<Gfx::Bitmap> m_front_bitmap;
    RefPtr<Gfx::Bitmap> m_back_bitmap;
    RefPtr<Gfx::Bitmap> m_icon;
    int m_window_id { 0 };
    float m_opacity_when_windowless { 1.0f };
    RefPtr<Widget> m_main_widget;
    WeakPtr<Widget> m_focused_widget;
    WeakPtr<Widget> m_global_cursor_tracking_widget;
    WeakPtr<Widget> m_automatic_cursor_tracking_widget;
    WeakPtr<Widget> m_hovered_widget;
    Gfx::Rect m_rect_when_windowless;
    String m_title_when_windowless;
    Vector<Gfx::Rect, 32> m_pending_paint_event_rects;
    Gfx::Size m_size_increment;
    Gfx::Size m_base_size;
    Color m_background_color { Color::WarmGray };
    WindowType m_window_type { WindowType::Normal };
    bool m_is_active { false };
    bool m_has_alpha_channel { false };
    bool m_double_buffering_enabled { true };
    bool m_modal { false };
    bool m_resizable { true };
    bool m_minimizable { true };
    bool m_fullscreen { false };
    bool m_show_titlebar { true };
    bool m_layout_pending { false };
    bool m_visible_for_timer_purposes { true };
};

}

template<>
inline bool Core::is<GUI::Window>(const Core::Object& object)
{
    return object.is_window();
}
