#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibC/SharedBuffer.h>
#include <LibC/stdio.h>
#include <LibC/stdlib.h>
#include <LibC/unistd.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GEvent.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>

//#define UPDATE_COALESCING_DEBUG

static HashTable<GWindow*> all_windows;
static HashMap<int, GWindow*> reified_windows;

GWindow* GWindow::from_window_id(int window_id)
{
    auto it = reified_windows.find(window_id);
    if (it != reified_windows.end())
        return (*it).value;
    return nullptr;
}

GWindow::GWindow(CObject* parent)
    : CObject(parent)
{
    all_windows.set(this);
    m_rect_when_windowless = { 100, 400, 140, 140 };
    m_title_when_windowless = "GWindow";
}

GWindow::~GWindow()
{
    all_windows.remove(this);
    hide();
}

void GWindow::close()
{
    hide();
}

void GWindow::move_to_front()
{
    if (!m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::MoveWindowToFront;
    request.window_id = m_window_id;
    GWindowServerConnection::the().post_message_to_server(request);
}

void GWindow::show()
{
    if (m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::CreateWindow;
    request.window_id = m_window_id;
    request.window.rect = m_rect_when_windowless;
    request.window.has_alpha_channel = m_has_alpha_channel;
    request.window.modal = m_modal;
    request.window.resizable = m_resizable;
    request.window.fullscreen = m_fullscreen;
    request.window.show_titlebar = m_show_titlebar;
    request.window.opacity = m_opacity_when_windowless;
    request.window.background_color = m_background_color.value();
    request.window.size_increment = m_size_increment;
    request.window.base_size = m_base_size;
    request.window.type = (WSAPI_WindowType)m_window_type;
    ASSERT(m_title_when_windowless.length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    auto response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidCreateWindow);
    m_window_id = response.window_id;

    apply_icon();

    reified_windows.set(m_window_id, this);
    GApplication::the().did_create_window({});
    update();
}

void GWindow::hide()
{
    if (!m_window_id)
        return;
    reified_windows.remove(m_window_id);
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::DestroyWindow;
    request.window_id = m_window_id;
    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidDestroyWindow);
    m_window_id = 0;
    m_pending_paint_event_rects.clear();
    m_back_bitmap = nullptr;
    m_front_bitmap = nullptr;

    bool app_has_visible_windows = false;
    for (auto& window : all_windows) {
        if (window->is_visible()) {
            app_has_visible_windows = true;
            break;
        }
    }
    if (!app_has_visible_windows)
        GApplication::the().did_delete_last_window({});
}

void GWindow::set_title(const StringView& title)
{
    m_title_when_windowless = title;
    if (!m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowTitle;
    request.window_id = m_window_id;
    ASSERT(m_title_when_windowless.length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    GWindowServerConnection::the().post_message_to_server(request);
}

String GWindow::title() const
{
    if (!m_window_id)
        return m_title_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowTitle;
    request.window_id = m_window_id;
    auto response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowTitle);
    return String(response.text, response.text_length);
}

Rect GWindow::rect() const
{
    if (!m_window_id)
        return m_rect_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowRect;
    request.window_id = m_window_id;
    auto response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowRect);
    ASSERT(response.window_id == m_window_id);
    return response.window.rect;
}

void GWindow::set_rect(const Rect& a_rect)
{
    m_rect_when_windowless = a_rect;
    if (!m_window_id) {
        if (m_main_widget)
            m_main_widget->resize(m_rect_when_windowless.size());
        return;
    }
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowRect;
    request.window_id = m_window_id;
    request.window.rect = a_rect;
    GWindowServerConnection::the().post_message_to_server(request);
    if (m_back_bitmap && m_back_bitmap->size() != a_rect.size())
        m_back_bitmap = nullptr;
    if (m_front_bitmap && m_front_bitmap->size() != a_rect.size())
        m_front_bitmap = nullptr;
    if (m_main_widget)
        m_main_widget->resize(a_rect.size());
}

void GWindow::set_window_type(GWindowType window_type)
{
    m_window_type = window_type;
}

void GWindow::set_override_cursor(GStandardCursor cursor)
{
    if (!m_window_id)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowOverrideCursor;
    request.window_id = m_window_id;
    request.cursor.cursor = (WSAPI_StandardCursor)cursor;
    GWindowServerConnection::the().post_message_to_server(request);
}

void GWindow::event(CEvent& event)
{
    if (event.type() == GEvent::MouseUp || event.type() == GEvent::MouseDown || event.type() == GEvent::MouseDoubleClick || event.type() == GEvent::MouseMove || event.type() == GEvent::MouseWheel) {
        auto& mouse_event = static_cast<GMouseEvent&>(event);
        if (m_global_cursor_tracking_widget) {
            auto window_relative_rect = m_global_cursor_tracking_widget->window_relative_rect();
            Point local_point { mouse_event.x() - window_relative_rect.x(), mouse_event.y() - window_relative_rect.y() };
            auto local_event = make<GMouseEvent>((GEvent::Type)event.type(), local_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
            m_global_cursor_tracking_widget->dispatch_event(*local_event, this);
            return;
        }
        if (m_automatic_cursor_tracking_widget) {
            auto window_relative_rect = m_automatic_cursor_tracking_widget->window_relative_rect();
            Point local_point { mouse_event.x() - window_relative_rect.x(), mouse_event.y() - window_relative_rect.y() };
            auto local_event = make<GMouseEvent>((GEvent::Type)event.type(), local_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
            m_automatic_cursor_tracking_widget->dispatch_event(*local_event, this);
            if (mouse_event.buttons() == 0)
                m_automatic_cursor_tracking_widget = nullptr;
            return;
        }
        if (!m_main_widget)
            return;
        auto result = m_main_widget->hit_test(mouse_event.position());
        auto local_event = make<GMouseEvent>((GEvent::Type)event.type(), result.local_position, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
        ASSERT(result.widget);
        set_hovered_widget(result.widget);
        if (mouse_event.buttons() != 0 && !m_automatic_cursor_tracking_widget)
            m_automatic_cursor_tracking_widget = result.widget->make_weak_ptr();
        if (result.widget != m_global_cursor_tracking_widget.ptr())
            return result.widget->dispatch_event(*local_event, this);
        return;
    }

    if (event.type() == GEvent::MultiPaint) {
        if (!m_window_id)
            return;
        if (!m_main_widget)
            return;
        auto& paint_event = static_cast<GMultiPaintEvent&>(event);
        auto rects = paint_event.rects();
        ASSERT(!rects.is_empty());
        if (m_back_bitmap && m_back_bitmap->size() != paint_event.window_size()) {
            // Eagerly discard the backing store if we learn from this paint event that it needs to be bigger.
            // Otherwise we would have to wait for a resize event to tell us. This way we don't waste the
            // effort on painting into an undersized bitmap that will be thrown away anyway.
            m_back_bitmap = nullptr;
        }
        bool created_new_backing_store = !m_back_bitmap;
        if (!m_back_bitmap)
            m_back_bitmap = create_backing_bitmap(paint_event.window_size());

        auto rect = rects.first();
        if (rect.is_empty() || created_new_backing_store) {
            rects.clear();
            rects.append({ {}, paint_event.window_size() });
        }

        for (auto& rect : rects)
            m_main_widget->dispatch_event(*make<GPaintEvent>(rect), this);

        paint_keybinds();

        if (m_double_buffering_enabled)
            flip(rects);
        else if (created_new_backing_store)
            set_current_backing_bitmap(*m_back_bitmap, true);

        if (m_window_id) {
            WSAPI_ClientMessage message;
            message.type = WSAPI_ClientMessage::Type::DidFinishPainting;
            message.window_id = m_window_id;
            message.rect_count = rects.size();
            for (int i = 0; i < min(WSAPI_ClientMessage::max_inline_rect_count, rects.size()); ++i)
                message.rects[i] = rects[i];
            ByteBuffer extra_data;
            if (rects.size() > WSAPI_ClientMessage::max_inline_rect_count)
                extra_data = ByteBuffer::wrap(&rects[WSAPI_ClientMessage::max_inline_rect_count], (rects.size() - WSAPI_ClientMessage::max_inline_rect_count) * sizeof(WSAPI_Rect));
            GWindowServerConnection::the().post_message_to_server(message, move(extra_data));
        }
        return;
    }

    if (event.type() == GEvent::KeyUp || event.type() == GEvent::KeyDown) {
        auto keyevent = static_cast<GKeyEvent&>(event);
        if (keyevent.logo() && event.type() == GEvent::KeyUp) {
            if (m_keybind_mode) {
                m_keybind_mode = false;
            } else {
                m_keybind_mode = true;
                collect_keyboard_activation_targets();
                m_entered_keybind = "";
            }
            update();
            return;
        }

        if (m_keybind_mode) {
            if (event.type() == GEvent::KeyUp) {
                StringBuilder builder;
                builder.append(m_entered_keybind);
                builder.append(keyevent.text());
                m_entered_keybind = builder.to_string();

                auto found_widget = m_keyboard_activation_targets.find(m_entered_keybind);
                if (found_widget != m_keyboard_activation_targets.end()) {
                    m_keybind_mode = false;
                    auto event = make<GMouseEvent>(GEvent::MouseDown, Point(), 0, GMouseButton::Left, 0, 0);
                    found_widget->value->dispatch_event(*event, this);
                    event = make<GMouseEvent>(GEvent::MouseUp, Point(), 0, GMouseButton::Left, 0, 0);
                    found_widget->value->dispatch_event(*event, this);
                } else if (m_entered_keybind.length() >= m_max_keybind_length) {
                    m_keybind_mode = false;
                }
                update();
            }
        } else {
            if (m_focused_widget)
                return m_focused_widget->dispatch_event(event, this);
            if (m_main_widget)
                return m_main_widget->dispatch_event(event, this);
        }
        return;
    }

    if (event.type() == GEvent::WindowBecameActive || event.type() == GEvent::WindowBecameInactive) {
        if (event.type() == GEvent::WindowBecameInactive && m_keybind_mode) {
            m_keybind_mode = false;
            update();
        }

        m_is_active = event.type() == GEvent::WindowBecameActive;
        if (m_main_widget)
            m_main_widget->dispatch_event(event, this);
        if (m_focused_widget)
            m_focused_widget->update();
        return;
    }

    if (event.type() == GEvent::WindowCloseRequest) {
        if (on_close_request) {
            if (on_close_request() == GWindow::CloseRequestDecision::StayOpen)
                return;
        }
        close();
        return;
    }

    if (event.type() == GEvent::WindowLeft) {
        set_hovered_widget(nullptr);
        return;
    }

    if (event.type() == GEvent::Resize) {
        auto new_size = static_cast<GResizeEvent&>(event).size();
        if (m_back_bitmap && m_back_bitmap->size() != new_size)
            m_back_bitmap = nullptr;
        if (!m_pending_paint_event_rects.is_empty()) {
            m_pending_paint_event_rects.clear_with_capacity();
            m_pending_paint_event_rects.append({ {}, new_size });
        }
        m_rect_when_windowless = { {}, new_size };
        m_main_widget->set_relative_rect({ {}, new_size });
        return;
    }

    if (event.type() > GEvent::__Begin_WM_Events && event.type() < GEvent::__End_WM_Events)
        return wm_event(static_cast<GWMEvent&>(event));

    CObject::event(event);
}

void GWindow::paint_keybinds()
{
    if (!m_keybind_mode)
        return;
    GPainter painter(*m_main_widget);

    for (auto& keypair : m_keyboard_activation_targets) {
        if (!keypair.value)
            continue;
        auto& widget = *keypair.value;
        bool could_be_keybind = true;
        for (int i = 0; i < m_entered_keybind.length(); ++i) {
            if (keypair.key.characters()[i] != m_entered_keybind.characters()[i]) {
                could_be_keybind = false;
                break;
            }
        }

        if (could_be_keybind) {
            Rect rect { widget.x() - 5, widget.y() - 5, 4 + Font::default_font().width(keypair.key), 16 };
            Rect highlight_rect { widget.x() - 3, widget.y() - 5, 0, 16 };

            painter.fill_rect(rect, Color::WarmGray);
            painter.draw_rect(rect, Color::Black);
            painter.draw_text(rect, keypair.key.characters(), TextAlignment::Center, Color::Black);
            painter.draw_text(highlight_rect, m_entered_keybind.characters(), TextAlignment::CenterLeft, Color::MidGray);
        }
    }
}

static void collect_keyboard_activation_targets_impl(GWidget& widget, Vector<GWidget*>& targets)
{
    widget.for_each_child_widget([&](auto& child) {
        if (child.supports_keyboard_activation()) {
            targets.append(&child);
            collect_keyboard_activation_targets_impl(child, targets);
        }
        return IterationDecision::Continue;
    });
}

void GWindow::collect_keyboard_activation_targets()
{
    m_keyboard_activation_targets.clear();
    if (!m_main_widget)
        return;

    Vector<GWidget*> targets;
    collect_keyboard_activation_targets_impl(*m_main_widget, targets);

    m_max_keybind_length = ceil_div(targets.size(), ('z' - 'a'));
    size_t buffer_length = m_max_keybind_length + 1;
    char keybind_buffer[buffer_length];
    for (size_t i = 0; i < buffer_length - 1; ++i)
        keybind_buffer[i] = 'a';
    keybind_buffer[buffer_length - 1] = '\0';

    for (auto& widget : targets) {
        m_keyboard_activation_targets.set(String(keybind_buffer), widget->make_weak_ptr());
        for (size_t i = 0; i < buffer_length - 1; ++i) {
            if (keybind_buffer[i] >= 'z') {
                keybind_buffer[i] = 'a';
            } else {
                ++keybind_buffer[i];
                break;
            }
        }
    }
}

bool GWindow::is_visible() const
{
    return m_window_id != 0;
}

void GWindow::update(const Rect& a_rect)
{
    if (!m_window_id)
        return;

    for (auto& pending_rect : m_pending_paint_event_rects) {
        if (pending_rect.contains(a_rect)) {
#ifdef UPDATE_COALESCING_DEBUG
            dbgprintf("Ignoring %s since it's contained by pending rect %s\n", a_rect.to_string().characters(), pending_rect.to_string().characters());
#endif
            return;
        }
    }

    if (m_pending_paint_event_rects.is_empty()) {
        deferred_invoke([this](auto&) {
            auto rects = move(m_pending_paint_event_rects);
            if (rects.is_empty())
                return;
            WSAPI_ClientMessage request;
            request.type = WSAPI_ClientMessage::Type::InvalidateRect;
            request.window_id = m_window_id;
            for (int i = 0; i < min(WSAPI_ClientMessage::max_inline_rect_count, rects.size()); ++i)
                request.rects[i] = rects[i];
            ByteBuffer extra_data;
            if (rects.size() > WSAPI_ClientMessage::max_inline_rect_count)
                extra_data = ByteBuffer::wrap(&rects[WSAPI_ClientMessage::max_inline_rect_count], (rects.size() - WSAPI_ClientMessage::max_inline_rect_count) * sizeof(WSAPI_Rect));
            request.rect_count = rects.size();
            GWindowServerConnection::the().post_message_to_server(request, move(extra_data));
        });
    }
    m_pending_paint_event_rects.append(a_rect);
}

void GWindow::set_main_widget(GWidget* widget)
{
    if (m_main_widget == widget)
        return;
    if (m_main_widget)
        remove_child(*m_main_widget);
    m_main_widget = widget;
    if (m_main_widget) {
        add_child(*widget);
        auto new_window_rect = rect();
        if (m_main_widget->horizontal_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_width(m_main_widget->preferred_size().width());
        if (m_main_widget->vertical_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_height(m_main_widget->preferred_size().height());
        set_rect(new_window_rect);
        m_main_widget->set_relative_rect({ {}, new_window_rect.size() });
        m_main_widget->set_window(this);
        if (m_main_widget->accepts_focus())
            m_main_widget->set_focus(true);
    }
    update();
}

void GWindow::set_focused_widget(GWidget* widget)
{
    if (m_focused_widget == widget)
        return;
    if (m_focused_widget) {
        CEventLoop::current().post_event(*m_focused_widget, make<GEvent>(GEvent::FocusOut));
        m_focused_widget->update();
    }
    m_focused_widget = widget ? widget->make_weak_ptr() : nullptr;
    if (m_focused_widget) {
        CEventLoop::current().post_event(*m_focused_widget, make<GEvent>(GEvent::FocusIn));
        m_focused_widget->update();
    }
}

void GWindow::set_global_cursor_tracking_widget(GWidget* widget)
{
    if (widget == m_global_cursor_tracking_widget)
        return;
    m_global_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;
}

void GWindow::set_automatic_cursor_tracking_widget(GWidget* widget)
{
    if (widget == m_automatic_cursor_tracking_widget)
        return;
    m_automatic_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;
}

void GWindow::set_has_alpha_channel(bool value)
{
    if (m_has_alpha_channel == value)
        return;
    m_has_alpha_channel = value;
    if (!m_window_id)
        return;

    m_pending_paint_event_rects.clear();
    m_back_bitmap = nullptr;
    m_front_bitmap = nullptr;

    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::SetWindowHasAlphaChannel;
    message.window_id = m_window_id;
    message.value = value;
    GWindowServerConnection::the().sync_request(message, WSAPI_ServerMessage::DidSetWindowHasAlphaChannel);

    update();
}

void GWindow::set_double_buffering_enabled(bool value)
{
    ASSERT(!m_window_id);
    m_double_buffering_enabled = value;
}

void GWindow::set_opacity(float opacity)
{
    m_opacity_when_windowless = opacity;
    if (!m_window_id)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowOpacity;
    request.window_id = m_window_id;
    request.window.opacity = opacity;
    m_opacity_when_windowless = opacity;
    GWindowServerConnection::the().post_message_to_server(request);
}

void GWindow::set_hovered_widget(GWidget* widget)
{
    if (widget == m_hovered_widget)
        return;

    if (m_hovered_widget)
        CEventLoop::current().post_event(*m_hovered_widget, make<GEvent>(GEvent::Leave));

    m_hovered_widget = widget ? widget->make_weak_ptr() : nullptr;

    if (m_hovered_widget)
        CEventLoop::current().post_event(*m_hovered_widget, make<GEvent>(GEvent::Enter));
}

void GWindow::set_current_backing_bitmap(GraphicsBitmap& bitmap, bool flush_immediately)
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::SetWindowBackingStore;
    message.window_id = m_window_id;
    message.backing.bpp = 32;
    message.backing.pitch = bitmap.pitch();
    message.backing.shared_buffer_id = bitmap.shared_buffer_id();
    message.backing.has_alpha_channel = bitmap.has_alpha_channel();
    message.backing.size = bitmap.size();
    message.backing.flush_immediately = flush_immediately;
    GWindowServerConnection::the().sync_request(message, WSAPI_ServerMessage::Type::DidSetWindowBackingStore);
}

void GWindow::flip(const Vector<Rect, 32>& dirty_rects)
{
    swap(m_front_bitmap, m_back_bitmap);

    set_current_backing_bitmap(*m_front_bitmap);

    if (!m_back_bitmap || m_back_bitmap->size() != m_front_bitmap->size()) {
        m_back_bitmap = create_backing_bitmap(m_front_bitmap->size());
        memcpy(m_back_bitmap->scanline(0), m_front_bitmap->scanline(0), m_front_bitmap->size_in_bytes());
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(*m_back_bitmap);
    for (auto& dirty_rect : dirty_rects)
        painter.blit(dirty_rect.location(), *m_front_bitmap, dirty_rect);
}

NonnullRefPtr<GraphicsBitmap> GWindow::create_shared_bitmap(GraphicsBitmap::Format format, const Size& size)
{
    ASSERT(GWindowServerConnection::the().server_pid());
    ASSERT(!size.is_empty());
    size_t pitch = round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16);
    size_t size_in_bytes = size.height() * pitch;
    auto shared_buffer = SharedBuffer::create_with_size(size_in_bytes);
    ASSERT(shared_buffer);
    shared_buffer->share_with(GWindowServerConnection::the().server_pid());
    return GraphicsBitmap::create_with_shared_buffer(format, *shared_buffer, size);
}

NonnullRefPtr<GraphicsBitmap> GWindow::create_backing_bitmap(const Size& size)
{
    auto format = m_has_alpha_channel ? GraphicsBitmap::Format::RGBA32 : GraphicsBitmap::Format::RGB32;
    return create_shared_bitmap(format, size);
}

void GWindow::set_modal(bool modal)
{
    ASSERT(!m_window_id);
    m_modal = modal;
}

void GWindow::wm_event(GWMEvent&)
{
}

void GWindow::set_icon(const GraphicsBitmap* icon)
{
    if (m_icon == icon)
        return;

    m_icon = create_shared_bitmap(GraphicsBitmap::Format::RGBA32, icon->size());
    {
        GPainter painter(*m_icon);
        painter.blit({ 0, 0 }, *icon, icon->rect());
    }

    apply_icon();
}

void GWindow::apply_icon()
{
    if (!m_icon)
        return;

    if (!m_window_id)
        return;

    int rc = seal_shared_buffer(m_icon->shared_buffer_id());
    ASSERT(rc == 0);

    rc = share_buffer_globally(m_icon->shared_buffer_id());
    ASSERT(rc == 0);

    static bool has_set_process_icon;
    if (!has_set_process_icon)
        set_process_icon(m_icon->shared_buffer_id());

    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::SetWindowIconBitmap;
    message.window_id = m_window_id;
    message.window.icon_buffer_id = m_icon->shared_buffer_id();
    message.window.icon_size = m_icon->size();
    GWindowServerConnection::the().post_message_to_server(message);
}

void GWindow::start_wm_resize()
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::WM_StartWindowResize;
    message.wm.client_id = GWindowServerConnection::the().my_client_id();
    message.wm.window_id = m_window_id;
    GWindowServerConnection::the().post_message_to_server(message);
}

Vector<GWidget*> GWindow::focusable_widgets() const
{
    if (!m_main_widget)
        return {};

    Vector<GWidget*> collected_widgets;

    Function<void(GWidget&)> collect_focusable_widgets = [&](GWidget& widget) {
        if (widget.accepts_focus())
            collected_widgets.append(&widget);
        widget.for_each_child_widget([&](auto& child) {
            if (!child.is_visible())
                return IterationDecision::Continue;
            if (!child.is_enabled())
                return IterationDecision::Continue;
            collect_focusable_widgets(child);
            return IterationDecision::Continue;
        });
    };

    collect_focusable_widgets(const_cast<GWidget&>(*m_main_widget));
    return collected_widgets;
}

void GWindow::save_to(AK::JsonObject& json)
{
    json.set("title", title());
    json.set("visible", is_visible());
    json.set("active", is_active());
    json.set("resizable", is_resizable());
    json.set("fullscreen", is_fullscreen());
    json.set("rect", rect().to_string());
    json.set("base_size", base_size().to_string());
    json.set("size_increment", size_increment().to_string());
    CObject::save_to(json);
}

void GWindow::set_fullscreen(bool fullscreen)
{
    if (m_fullscreen == fullscreen)
        return;
    m_fullscreen = fullscreen;
    if (!m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetFullscreen;
    request.window_id = m_window_id;
    request.value = fullscreen;
    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidSetFullscreen);
}

void GWindow::schedule_relayout()
{
    if (m_layout_pending)
        return;
    m_layout_pending = true;
    deferred_invoke([this](auto&) {
        if (main_widget())
            main_widget()->do_layout();
        update();
        m_layout_pending = false;
    });
}
