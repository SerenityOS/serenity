/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "WebContentView.h"
#include "ConsoleWidget.h"
#include "InspectorWidget.h"
#include "Utilities.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Browser/CookieJar.h>
#include <Kernel/API/KeyCode.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/IODevice.h>
#include <LibCore/MemoryStream.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Rect.h>
#include <LibGfx/SystemTheme.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibMain/Main.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWebView/WebContentClient.h>
#include <QApplication>
#include <QCursor>
#include <QIcon>
#include <QInputDialog>
#include <QLineEdit>
#include <QMessageBox>
#include <QMouseEvent>
#include <QPaintEvent>
#include <QPainter>
#include <QScrollBar>
#include <QSocketNotifier>
#include <QTextEdit>
#include <QTimer>
#include <QToolTip>

WebContentView::WebContentView(StringView webdriver_content_ipc_path)
    : m_webdriver_content_ipc_path(webdriver_content_ipc_path)
{
    setMouseTracking(true);

    setFocusPolicy(Qt::FocusPolicy::StrongFocus);

    m_device_pixel_ratio = devicePixelRatio();
    m_inverse_pixel_scaling_ratio = 1.0 / m_device_pixel_ratio;

    verticalScrollBar()->setSingleStep(24);
    horizontalScrollBar()->setSingleStep(24);

    QObject::connect(verticalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });
    QObject::connect(horizontalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });

    create_client();
}

WebContentView::~WebContentView()
{
    close_sub_widgets();
}

void WebContentView::load(AK::URL const& url)
{
    m_url = url;
    client().async_load_url(url);
}

void WebContentView::load_html(StringView html, AK::URL const& url)
{
    m_url = url;
    client().async_load_html(html, url);
}

unsigned get_button_from_qt_event(QMouseEvent const& event)
{
    if (event.button() == Qt::MouseButton::LeftButton)
        return 1;
    if (event.button() == Qt::MouseButton::RightButton)
        return 2;
    if (event.button() == Qt::MouseButton::MiddleButton)
        return 4;
    if (event.button() == Qt::MouseButton::BackButton)
        return 8;
    if (event.buttons() == Qt::MouseButton::ForwardButton)
        return 16;
    return 0;
}

unsigned get_buttons_from_qt_event(QMouseEvent const& event)
{
    unsigned buttons = 0;
    if (event.buttons() & Qt::MouseButton::LeftButton)
        buttons |= 1;
    if (event.buttons() & Qt::MouseButton::RightButton)
        buttons |= 2;
    if (event.buttons() & Qt::MouseButton::MiddleButton)
        buttons |= 4;
    if (event.buttons() & Qt::MouseButton::BackButton)
        buttons |= 8;
    if (event.buttons() & Qt::MouseButton::ForwardButton)
        buttons |= 16;
    return buttons;
}

unsigned get_modifiers_from_qt_mouse_event(QMouseEvent const& event)
{
    unsigned modifiers = 0;
    if (event.modifiers() & Qt::Modifier::ALT)
        modifiers |= 1;
    if (event.modifiers() & Qt::Modifier::CTRL)
        modifiers |= 2;
    if (event.modifiers() & Qt::Modifier::SHIFT)
        modifiers |= 4;
    return modifiers;
}

unsigned get_modifiers_from_qt_keyboard_event(QKeyEvent const& event)
{
    auto modifiers = 0;
    if (event.modifiers().testFlag(Qt::AltModifier))
        modifiers |= KeyModifier::Mod_Alt;
    if (event.modifiers().testFlag(Qt::ControlModifier))
        modifiers |= KeyModifier::Mod_Ctrl;
    if (event.modifiers().testFlag(Qt::MetaModifier))
        modifiers |= KeyModifier::Mod_Super;
    if (event.modifiers().testFlag(Qt::ShiftModifier))
        modifiers |= KeyModifier::Mod_Shift;
    if (event.modifiers().testFlag(Qt::AltModifier))
        modifiers |= KeyModifier::Mod_AltGr;
    return modifiers;
}

KeyCode get_keycode_from_qt_keyboard_event(QKeyEvent const& event)
{
    struct Mapping {
        constexpr Mapping(Qt::Key q, KeyCode s)
            : qt_key(q)
            , serenity_key(s)
        {
        }

        Qt::Key qt_key;
        KeyCode serenity_key;
    };

    constexpr Mapping mappings[] = {
        { Qt::Key_0, Key_0 },
        { Qt::Key_1, Key_1 },
        { Qt::Key_2, Key_2 },
        { Qt::Key_3, Key_3 },
        { Qt::Key_4, Key_4 },
        { Qt::Key_5, Key_5 },
        { Qt::Key_6, Key_6 },
        { Qt::Key_7, Key_7 },
        { Qt::Key_8, Key_8 },
        { Qt::Key_9, Key_9 },
        { Qt::Key_A, Key_A },
        { Qt::Key_Alt, Key_Alt },
        { Qt::Key_Ampersand, Key_Ampersand },
        { Qt::Key_Apostrophe, Key_Apostrophe },
        { Qt::Key_AsciiCircum, Key_Circumflex },
        { Qt::Key_AsciiTilde, Key_Tilde },
        { Qt::Key_Asterisk, Key_Asterisk },
        { Qt::Key_At, Key_AtSign },
        { Qt::Key_B, Key_B },
        { Qt::Key_Backslash, Key_Backslash },
        { Qt::Key_Backspace, Key_Backspace },
        { Qt::Key_Bar, Key_Pipe },
        { Qt::Key_BraceLeft, Key_LeftBrace },
        { Qt::Key_BraceRight, Key_RightBrace },
        { Qt::Key_BracketLeft, Key_LeftBracket },
        { Qt::Key_BracketRight, Key_RightBracket },
        { Qt::Key_C, Key_C },
        { Qt::Key_CapsLock, Key_CapsLock },
        { Qt::Key_Colon, Key_Colon },
        { Qt::Key_Comma, Key_Comma },
        { Qt::Key_Control, Key_Control },
        { Qt::Key_D, Key_D },
        { Qt::Key_Delete, Key_Delete },
        { Qt::Key_Dollar, Key_Dollar },
        { Qt::Key_Down, Key_Down },
        { Qt::Key_E, Key_E },
        { Qt::Key_End, Key_End },
        { Qt::Key_Equal, Key_Equal },
        { Qt::Key_Escape, Key_Escape },
        { Qt::Key_exclamdown, Key_ExclamationPoint },
        { Qt::Key_F, Key_F },
        { Qt::Key_F1, Key_F1 },
        { Qt::Key_F10, Key_F10 },
        { Qt::Key_F11, Key_F11 },
        { Qt::Key_F12, Key_F12 },
        { Qt::Key_F2, Key_F2 },
        { Qt::Key_F3, Key_F3 },
        { Qt::Key_F4, Key_F4 },
        { Qt::Key_F5, Key_F5 },
        { Qt::Key_F6, Key_F6 },
        { Qt::Key_F7, Key_F7 },
        { Qt::Key_F8, Key_F8 },
        { Qt::Key_F9, Key_F9 },
        { Qt::Key_G, Key_G },
        { Qt::Key_Greater, Key_GreaterThan },
        { Qt::Key_H, Key_H },
        { Qt::Key_Home, Key_Home },
        { Qt::Key_I, Key_I },
        { Qt::Key_Insert, Key_Insert },
        { Qt::Key_J, Key_J },
        { Qt::Key_K, Key_K },
        { Qt::Key_L, Key_L },
        { Qt::Key_Left, Key_Left },
        { Qt::Key_Less, Key_LessThan },
        { Qt::Key_M, Key_M },
        { Qt::Key_Menu, Key_Menu },
        { Qt::Key_Minus, Key_Minus },
        { Qt::Key_N, Key_N },
        { Qt::Key_NumLock, Key_NumLock },
        { Qt::Key_O, Key_O },
        { Qt::Key_P, Key_P },
        { Qt::Key_PageDown, Key_PageDown },
        { Qt::Key_PageUp, Key_PageUp },
        { Qt::Key_ParenLeft, Key_LeftParen },
        { Qt::Key_ParenRight, Key_RightParen },
        { Qt::Key_Percent, Key_Percent },
        { Qt::Key_Period, Key_Period },
        { Qt::Key_Plus, Key_Plus },
        { Qt::Key_Print, Key_PrintScreen },
        { Qt::Key_Q, Key_Q },
        { Qt::Key_Question, Key_QuestionMark },
        { Qt::Key_QuoteDbl, Key_DoubleQuote },
        { Qt::Key_R, Key_R },
        { Qt::Key_Return, Key_Return },
        { Qt::Key_Right, Key_Right },
        { Qt::Key_S, Key_S },
        { Qt::Key_ScrollLock, Key_ScrollLock },
        { Qt::Key_Semicolon, Key_Semicolon },
        { Qt::Key_Shift, Key_LeftShift },
        { Qt::Key_Slash, Key_Slash },
        { Qt::Key_Space, Key_Space },
        { Qt::Key_Super_L, Key_Super },
        { Qt::Key_SysReq, Key_SysRq },
        { Qt::Key_T, Key_T },
        { Qt::Key_Tab, Key_Tab },
        { Qt::Key_U, Key_U },
        { Qt::Key_Underscore, Key_Underscore },
        { Qt::Key_Up, Key_Up },
        { Qt::Key_V, Key_V },
        { Qt::Key_W, Key_W },
        { Qt::Key_X, Key_X },
        { Qt::Key_Y, Key_Y },
        { Qt::Key_Z, Key_Z },
    };

    for (auto const& mapping : mappings) {
        if (event.key() == mapping.qt_key)
            return mapping.serenity_key;
    }
    return Key_Invalid;
}

void WebContentView::mouseMoveEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto buttons = get_buttons_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    client().async_mouse_move(to_content(position), 0, buttons, modifiers);
}

void WebContentView::mousePressEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    if (button == 0) {
        // We could not convert Qt buttons to something that Lagom can
        // recognize - don't even bother propagating this to the web engine
        // as it will not handle it anyway, and it will (currently) assert
        return;
    }
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    auto buttons = get_buttons_from_qt_event(*event);
    client().async_mouse_down(to_content(position), button, buttons, modifiers);
}

void WebContentView::mouseReleaseEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);

    if (event->button() & Qt::MouseButton::BackButton) {
        emit back_mouse_button();
    } else if (event->button() & Qt::MouseButton::ForwardButton) {
        emit forward_mouse_button();
    }

    if (button == 0) {
        // We could not convert Qt buttons to something that Lagom can
        // recognize - don't even bother propagating this to the web engine
        // as it will not handle it anyway, and it will (currently) assert
        return;
    }
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    auto buttons = get_buttons_from_qt_event(*event);
    client().async_mouse_up(to_content(position), button, buttons, modifiers);
}

void WebContentView::keyPressEvent(QKeyEvent* event)
{
    switch (event->key()) {
    case Qt::Key_Left:
    case Qt::Key_Right:
    case Qt::Key_Up:
    case Qt::Key_Down:
    case Qt::Key_PageUp:
    case Qt::Key_PageDown:
        QAbstractScrollArea::keyPressEvent(event);
        break;
    default:
        break;
    }

    if (event->key() == Qt::Key_Backtab) {
        // NOTE: Qt transforms Shift+Tab into a "Backtab", so we undo that transformation here.
        client().async_key_down(KeyCode::Key_Tab, Mod_Shift, '\t');
        return;
    }

    auto text = event->text();
    if (text.isEmpty()) {
        return;
    }
    auto point = event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    client().async_key_down(keycode, modifiers, point);
}

void WebContentView::keyReleaseEvent(QKeyEvent* event)
{
    auto text = event->text();
    if (text.isEmpty()) {
        return;
    }
    auto point = event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    client().async_key_up(keycode, modifiers, point);
}

void WebContentView::focusInEvent(QFocusEvent*)
{
    client().async_set_has_focus(true);
}

void WebContentView::focusOutEvent(QFocusEvent*)
{
    client().async_set_has_focus(false);
}

Gfx::IntPoint WebContentView::to_content(Gfx::IntPoint viewport_position) const
{
    return viewport_position.translated(horizontalScrollBar()->value(), verticalScrollBar()->value());
}

Gfx::IntPoint WebContentView::to_widget(Gfx::IntPoint content_position) const
{
    return content_position.translated(-horizontalScrollBar()->value(), -verticalScrollBar()->value());
}

void WebContentView::paintEvent(QPaintEvent*)
{
    QPainter painter(viewport());
    painter.scale(m_inverse_pixel_scaling_ratio, m_inverse_pixel_scaling_ratio);

    if (auto* bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.bitmap.ptr() : m_backup_bitmap.ptr()) {
        QImage q_image(bitmap->scanline_u8(0), bitmap->width(), bitmap->height(), QImage::Format_RGB32);
        painter.drawImage(QPoint(0, 0), q_image);
        return;
    }

    painter.fillRect(rect(), palette().base());
}

void WebContentView::resizeEvent(QResizeEvent* event)
{
    QAbstractScrollArea::resizeEvent(event);
    handle_resize();
}

void WebContentView::handle_resize()
{
    update_viewport_rect();

    if (m_client_state.has_usable_bitmap) {
        // NOTE: We keep the outgoing front bitmap as a backup so we have something to paint until we get a new one.
        m_backup_bitmap = m_client_state.front_bitmap.bitmap;
    }

    if (m_client_state.front_bitmap.bitmap)
        client().async_remove_backing_store(m_client_state.front_bitmap.id);

    if (m_client_state.back_bitmap.bitmap)
        client().async_remove_backing_store(m_client_state.back_bitmap.id);

    m_client_state.front_bitmap = {};
    m_client_state.back_bitmap = {};
    m_client_state.has_usable_bitmap = false;

    auto available_size = m_viewport_rect.size();

    if (available_size.is_empty())
        return;

    if (auto new_bitmap_or_error = Gfx::Bitmap::try_create_shareable(Gfx::BitmapFormat::BGRx8888, available_size); !new_bitmap_or_error.is_error()) {
        m_client_state.front_bitmap.bitmap = new_bitmap_or_error.release_value();
        m_client_state.front_bitmap.id = m_client_state.next_bitmap_id++;
        client().async_add_backing_store(m_client_state.front_bitmap.id, m_client_state.front_bitmap.bitmap->to_shareable_bitmap());
    }

    if (auto new_bitmap_or_error = Gfx::Bitmap::try_create_shareable(Gfx::BitmapFormat::BGRx8888, available_size); !new_bitmap_or_error.is_error()) {
        m_client_state.back_bitmap.bitmap = new_bitmap_or_error.release_value();
        m_client_state.back_bitmap.id = m_client_state.next_bitmap_id++;
        client().async_add_backing_store(m_client_state.back_bitmap.id, m_client_state.back_bitmap.bitmap->to_shareable_bitmap());
    }

    request_repaint();
}

void WebContentView::update_viewport_rect()
{
    auto scaled_width = int(viewport()->width() / m_inverse_pixel_scaling_ratio);
    auto scaled_height = int(viewport()->height() / m_inverse_pixel_scaling_ratio);
    Gfx::IntRect rect(horizontalScrollBar()->value(), verticalScrollBar()->value(), scaled_width, scaled_height);

    m_viewport_rect = rect;
    client().async_set_viewport_rect(rect);

    request_repaint();
}

void WebContentView::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    client().async_debug_request(request, argument);
}

void WebContentView::run_javascript(DeprecatedString const& js_source)
{
    client().async_run_javascript(js_source);
}

void WebContentView::did_output_js_console_message(i32 message_index)
{
    if (m_console_widget)
        m_console_widget->notify_about_new_console_message(message_index);
}

void WebContentView::did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> message_types, Vector<DeprecatedString> messages)
{
    if (m_console_widget)
        m_console_widget->handle_console_messages(start_index, message_types, messages);
}

void WebContentView::ensure_js_console_widget()
{
    if (!m_console_widget) {
        m_console_widget = new Ladybird::ConsoleWidget;
        m_console_widget->setWindowTitle("JS Console");
        m_console_widget->resize(640, 480);
        m_console_widget->on_js_input = [this](auto js_source) {
            client().async_js_console_input(js_source);
        };
        m_console_widget->on_request_messages = [this](i32 start_index) {
            client().async_js_console_request_messages(start_index);
        };
    }
}

void WebContentView::show_js_console()
{
    ensure_js_console_widget();
    m_console_widget->show();
}

void WebContentView::ensure_inspector_widget()
{
    if (m_inspector_widget)
        return;
    m_inspector_widget = new Ladybird::InspectorWidget;
    m_inspector_widget->setWindowTitle("Inspector");
    m_inspector_widget->resize(640, 480);
    m_inspector_widget->on_close = [this] {
        clear_inspected_dom_node();
    };

    m_inspector_widget->on_dom_node_inspected = [&](auto id, auto pseudo_element) {
        return inspect_dom_node(id, pseudo_element);
    };
}

void WebContentView::close_sub_widgets()
{
    auto close_widget_window = [](auto* widget) {
        if (widget)
            widget->close();
    };
    close_widget_window(m_console_widget);
    close_widget_window(m_inspector_widget);
}

bool WebContentView::is_inspector_open() const
{
    return m_inspector_widget && m_inspector_widget->isVisible();
}

void WebContentView::inspect_dom_tree()
{
    client().async_inspect_dom_tree();
}

ErrorOr<Ladybird::DOMNodeProperties> WebContentView::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element)
{
    auto response = client().inspect_dom_node(node_id, pseudo_element);
    if (!response.has_style())
        return Error::from_string_view("Inspected node returned no style"sv);
    return Ladybird::DOMNodeProperties {
        .computed_style_json = TRY(String::from_deprecated_string(response.take_computed_style())),
        .resolved_style_json = TRY(String::from_deprecated_string(response.take_resolved_style())),
        .custom_properties_json = TRY(String::from_deprecated_string(response.take_custom_properties())),
    };
}

void WebContentView::clear_inspected_dom_node()
{
    (void)inspect_dom_node(0, {});
}

void WebContentView::show_inspector()
{
    ensure_inspector_widget();
    m_inspector_widget->show();
    inspect_dom_tree();
}

void WebContentView::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    client().async_set_preferred_color_scheme(color_scheme);
}

void WebContentView::zoom_in()
{
    if (m_zoom_level >= ZOOM_MAX_LEVEL)
        return;
    m_zoom_level += ZOOM_STEP;
    update_zoom();
}

void WebContentView::zoom_out()
{
    if (m_zoom_level <= ZOOM_MIN_LEVEL)
        return;
    m_zoom_level -= ZOOM_STEP;
    update_zoom();
}

void WebContentView::reset_zoom()
{
    m_zoom_level = 1.0f;
    update_zoom();
}

void WebContentView::update_zoom()
{
    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio * m_zoom_level);
    update_viewport_rect();
    request_repaint();
}

void WebContentView::showEvent(QShowEvent* event)
{
    QAbstractScrollArea::showEvent(event);
    client().async_set_system_visibility_state(true);
}

void WebContentView::hideEvent(QHideEvent* event)
{
    QAbstractScrollArea::hideEvent(event);
    client().async_set_system_visibility_state(false);
}

WebContentClient& WebContentView::client()
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

void WebContentView::create_client()
{
    m_client_state = {};

    int socket_fds[2] {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int wc_fd_passing_fd = fd_passing_socket_fds[1];

    auto child_pid = fork();
    if (!child_pid) {
        MUST(Core::System::close(ui_fd_passing_fd));
        MUST(Core::System::close(ui_fd));

        auto takeover_string = DeprecatedString::formatted("WebContent:{}", wc_fd);
        MUST(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto webcontent_fd_passing_socket_string = DeprecatedString::number(wc_fd_passing_fd);

        Vector<StringView> arguments {
            "WebContent"sv,
            "--webcontent-fd-passing-socket"sv,
            webcontent_fd_passing_socket_string
        };

        if (!m_webdriver_content_ipc_path.is_empty()) {
            arguments.append("--webdriver-content-path"sv);
            arguments.append(m_webdriver_content_ipc_path);
        }

        auto result = Core::System::exec("./WebContent/WebContent"sv, arguments, Core::System::SearchInPath::Yes);
        if (result.is_error()) {
            auto web_content_path = ak_deprecated_string_from_qstring(QCoreApplication::applicationDirPath() + "/WebContent");
            result = Core::System::exec(web_content_path, arguments, Core::System::SearchInPath::Yes);
        }

        if (result.is_error())
            warnln("Could not launch WebContent: {}", result.error());
        VERIFY_NOT_REACHED();
    }

    MUST(Core::System::close(wc_fd_passing_fd));
    MUST(Core::System::close(wc_fd));

    auto socket = MUST(Core::Stream::LocalSocket::adopt_fd(ui_fd));
    MUST(socket->set_blocking(true));

    auto new_client = MUST(adopt_nonnull_ref_or_enomem(new (nothrow) WebView::WebContentClient(std::move(socket), *this)));
    new_client->set_fd_passing_socket(MUST(Core::Stream::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    auto* notifier = new QSocketNotifier(new_client->socket().fd().value(), QSocketNotifier::Type::Read);
    QObject::connect(notifier, &QSocketNotifier::activated, [new_client = new_client.ptr()] {
        if (auto notifier = new_client->socket().notifier())
            notifier->on_ready_to_read();
    });

    struct DeferredInvokerQt final : IPC::DeferredInvoker {
        virtual ~DeferredInvokerQt() = default;
        virtual void schedule(Function<void()> callback) override
        {
            QTimer::singleShot(0, std::move(callback));
        }
    };

    new_client->set_deferred_invoker(make<DeferredInvokerQt>());

    m_client_state.client = new_client;
    m_client_state.client->on_web_content_process_crash = [this] {
        QTimer::singleShot(0, [this] {
            handle_web_content_process_crash();
        });
    };

    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio);
    client().async_update_system_theme(MUST(Gfx::load_system_theme(DeprecatedString::formatted("{}/res/themes/Default.ini", s_serenity_resource_root))));
    client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());

    // FIXME: Get the screen rect.
    // client().async_update_screen_rects(GUI::Desktop::the().rects(), GUI::Desktop::the().main_screen_index());
}

void WebContentView::handle_web_content_process_crash()
{
    dbgln("WebContent process crashed!");
    create_client();
    VERIFY(m_client_state.client);

    // Don't keep a stale backup bitmap around.
    m_backup_bitmap = nullptr;

    handle_resize();
    StringBuilder builder;
    builder.append("<html><head><title>Crashed: "sv);
    builder.append(escape_html_entities(m_url.to_deprecated_string()));
    builder.append("</title></head><body>"sv);
    builder.append("<h1>Web page crashed"sv);
    if (!m_url.host().is_empty()) {
        builder.appendff(" on {}", escape_html_entities(m_url.host()));
    }
    builder.append("</h1>"sv);
    auto escaped_url = escape_html_entities(m_url.to_deprecated_string());
    builder.appendff("The web page <a href=\"{}\">{}</a> has crashed.<br><br>You can reload the page to try again.", escaped_url, escaped_url);
    builder.append("</body></html>"sv);
    load_html(builder.to_deprecated_string(), m_url);
}

void WebContentView::notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id)
{
    if (m_client_state.back_bitmap.id == bitmap_id) {
        m_client_state.has_usable_bitmap = true;
        m_client_state.back_bitmap.pending_paints--;
        swap(m_client_state.back_bitmap, m_client_state.front_bitmap);
        // We don't need the backup bitmap anymore, so drop it.
        m_backup_bitmap = nullptr;
        viewport()->update();

        if (m_client_state.got_repaint_requests_while_painting) {
            m_client_state.got_repaint_requests_while_painting = false;
            request_repaint();
        }
    }
}

void WebContentView::notify_server_did_invalidate_content_rect(Badge<WebContentClient>, [[maybe_unused]] Gfx::IntRect const& content_rect)
{
    request_repaint();
}

void WebContentView::notify_server_did_change_selection(Badge<WebContentClient>)
{
    request_repaint();
}

void WebContentView::notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor)
{
    switch (cursor) {
    case Gfx::StandardCursor::Hand:
        setCursor(Qt::PointingHandCursor);
        break;
    case Gfx::StandardCursor::IBeam:
        setCursor(Qt::IBeamCursor);
        break;
    case Gfx::StandardCursor::Arrow:
    default:
        setCursor(Qt::ArrowCursor);
        break;
    }
}

void WebContentView::notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize content_size)
{
    verticalScrollBar()->setMinimum(0);
    verticalScrollBar()->setMaximum(content_size.height() - m_viewport_rect.height());
    verticalScrollBar()->setPageStep(m_viewport_rect.height());
    horizontalScrollBar()->setMinimum(0);
    horizontalScrollBar()->setMaximum(content_size.width() - m_viewport_rect.width());
    horizontalScrollBar()->setPageStep(m_viewport_rect.width());
}

void WebContentView::notify_server_did_change_title(Badge<WebContentClient>, DeprecatedString const& title)
{
    emit title_changed(qstring_from_ak_deprecated_string(title));
}

void WebContentView::notify_server_did_request_scroll(Badge<WebContentClient>, i32 x_delta, i32 y_delta)
{
    horizontalScrollBar()->setValue(horizontalScrollBar()->value() + x_delta);
    verticalScrollBar()->setValue(verticalScrollBar()->value() + y_delta);
}

void WebContentView::notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint scroll_position)
{
    horizontalScrollBar()->setValue(scroll_position.x());
    verticalScrollBar()->setValue(scroll_position.y());
}

void WebContentView::notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const& rect)
{
    if (m_viewport_rect.contains(rect))
        return;

    if (rect.top() < m_viewport_rect.top()) {
        verticalScrollBar()->setValue(rect.top());
    } else if (rect.top() > m_viewport_rect.top() && rect.bottom() > m_viewport_rect.bottom()) {
        verticalScrollBar()->setValue(rect.bottom() - m_viewport_rect.height() + 1);
    }
}

void WebContentView::notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint content_position, DeprecatedString const& tooltip)
{
    auto widget_position = to_widget(content_position);
    QToolTip::showText(
        mapToGlobal(QPoint(widget_position.x(), widget_position.y())),
        qstring_from_ak_deprecated_string(tooltip),
        this);
}

void WebContentView::notify_server_did_leave_tooltip_area(Badge<WebContentClient>)
{
    QToolTip::hideText();
}

void WebContentView::notify_server_did_hover_link(Badge<WebContentClient>, AK::URL const& url)
{
    emit link_hovered(qstring_from_ak_deprecated_string(url.to_deprecated_string()));
}

void WebContentView::notify_server_did_unhover_link(Badge<WebContentClient>)
{
    emit link_unhovered();
}

void WebContentView::notify_server_did_click_link(Badge<WebContentClient>, AK::URL const& url, DeprecatedString const& target, unsigned int modifiers)
{
    // FIXME
    (void)url;
    (void)target;
    (void)modifiers;
    // if (on_link_click)
    // on_link_click(url, target, modifiers);
}

void WebContentView::notify_server_did_middle_click_link(Badge<WebContentClient>, AK::URL const& url, DeprecatedString const& target, unsigned int modifiers)
{
    (void)url;
    (void)target;
    (void)modifiers;
}

void WebContentView::notify_server_did_start_loading(Badge<WebContentClient>, AK::URL const& url, bool is_redirect)
{
    m_url = url;
    emit load_started(url, is_redirect);
    if (m_inspector_widget)
        m_inspector_widget->clear_dom_json();
}

void WebContentView::notify_server_did_finish_loading(Badge<WebContentClient>, AK::URL const& url)
{
    m_url = url;
    if (is_inspector_open())
        inspect_dom_tree();
}

void WebContentView::notify_server_did_request_navigate_back(Badge<WebContentClient>)
{
    emit navigate_back();
}

void WebContentView::notify_server_did_request_navigate_forward(Badge<WebContentClient>)
{
    emit navigate_forward();
}

void WebContentView::notify_server_did_request_refresh(Badge<WebContentClient>)
{
    emit refresh();
}

void WebContentView::notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint content_position)
{
    // FIXME
    (void)content_position;
}

void WebContentView::notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const&, unsigned)
{
    // FIXME
    (void)content_position;
    (void)url;
}

void WebContentView::notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const&, unsigned, Gfx::ShareableBitmap const& bitmap)
{
    // FIXME
    (void)content_position;
    (void)url;
    (void)bitmap;
}

void WebContentView::notify_server_did_request_alert(Badge<WebContentClient>, DeprecatedString const& message)
{
    m_dialog = new QMessageBox(QMessageBox::Icon::Warning, "Ladybird", qstring_from_ak_deprecated_string(message), QMessageBox::StandardButton::Ok, this);
    m_dialog->exec();

    client().async_alert_closed();
    m_dialog = nullptr;
}

void WebContentView::notify_server_did_request_confirm(Badge<WebContentClient>, DeprecatedString const& message)
{
    m_dialog = new QMessageBox(QMessageBox::Icon::Question, "Ladybird", qstring_from_ak_deprecated_string(message), QMessageBox::StandardButton::Ok | QMessageBox::StandardButton::Cancel, this);
    auto result = m_dialog->exec();

    client().async_confirm_closed(result == QMessageBox::StandardButton::Ok || result == QDialog::Accepted);
    m_dialog = nullptr;
}

void WebContentView::notify_server_did_request_prompt(Badge<WebContentClient>, DeprecatedString const& message, DeprecatedString const& default_)
{
    m_dialog = new QInputDialog(this);
    auto& dialog = static_cast<QInputDialog&>(*m_dialog);

    dialog.setWindowTitle("Ladybird");
    dialog.setLabelText(qstring_from_ak_deprecated_string(message));
    dialog.setTextValue(qstring_from_ak_deprecated_string(default_));

    if (dialog.exec() == QDialog::Accepted)
        client().async_prompt_closed(ak_deprecated_string_from_qstring(dialog.textValue()));
    else
        client().async_prompt_closed({});

    m_dialog = nullptr;
}

void WebContentView::notify_server_did_request_set_prompt_text(Badge<WebContentClient>, DeprecatedString const& message)
{
    if (m_dialog && is<QInputDialog>(*m_dialog))
        static_cast<QInputDialog&>(*m_dialog).setTextValue(qstring_from_ak_deprecated_string(message));
}

void WebContentView::notify_server_did_request_accept_dialog(Badge<WebContentClient>)
{
    if (m_dialog)
        m_dialog->accept();
}

void WebContentView::notify_server_did_request_dismiss_dialog(Badge<WebContentClient>)
{
    if (m_dialog)
        m_dialog->reject();
}

void WebContentView::get_source()
{
    client().async_get_source();
}

void WebContentView::notify_server_did_get_source(AK::URL const& url, DeprecatedString const& source)
{
    emit got_source(url, qstring_from_ak_deprecated_string(source));
}

void WebContentView::notify_server_did_get_dom_tree(DeprecatedString const& dom_tree)
{
    if (on_get_dom_tree)
        on_get_dom_tree(dom_tree);
    if (m_inspector_widget)
        m_inspector_widget->set_dom_json(dom_tree);
}

void WebContentView::notify_server_did_get_dom_node_properties(i32 node_id, DeprecatedString const& specified_style, DeprecatedString const& computed_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing)
{
    if (on_get_dom_node_properties)
        on_get_dom_node_properties(node_id, specified_style, computed_style, custom_properties, node_box_sizing);
}

void WebContentView::notify_server_did_output_js_console_message(i32 message_index)
{
    if (m_console_widget)
        m_console_widget->notify_about_new_console_message(message_index);
}

void WebContentView::notify_server_did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)
{
    if (m_console_widget)
        m_console_widget->handle_console_messages(start_index, message_types, messages);
}

void WebContentView::notify_server_did_change_favicon(Gfx::Bitmap const& bitmap)
{
    auto qimage = QImage(bitmap.scanline_u8(0), bitmap.width(), bitmap.height(), QImage::Format_ARGB32);
    if (qimage.isNull())
        return;
    auto qpixmap = QPixmap::fromImage(qimage);
    if (qpixmap.isNull())
        return;
    emit favicon_changed(QIcon(qpixmap));
}

Vector<Web::Cookie::Cookie> WebContentView::notify_server_did_request_all_cookies(Badge<WebContentClient>, AK::URL const& url)
{
    if (on_get_all_cookies)
        return on_get_all_cookies(url);
    return {};
}

Optional<Web::Cookie::Cookie> WebContentView::notify_server_did_request_named_cookie(Badge<WebContentClient>, AK::URL const& url, DeprecatedString const& name)
{
    if (on_get_named_cookie)
        return on_get_named_cookie(url, name);
    return {};
}

DeprecatedString WebContentView::notify_server_did_request_cookie(Badge<WebContentClient>, AK::URL const& url, Web::Cookie::Source source)
{
    if (on_get_cookie)
        return on_get_cookie(url, source);
    return {};
}

void WebContentView::notify_server_did_set_cookie(Badge<WebContentClient>, AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    if (on_set_cookie)
        on_set_cookie(url, cookie, source);
}

void WebContentView::notify_server_did_update_cookie(Badge<WebContentClient>, Web::Cookie::Cookie const& cookie)
{
    if (on_update_cookie)
        on_update_cookie(cookie);
}

void WebContentView::notify_server_did_update_resource_count(i32 count_waiting)
{
    // FIXME
    (void)count_waiting;
}

void WebContentView::notify_server_did_request_restore_window()
{
    emit restore_window();
}

Gfx::IntPoint WebContentView::notify_server_did_request_reposition_window(Gfx::IntPoint position)
{
    return emit reposition_window(position);
}

Gfx::IntSize WebContentView::notify_server_did_request_resize_window(Gfx::IntSize size)
{
    return emit resize_window(size);
}

Gfx::IntRect WebContentView::notify_server_did_request_maximize_window()
{
    return emit maximize_window();
}

Gfx::IntRect WebContentView::notify_server_did_request_minimize_window()
{
    return emit minimize_window();
}

Gfx::IntRect WebContentView::notify_server_did_request_fullscreen_window()
{
    return emit fullscreen_window();
}

void WebContentView::notify_server_did_request_file(Badge<WebContentClient>, DeprecatedString const& path, i32 request_id)
{
    auto file = Core::Stream::File::open(path, Core::Stream::OpenMode::Read);
    if (file.is_error())
        client().async_handle_file_return(file.error().code(), {}, request_id);
    else
        client().async_handle_file_return(0, IPC::File(*file.value()), request_id);
}

void WebContentView::request_repaint()
{
    // If this widget was instantiated but not yet added to a window,
    // it won't have a back bitmap yet, so we can just skip repaint requests.
    if (!m_client_state.back_bitmap.bitmap)
        return;
    // Don't request a repaint until pending paint requests have finished.
    if (m_client_state.back_bitmap.pending_paints) {
        m_client_state.got_repaint_requests_while_painting = true;
        return;
    }
    m_client_state.back_bitmap.pending_paints++;
    client().async_paint(m_client_state.back_bitmap.bitmap->rect().translated(horizontalScrollBar()->value(), verticalScrollBar()->value()), m_client_state.back_bitmap.id);
}

bool WebContentView::event(QEvent* event)
{
    // NOTE: We have to implement event() manually as Qt's focus navigation mechanism
    //       eats all the Tab key presses by default.

    if (event->type() == QEvent::KeyPress) {
        keyPressEvent(static_cast<QKeyEvent*>(event));
        return true;
    }
    if (event->type() == QEvent::KeyRelease) {
        keyReleaseEvent(static_cast<QKeyEvent*>(event));
        return true;
    }
    return QAbstractScrollArea::event(event);
}

void WebContentView::notify_server_did_finish_handling_input_event(bool event_was_accepted)
{
    // FIXME: Currently Ladybird handles the keyboard shortcuts before passing the event to web content, so
    //        we don't need to do anything here. But we'll need to once we start asking web content first.
    (void)event_was_accepted;
}

void WebContentView::notify_server_did_get_accessibility_tree(DeprecatedString const&)
{
    dbgln("TODO: support accessibility tree in Ladybird");
}

DeprecatedString WebContentView::selected_text()
{
    return client().get_selected_text();
}

void WebContentView::select_all()
{
    client().async_select_all();
}
