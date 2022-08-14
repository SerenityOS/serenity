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
#include "RequestManagerQt.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
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
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Rect.h>
#include <LibJS/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/ImageDecoding.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <LibWebSocket/WebSocket.h>
#include <QCursor>
#include <QIcon>
#include <QLineEdit>
#include <QMessageBox>
#include <QMouseEvent>
#include <QPaintEvent>
#include <QPainter>
#include <QScrollBar>
#include <QTextEdit>
#include <QVBoxLayout>

AK::String akstring_from_qstring(QString const& qstring)
{
    return AK::String(qstring.toUtf8().data());
}

QString qstring_from_akstring(AK::String const& akstring)
{
    return QString::fromUtf8(akstring.characters(), akstring.length());
}

String s_serenity_resource_root = [] {
    auto const* source_dir = getenv("SERENITY_SOURCE_DIR");
    if (source_dir) {
        return String::formatted("{}/Base", source_dir);
    }
    auto* home = getenv("XDG_CONFIG_HOME") ?: getenv("HOME");
    VERIFY(home);
    return String::formatted("{}/.lagom", home);
}();

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
        emit m_view.loadStarted(url);
    }

    virtual void page_did_finish_loading(AK::URL const&) override
    {
        initialize_js_console();
        m_console_client->send_messages(0);
    }

    void initialize_js_console()
    {
        auto* document = page().top_level_browsing_context().active_document();
        auto interpreter = document->interpreter().make_weak_ptr();
        if (m_interpreter.ptr() == interpreter.ptr())
            return;

        m_interpreter = interpreter;
        m_console_client = make<Ladybird::ConsoleClient>(interpreter->global_object().console(), interpreter, m_view);
        interpreter->global_object().console().set_client(*m_console_client.ptr());
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

    virtual void page_did_enter_tooltip_area(Gfx::IntPoint const&, String const&) override
    {
        m_view.setCursor(Qt::IBeamCursor);
    }

    virtual void page_did_leave_tooltip_area() override
    {
        m_view.setCursor(Qt::ArrowCursor);
    }

    virtual void page_did_hover_link(AK::URL const& url) override
    {
        emit m_view.linkHovered(url.to_string().characters());
    }

    virtual void page_did_unhover_link() override
    {
        emit m_view.linkUnhovered();
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

    virtual void page_did_request_scroll_into_view(Gfx::IntRect const&) override
    {
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
    WeakPtr<JS::Interpreter> m_interpreter;
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
    auto key = (KeyCode)event.nativeScanCode();
    key = Key_Insert;

    switch (event.key()) {
    case Qt::Key_Escape:
        key = Key_Escape;
        break;
    case Qt::Key_Tab:
        key = Key_Tab;
        break;
    case Qt::Key_Backspace:
        key = Key_Backspace;
        break;
    case Qt::Key_Return:
        key = Key_Return;
        break;
    case Qt::Key_Insert:
        key = Key_Insert;
        break;
    case Qt::Key_Delete:
        key = Key_Delete;
        break;
    case Qt::Key_Print:
        key = Key_PrintScreen;
        break;
    case Qt::Key_SysReq:
        key = Key_SysRq;
        break;
    case Qt::Key_Home:
        key = Key_Home;
        break;
    case Qt::Key_End:
        key = Key_End;
        break;
    case Qt::Key_Left:
        key = Key_Left;
        break;
    case Qt::Key_Up:
        key = Key_Up;
        break;
    case Qt::Key_Right:
        key = Key_Right;
        break;
    case Qt::Key_Down:
        key = Key_Down;
        break;
    case Qt::Key_PageUp:
        key = Key_PageUp;
        break;
    case Qt::Key_PageDown:
        key = Key_PageDown;
        break;
    case Qt::Key_Shift:
        key = Key_LeftShift;
        break;
        // TODO: On Serenity/AK - we distinghuish the shift by the modifiers flag.
        //        case Qt::Key_Shift:
        //            key = Key_RightShift;
        //            break;
    case Qt::Key_Control:
        key = Key_Control;
        break;
    case Qt::Key_Alt:
        key = Key_Alt;
        break;
    case Qt::Key_CapsLock:
        key = Key_CapsLock;
        break;
    case Qt::Key_NumLock:
        key = Key_NumLock;
        break;
    case Qt::Key_ScrollLock:
        key = Key_ScrollLock;
        break;
    case Qt::Key_F1:
        key = Key_F1;
        break;
    case Qt::Key_F2:
        key = Key_F2;
        break;
    case Qt::Key_F3:
        key = Key_F3;
        break;
    case Qt::Key_F4:
        key = Key_F4;
        break;
    case Qt::Key_F5:
        key = Key_F5;
        break;
    case Qt::Key_F6:
        key = Key_F6;
        break;
    case Qt::Key_F7:
        key = Key_F7;
        break;
    case Qt::Key_F8:
        key = Key_F8;
        break;
    case Qt::Key_F9:
        key = Key_F9;
        break;
    case Qt::Key_F10:
        key = Key_F10;
        break;
    case Qt::Key_F11:
        key = Key_F11;
        break;
    case Qt::Key_F12:
        key = Key_F12;
        break;
    case Qt::Key_Space:
        key = Key_Space;
        break;
    case Qt::Key_exclamdown:
        key = Key_ExclamationPoint;
        break;
    case Qt::Key_QuoteDbl:
        key = Key_DoubleQuote;
        break;
        //        case Qt::Key_: ????
        //            key = Key_Hashtag;
        //            break;
    case Qt::Key_Dollar:
        key = Key_Dollar;
        break;
    case Qt::Key_Percent:
        key = Key_Percent;
        break;
    case Qt::Key_Ampersand:
        key = Key_Ampersand;
        break;
    case Qt::Key_Apostrophe:
        key = Key_Apostrophe;
        break;
    case Qt::Key_ParenLeft:
        key = Key_LeftParen;
        break;
    case Qt::Key_ParenRight:
        key = Key_RightParen;
        break;
    case Qt::Key_Asterisk:
        key = Key_Asterisk;
        break;
    case Qt::Key_Plus:
        key = Key_Plus;
        break;
    case Qt::Key_Comma:
        key = Key_Comma;
        break;
    case Qt::Key_Minus:
        key = Key_Minus;
        break;
    case Qt::Key_Period:
        key = Key_Period;
        break;
    case Qt::Key_Slash:
        key = Key_Slash;
        break;
    case Qt::Key_0:
        key = Key_0;
        break;
    case Qt::Key_1:
        key = Key_1;
        break;
    case Qt::Key_2:
        key = Key_2;
        break;
    case Qt::Key_3:
        key = Key_3;
        break;
    case Qt::Key_4:
        key = Key_4;
        break;
    case Qt::Key_5:
        key = Key_5;
        break;
    case Qt::Key_6:
        key = Key_6;
        break;
    case Qt::Key_7:
        key = Key_7;
        break;
    case Qt::Key_8:
        key = Key_8;
        break;
    case Qt::Key_9:
        key = Key_9;
        break;
    case Qt::Key_Colon:
        key = Key_Colon;
        break;
    case Qt::Key_Semicolon:
        key = Key_Semicolon;
        break;
    case Qt::Key_Less:
        key = Key_LessThan;
        break;
    case Qt::Key_Equal:
        key = Key_Equal;
        break;
    case Qt::Key_Greater:
        key = Key_GreaterThan;
        break;
    case Qt::Key_Question:
        key = Key_QuestionMark;
        break;
    case Qt::Key_At:
        key = Key_AtSign;
        break;
    case Qt::Key_A:
        key = Key_A;
        break;
    case Qt::Key_B:
        key = Key_B;
        break;
    case Qt::Key_C:
        key = Key_C;
        break;
    case Qt::Key_D:
        key = Key_D;
        break;
    case Qt::Key_E:
        key = Key_E;
        break;
    case Qt::Key_F:
        key = Key_F;
        break;
    case Qt::Key_G:
        key = Key_G;
        break;
    case Qt::Key_H:
        key = Key_H;
        break;
    case Qt::Key_I:
        key = Key_I;
        break;
    case Qt::Key_J:
        key = Key_J;
        break;
    case Qt::Key_K:
        key = Key_K;
        break;
    case Qt::Key_L:
        key = Key_L;
        break;
    case Qt::Key_M:
        key = Key_M;
        break;
    case Qt::Key_N:
        key = Key_N;
        break;
    case Qt::Key_O:
        key = Key_O;
        break;
    case Qt::Key_P:
        key = Key_P;
        break;
    case Qt::Key_Q:
        key = Key_Q;
        break;
    case Qt::Key_R:
        key = Key_R;
        break;
    case Qt::Key_S:
        key = Key_S;
        break;
    case Qt::Key_T:
        key = Key_T;
        break;
    case Qt::Key_U:
        key = Key_U;
        break;
    case Qt::Key_V:
        key = Key_V;
        break;
    case Qt::Key_W:
        key = Key_W;
        break;
    case Qt::Key_X:
        key = Key_X;
        break;
    case Qt::Key_Y:
        key = Key_Y;
        break;
    case Qt::Key_Z:
        key = Key_Z;
        break;
    case Qt::Key_BracketLeft:
        key = Key_LeftBracket;
        break;
    case Qt::Key_BracketRight:
        key = Key_RightBracket;
        break;
    case Qt::Key_Backslash:
        key = Key_Backslash;
        break;
        //        case Qt::Key_fk: ???
        //            key = Key_Circumflex;
        //            break;
    case Qt::Key_Underscore:
        key = Key_Underscore;
        break;
    case Qt::Key_BraceLeft:
        key = Key_LeftBrace;
        break;
    case Qt::Key_BraceRight:
        key = Key_RightBrace;
        break;
    case Qt::Key_Bar:
        key = Key_Pipe;
        break;
    case Qt::Key_AsciiTilde: //? Unsure about it
        key = Key_Tilde;
        break;
        //        case Qt::Key_AsciiTilde: ???
        //            key = Key_Backtick;
        //            break;
        // On serenity - super/meta R/L are distinguished by key modifiers
    case Qt::Key_Super_L:
        key = Key_Super;
        break;
    case Qt::Key_Menu:
        key = Key_Menu;
        break;
    }
    return key;
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
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    auto point = event->text()[0].unicode();
    //    dbgln(String::formatted("keycode={}, modifiers={}, point={}", (int32_t)keycode, modifiers, point));
    m_page_client->page().handle_keydown(keycode, modifiers, point);
}

void WebView::keyReleaseEvent(QKeyEvent* event)
{
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    auto point = event->text()[0].unicode();
    m_page_client->page().handle_keyup(keycode, modifiers, point);
}

Gfx::IntPoint WebView::to_content(Gfx::IntPoint viewport_position) const
{
    return viewport_position.translated(horizontalScrollBar()->value(), verticalScrollBar()->value());
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
    auto scaled_width = int(event->size().width() / m_inverse_pixel_scaling_ratio);
    auto scaled_height = int(event->size().height() / m_inverse_pixel_scaling_ratio);
    Gfx::IntRect rect(horizontalScrollBar()->value(), verticalScrollBar()->value(), scaled_width, scaled_height);
    if (verticalScrollBar()->isVisible())
        rect.set_width(rect.width() - verticalScrollBar()->width());
    if (horizontalScrollBar()->isVisible())
        rect.set_height(rect.height() - horizontalScrollBar()->height());
    m_page_client->set_viewport_rect(rect);
}

static Optional<Web::ImageDecoding::DecodedImage> decode_image_with_qt(ReadonlyBytes data)
{
    auto image = QImage::fromData(data.data(), static_cast<int>(data.size()));
    if (image.isNull())
        return {};
    image = image.convertToFormat(QImage::Format::Format_ARGB32);
    auto bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(image.width(), image.height())));
    for (int y = 0; y < image.height(); ++y) {
        memcpy(bitmap->scanline_u8(y), image.scanLine(y), image.width() * 4);
    }
    Vector<Web::ImageDecoding::Frame> frames;

    frames.append(Web::ImageDecoding::Frame {
        bitmap,
    });
    return Web::ImageDecoding::DecodedImage {
        false,
        0,
        move(frames),
    };
}

static Optional<Web::ImageDecoding::DecodedImage> decode_image_with_libgfx(ReadonlyBytes data)
{
    auto decoder = Gfx::ImageDecoder::try_create(data);

    if (!decoder || !decoder->frame_count()) {
        return {};
    }

    bool had_errors = false;
    Vector<Web::ImageDecoding::Frame> frames;
    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        auto frame_or_error = decoder->frame(i);
        if (frame_or_error.is_error()) {
            frames.append({ {}, 0 });
            had_errors = true;
        } else {
            auto frame = frame_or_error.release_value();
            frames.append({ move(frame.image), static_cast<size_t>(frame.duration) });
        }
    }

    if (had_errors)
        return {};

    return Web::ImageDecoding::DecodedImage {
        decoder->is_animated(),
        static_cast<u32>(decoder->loop_count()),
        move(frames),
    };
}

class HeadlessImageDecoderClient : public Web::ImageDecoding::Decoder {
public:
    static NonnullRefPtr<HeadlessImageDecoderClient> create()
    {
        return adopt_ref(*new HeadlessImageDecoderClient());
    }

    virtual ~HeadlessImageDecoderClient() override = default;

    virtual Optional<Web::ImageDecoding::DecodedImage> decode_image(ReadonlyBytes data) override
    {
        auto image = decode_image_with_libgfx(data);
        if (image.has_value())
            return image;
        return decode_image_with_qt(data);
    }

private:
    explicit HeadlessImageDecoderClient() = default;
};

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
#endif
}

void initialize_web_engine()
{
    Web::ImageDecoding::Decoder::initialize(HeadlessImageDecoderClient::create());
    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(HeadlessWebSocketClientManager::create());

    platform_init();

    Web::FrameLoader::set_default_favicon_path(String::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));
    dbgln("Set favicon path to {}", String::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    Gfx::FontDatabase::set_default_fonts_lookup_path(String::formatted("{}/res/fonts", s_serenity_resource_root));

    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

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

            m_page_client->m_console_client->handle_input(akstring_from_qstring(code));
        });
    }
    m_js_console_widget->show();
    m_js_console_input_edit->setFocus();
}
