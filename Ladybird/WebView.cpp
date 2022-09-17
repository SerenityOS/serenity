/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "WebView.h"
#include "ConsoleClient.h"
#include "CookieJar.h"
#include "EventLoopPluginQt.h"
#include "FontPluginQt.h"
#include "ImageCodecPluginLadybird.h"
#include "RequestManagerQt.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
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
#include <LibGfx/Rect.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <LibWebSocket/WebSocket.h>
#include <QApplication>
#include <QCursor>
#include <QIcon>
#include <QLineEdit>
#include <QMessageBox>
#include <QMouseEvent>
#include <QPaintEvent>
#include <QPainter>
#include <QScrollBar>
#include <QTextEdit>
#include <QToolTip>
#include <QVBoxLayout>

AK::String akstring_from_qstring(QString const& qstring)
{
    return AK::String(qstring.toUtf8().data());
}

QString qstring_from_akstring(AK::String const& akstring)
{
    return QString::fromUtf8(akstring.characters(), akstring.length());
}

String s_serenity_resource_root;

class HeadlessBrowserPageClient final : public Web::PageClient {
public:
    static NonnullOwnPtr<HeadlessBrowserPageClient> create(WebView& view)
    {
        return adopt_own(*new HeadlessBrowserPageClient(view));
    }

    Web::Page& page() { return *m_page; }
    Web::Page const& page() const { return *m_page; }

    Web::Layout::InitialContainingBlock* layout_root()
    {
        auto* document = page().top_level_browsing_context().active_document();
        if (!document)
            return nullptr;
        return document->layout_node();
    }

    void load(AK::URL const& url)
    {
        if (!url.is_valid())
            return;

        page().load(url);
    }

    void paint(Gfx::IntRect const& content_rect, Gfx::Bitmap& target)
    {
        Gfx::Painter painter(target);

        if (auto* document = page().top_level_browsing_context().active_document())
            document->update_layout();

        painter.fill_rect({ {}, content_rect.size() }, palette().base());

        auto* layout_root = this->layout_root();
        if (!layout_root) {
            return;
        }

        Web::PaintContext context(painter, palette(), content_rect.top_left());
        context.set_should_show_line_box_borders(m_should_show_line_box_borders);
        context.set_viewport_rect(content_rect);
        context.set_has_focus(true);
        layout_root->paint_all_phases(context);
    }

    void setup_palette(Core::AnonymousBuffer theme_buffer)
    {
        m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    }

    void set_viewport_rect(Gfx::IntRect rect)
    {
        m_viewport_rect = rect;
        page().top_level_browsing_context().set_viewport_rect(rect);
    }

    // ^Web::PageClient
    virtual Gfx::Palette palette() const override
    {
        return Gfx::Palette(*m_palette_impl);
    }

    virtual Gfx::IntRect screen_rect() const override
    {
        // FIXME: Return the actual screen rect.
        return m_viewport_rect;
    }

    Gfx::IntRect viewport_rect() const
    {
        return m_viewport_rect;
    }

    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override
    {
        return m_preferred_color_scheme;
    }

    virtual void page_did_change_title(String const& title) override
    {
        emit m_view.title_changed(title.characters());
    }

    virtual void page_did_start_loading(AK::URL const& url) override
    {
        emit m_view.load_started(url);
    }

    virtual void page_did_finish_loading(AK::URL const&) override
    {
        initialize_js_console();
        m_console_client->send_messages(0);
    }

    void initialize_js_console()
    {
        auto* document = page().top_level_browsing_context().active_document();
        auto realm = document->realm().make_weak_ptr();
        if (m_realm && m_realm.ptr() == realm.ptr())
            return;

        m_realm = realm;

        auto& console_object = *document->realm().intrinsics().console_object();
        m_console_client = make<Ladybird::ConsoleClient>(console_object.console(), *realm, m_view);
        console_object.console().set_client(*m_console_client.ptr());
    }

    virtual void page_did_change_selection() override
    {
    }

    virtual void page_did_request_cursor_change(Gfx::StandardCursor cursor) override
    {
        switch (cursor) {
        case Gfx::StandardCursor::Hand:
            m_view.setCursor(Qt::PointingHandCursor);
            break;
        case Gfx::StandardCursor::IBeam:
            m_view.setCursor(Qt::IBeamCursor);
            break;
        case Gfx::StandardCursor::Arrow:
        default:
            m_view.setCursor(Qt::ArrowCursor);
            break;
        }
    }

    virtual void page_did_request_context_menu(Gfx::IntPoint const&) override
    {
    }

    virtual void page_did_request_link_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned) override
    {
    }

    virtual void page_did_request_image_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned, Gfx::Bitmap const*) override
    {
    }

    virtual void page_did_click_link(AK::URL const&, String const&, unsigned) override
    {
    }

    virtual void page_did_middle_click_link(AK::URL const&, String const&, unsigned) override
    {
    }

    virtual void page_did_enter_tooltip_area(Gfx::IntPoint const& content_position, String const& tooltip) override
    {
        auto widget_position = m_view.to_widget(content_position);
        QToolTip::showText(
            m_view.mapToGlobal(QPoint(widget_position.x(), widget_position.y())),
            qstring_from_akstring(tooltip),
            &m_view);
    }

    virtual void page_did_leave_tooltip_area() override
    {
        QToolTip::hideText();
    }

    virtual void page_did_hover_link(AK::URL const& url) override
    {
        emit m_view.link_hovered(url.to_string().characters());
    }

    virtual void page_did_unhover_link() override
    {
        emit m_view.link_unhovered();
    }

    virtual void page_did_invalidate(Gfx::IntRect const&) override
    {
        m_view.viewport()->update();
    }

    virtual void page_did_change_favicon(Gfx::Bitmap const& bitmap) override
    {
        auto qimage = QImage(bitmap.scanline_u8(0), bitmap.width(), bitmap.height(), QImage::Format_ARGB32);
        if (qimage.isNull())
            return;
        auto qpixmap = QPixmap::fromImage(qimage);
        if (qpixmap.isNull())
            return;
        emit m_view.favicon_changed(QIcon(qpixmap));
    }

    virtual void page_did_layout() override
    {
        auto* layout_root = this->layout_root();
        VERIFY(layout_root);
        Gfx::IntSize content_size;
        if (layout_root->paint_box()->has_overflow())
            content_size = enclosing_int_rect(layout_root->paint_box()->scrollable_overflow_rect().value()).size();
        else
            content_size = enclosing_int_rect(layout_root->paint_box()->absolute_rect()).size();

        m_view.verticalScrollBar()->setMaximum(content_size.height() - m_viewport_rect.height());
        m_view.verticalScrollBar()->setPageStep(m_viewport_rect.height());
        m_view.horizontalScrollBar()->setMaximum(content_size.width() - m_viewport_rect.width());
        m_view.horizontalScrollBar()->setPageStep(m_viewport_rect.width());
    }

    virtual void page_did_request_scroll_into_view(Gfx::IntRect const& rect) override
    {
        if (m_viewport_rect.contains(rect))
            return;

        if (rect.top() < m_viewport_rect.top()) {
            m_view.verticalScrollBar()->setValue(rect.top());
        } else if (rect.top() > m_viewport_rect.top() && rect.bottom() > m_viewport_rect.bottom()) {
            m_view.verticalScrollBar()->setValue(rect.bottom() - m_viewport_rect.height() + 1);
        }
    }

    virtual void page_did_request_alert(String const& message) override
    {
        QMessageBox::warning(&m_view, "Ladybird", qstring_from_akstring(message));
    }

    virtual bool page_did_request_confirm(String const& message) override
    {
        auto result = QMessageBox::question(&m_view, "Ladybird", qstring_from_akstring(message),
            QMessageBox::StandardButton::Ok | QMessageBox::StandardButton::Cancel);

        return result == QMessageBox::StandardButton::Ok;
    }

    virtual String page_did_request_prompt(String const&, String const&) override
    {
        return String::empty();
    }

    virtual String page_did_request_cookie(AK::URL const& url, Web::Cookie::Source source) override
    {
        return m_cookie_jar.get_cookie(url, source);
    }

    virtual void page_did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) override
    {
        m_cookie_jar.set_cookie(url, cookie, source);
    }

    void dump_cookies() const
    {
        m_cookie_jar.dump_cookies();
    }

    void request_file(NonnullRefPtr<Web::FileRequest>& request) override
    {
        auto const file = Core::System::open(request->path(), O_RDONLY);
        request->on_file_request_finish(file);
    }

    void set_should_show_line_box_borders(bool state) { m_should_show_line_box_borders = state; }

    HeadlessBrowserPageClient(WebView& view)
        : m_view(view)
        , m_page(make<Web::Page>(*this))
    {
    }

    WebView& m_view;
    NonnullOwnPtr<Web::Page> m_page;
    Browser::CookieJar m_cookie_jar;

    OwnPtr<Ladybird::ConsoleClient> m_console_client;
    WeakPtr<JS::Realm> m_realm;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Gfx::IntRect m_viewport_rect { 0, 0, 800, 600 };
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };
    bool m_should_show_line_box_borders { false };
};

WebView::WebView()
{
    setMouseTracking(true);

    m_page_client = HeadlessBrowserPageClient::create(*this);

    m_page_client->setup_palette(Gfx::load_system_theme(String::formatted("{}/res/themes/Default.ini", s_serenity_resource_root)));

    // FIXME: Allow passing these values as arguments
    m_page_client->set_viewport_rect({ 0, 0, 800, 600 });

    m_inverse_pixel_scaling_ratio = 1.0 / devicePixelRatio();

    verticalScrollBar()->setSingleStep(24);
    horizontalScrollBar()->setSingleStep(24);

    QObject::connect(verticalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });
    QObject::connect(horizontalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });
}

WebView::~WebView()
{
}

void WebView::reload()
{
    auto url = m_page_client->page().top_level_browsing_context().active_document()->url();
    m_page_client->load(url);
}

void WebView::load(String const& url)
{
    m_page_client->load(AK::URL(url));
}

unsigned get_button_from_qt_event(QMouseEvent const& event)
{
    if (event.button() == Qt::MouseButton::LeftButton)
        return 1;
    if (event.button() == Qt::MouseButton::RightButton)
        return 2;
    if (event.button() == Qt::MouseButton::MiddleButton)
        return 4;
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

void WebView::mouseMoveEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto buttons = get_buttons_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    m_page_client->page().handle_mousemove(to_content(position), buttons, modifiers);
}

void WebView::mousePressEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    m_page_client->page().handle_mousedown(to_content(position), button, modifiers);
}

void WebView::mouseReleaseEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    m_page_client->page().handle_mouseup(to_content(position), button, modifiers);
}

void WebView::keyPressEvent(QKeyEvent* event)
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

    auto text = event->text();
    if (text.isEmpty()) {
        return;
    }
    auto point = event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    m_page_client->page().handle_keydown(keycode, modifiers, point);
}

void WebView::keyReleaseEvent(QKeyEvent* event)
{
    auto text = event->text();
    if (text.isEmpty()) {
        return;
    }
    auto point = event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    m_page_client->page().handle_keyup(keycode, modifiers, point);
}

Gfx::IntPoint WebView::to_content(Gfx::IntPoint viewport_position) const
{
    return viewport_position.translated(horizontalScrollBar()->value(), verticalScrollBar()->value());
}

Gfx::IntPoint WebView::to_widget(Gfx::IntPoint content_position) const
{
    return content_position.translated(-horizontalScrollBar()->value(), -verticalScrollBar()->value());
}

void WebView::paintEvent(QPaintEvent* event)
{
    QPainter painter(viewport());
    painter.setClipRect(event->rect());

    painter.scale(m_inverse_pixel_scaling_ratio, m_inverse_pixel_scaling_ratio);

    auto output_rect = m_page_client->viewport_rect();
    output_rect.set_x(horizontalScrollBar()->value());
    output_rect.set_y(verticalScrollBar()->value());
    auto output_bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, output_rect.size()));

    m_page_client->paint(output_rect, output_bitmap);

    QImage q_image(output_bitmap->scanline_u8(0), output_bitmap->width(), output_bitmap->height(), QImage::Format_RGB32);
    painter.drawImage(QPoint(0, 0), q_image);
}

void WebView::resizeEvent(QResizeEvent* event)
{
    QAbstractScrollArea::resizeEvent(event);
    update_viewport_rect();
}

void WebView::update_viewport_rect()
{
    auto scaled_width = int(size().width() / m_inverse_pixel_scaling_ratio);
    auto scaled_height = int(size().height() / m_inverse_pixel_scaling_ratio);
    Gfx::IntRect rect(horizontalScrollBar()->value(), verticalScrollBar()->value(), scaled_width, scaled_height);
    m_page_client->set_viewport_rect(rect);
}

class HeadlessWebSocketClientManager : public Web::WebSockets::WebSocketClientManager {
public:
    class HeadlessWebSocket
        : public Web::WebSockets::WebSocketClientSocket
        , public Weakable<HeadlessWebSocket> {
    public:
        static NonnullRefPtr<HeadlessWebSocket> create(NonnullRefPtr<WebSocket::WebSocket> underlying_socket)
        {
            return adopt_ref(*new HeadlessWebSocket(move(underlying_socket)));
        }

        virtual ~HeadlessWebSocket() override
        {
        }

        virtual Web::WebSockets::WebSocket::ReadyState ready_state() override
        {
            switch (m_websocket->ready_state()) {
            case WebSocket::ReadyState::Connecting:
                return Web::WebSockets::WebSocket::ReadyState::Connecting;
            case WebSocket::ReadyState::Open:
                return Web::WebSockets::WebSocket::ReadyState::Open;
            case WebSocket::ReadyState::Closing:
                return Web::WebSockets::WebSocket::ReadyState::Closing;
            case WebSocket::ReadyState::Closed:
                return Web::WebSockets::WebSocket::ReadyState::Closed;
            }
            VERIFY_NOT_REACHED();
        }

        virtual void send(ByteBuffer binary_or_text_message, bool is_text) override
        {
            m_websocket->send(WebSocket::Message(binary_or_text_message, is_text));
        }

        virtual void send(StringView message) override
        {
            m_websocket->send(WebSocket::Message(message));
        }

        virtual void close(u16 code, String reason) override
        {
            m_websocket->close(code, reason);
        }

    private:
        HeadlessWebSocket(NonnullRefPtr<WebSocket::WebSocket> underlying_socket)
            : m_websocket(move(underlying_socket))
        {
            m_websocket->on_open = [weak_this = make_weak_ptr()] {
                if (auto strong_this = weak_this.strong_ref())
                    if (strong_this->on_open)
                        strong_this->on_open();
            };
            m_websocket->on_message = [weak_this = make_weak_ptr()](auto message) {
                if (auto strong_this = weak_this.strong_ref()) {
                    if (strong_this->on_message) {
                        strong_this->on_message(Web::WebSockets::WebSocketClientSocket::Message {
                            .data = move(message.data()),
                            .is_text = message.is_text(),
                        });
                    }
                }
            };
            m_websocket->on_error = [weak_this = make_weak_ptr()](auto error) {
                if (auto strong_this = weak_this.strong_ref()) {
                    if (strong_this->on_error) {
                        switch (error) {
                        case WebSocket::WebSocket::Error::CouldNotEstablishConnection:
                            strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::CouldNotEstablishConnection);
                            return;
                        case WebSocket::WebSocket::Error::ConnectionUpgradeFailed:
                            strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ConnectionUpgradeFailed);
                            return;
                        case WebSocket::WebSocket::Error::ServerClosedSocket:
                            strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ServerClosedSocket);
                            return;
                        }
                        VERIFY_NOT_REACHED();
                    }
                }
            };
            m_websocket->on_close = [weak_this = make_weak_ptr()](u16 code, String reason, bool was_clean) {
                if (auto strong_this = weak_this.strong_ref())
                    if (strong_this->on_close)
                        strong_this->on_close(code, move(reason), was_clean);
            };
        }

        NonnullRefPtr<WebSocket::WebSocket> m_websocket;
    };

    static NonnullRefPtr<HeadlessWebSocketClientManager> create()
    {
        return adopt_ref(*new HeadlessWebSocketClientManager());
    }

    virtual ~HeadlessWebSocketClientManager() override { }

    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> connect(AK::URL const& url, String const& origin) override
    {
        WebSocket::ConnectionInfo connection_info(url);
        connection_info.set_origin(origin);

        auto connection = HeadlessWebSocket::create(WebSocket::WebSocket::create(move(connection_info)));
        return connection;
    }

private:
    HeadlessWebSocketClientManager() { }
};

static void platform_init()
{
#ifdef AK_OS_ANDROID
    extern void android_platform_init();
    android_platform_init();
#else
    s_serenity_resource_root = [] {
        auto const* source_dir = getenv("SERENITY_SOURCE_DIR");
        if (source_dir) {
            return String::formatted("{}/Base", source_dir);
        }
        auto* home = getenv("XDG_CONFIG_HOME") ?: getenv("HOME");
        VERIFY(home);
        auto home_lagom = String::formatted("{}/.lagom", home);
        if (Core::File::is_directory(home_lagom))
            return home_lagom;
        auto app_dir = akstring_from_qstring(QApplication::applicationDirPath());
        return LexicalPath(app_dir).parent().append("share"sv).string();
    }();
#endif
}

void initialize_web_engine()
{
    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Ladybird::EventLoopPluginQt);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPluginLadybird);

    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(HeadlessWebSocketClientManager::create());

    Web::FrameLoader::set_default_favicon_path(String::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    Web::Platform::FontPlugin::install(*new Ladybird::FontPluginQt);

    Web::FrameLoader::set_error_page_url(String::formatted("file://{}/res/html/error.html", s_serenity_resource_root));
}

void WebView::debug_request(String const& request, String const& argument)
{
    auto& page = m_page_client->page();

    if (request == "dump-dom-tree") {
        if (auto* doc = page.top_level_browsing_context().active_document())
            Web::dump_tree(*doc);
    }

    if (request == "dump-layout-tree") {
        if (auto* doc = page.top_level_browsing_context().active_document()) {
            if (auto* icb = doc->layout_node())
                Web::dump_tree(*icb);
        }
    }

    if (request == "dump-stacking-context-tree") {
        if (auto* doc = page.top_level_browsing_context().active_document()) {
            if (auto* icb = doc->layout_node()) {
                if (auto* stacking_context = icb->paint_box()->stacking_context())
                    stacking_context->dump();
            }
        }
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page.top_level_browsing_context().active_document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                Web::dump_sheet(sheet);
            }
        }
    }

    if (request == "collect-garbage") {
        Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
    }

    if (request == "set-line-box-borders") {
        bool state = argument == "on";
        m_page_client->set_should_show_line_box_borders(state);
        page.top_level_browsing_context().set_needs_display(page.top_level_browsing_context().viewport_rect());
    }

    if (request == "clear-cache") {
        Web::ResourceLoader::the().clear_cache();
    }

    if (request == "spoof-user-agent") {
        Web::ResourceLoader::the().set_user_agent(argument);
    }

    if (request == "same-origin-policy") {
        page.set_same_origin_policy_enabled(argument == "on");
    }

    if (request == "scripting") {
        page.set_is_scripting_enabled(argument == "on");
    }

    if (request == "dump-local-storage") {
        if (auto* doc = page.top_level_browsing_context().active_document())
            doc->window().local_storage()->dump();
    }

    if (request == "dump-cookies"sv)
        m_page_client->dump_cookies();
}

String WebView::source() const
{
    auto* document = m_page_client->page().top_level_browsing_context().active_document();
    if (!document)
        return String::empty();
    return document->source();
}

void WebView::run_javascript(String const& js_source) const
{
    auto* active_document = const_cast<Web::DOM::Document*>(m_page_client->page().top_level_browsing_context().active_document());

    if (!active_document)
        return;

    // This is partially based on "execute a javascript: URL request" https://html.spec.whatwg.org/multipage/browsing-the-web.html#javascript-protocol

    // Let settings be browsingContext's active document's relevant settings object.
    auto& settings = active_document->relevant_settings_object();

    // Let baseURL be settings's API base URL.
    auto base_url = settings.api_base_url();

    // Let script be the result of creating a classic script given scriptSource, settings, baseURL, and the default classic script fetch options.
    // FIXME: This doesn't pass in "default classic script fetch options"
    // FIXME: What should the filename be here?
    auto script = Web::HTML::ClassicScript::create("(client connection run_javascript)", js_source, settings, base_url);

    // Let evaluationStatus be the result of running the classic script script.
    auto evaluation_status = script->run();

    if (evaluation_status.is_error())
        dbgln("Exception :(");
}

void WebView::did_output_js_console_message(i32 message_index)
{
    m_page_client->m_console_client->send_messages(message_index);
}

void WebView::did_get_js_console_messages(i32, Vector<String>, Vector<String> messages)
{
    if (!m_js_console_input_edit)
        return;
    for (auto& message : messages) {
        m_js_console_output_edit->append(qstring_from_akstring(message).trimmed());
    }
}

void WebView::show_js_console()
{
    if (!m_js_console_widget) {
        m_js_console_widget = new QWidget;
        m_js_console_widget->setWindowTitle("JS Console");
        auto* layout = new QVBoxLayout;
        m_js_console_widget->setLayout(layout);
        m_js_console_output_edit = new QTextEdit;
        m_js_console_output_edit->setReadOnly(true);
        m_js_console_input_edit = new QLineEdit;
        layout->addWidget(m_js_console_output_edit);
        layout->addWidget(m_js_console_input_edit);
        m_js_console_widget->resize(640, 480);

        QObject::connect(m_js_console_input_edit, &QLineEdit::returnPressed, [this] {
            auto code = m_js_console_input_edit->text().trimmed();
            m_js_console_input_edit->clear();

            m_js_console_output_edit->append(QString("> %1").arg(code));

            m_page_client->initialize_js_console();
            m_page_client->m_console_client->handle_input(akstring_from_qstring(code));
        });
    }
    m_js_console_widget->show();
    m_js_console_input_edit->setFocus();
}
