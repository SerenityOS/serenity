/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "WebView.h"
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
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
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
#include <QMouseEvent>
#include <QPaintEvent>
#include <QPainter>
#include <QScrollBar>
#include <stdlib.h>

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

    virtual void page_did_set_document_in_top_level_browsing_context(Web::DOM::Document*) override
    {
    }

    virtual void page_did_start_loading(AK::URL const& url) override
    {
        emit m_view.loadStarted(url);
    }

    virtual void page_did_finish_loading(AK::URL const&) override
    {
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
    }

    virtual void page_did_leave_tooltip_area() override
    {
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

    virtual void page_did_request_alert(String const&) override
    {
    }

    virtual bool page_did_request_confirm(String const&) override
    {
        return false;
    }

    virtual String page_did_request_prompt(String const&, String const&) override
    {
        return String::empty();
    }

    virtual String page_did_request_cookie(AK::URL const&, Web::Cookie::Source) override
    {
        return String::empty();
    }

    virtual void page_did_set_cookie(AK::URL const&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override
    {
    }

    void request_file(NonnullRefPtr<Web::FileRequest>& request) override
    {
        auto const file = Core::System::open(request->path(), O_RDONLY);
        request->on_file_request_finish(file);
    }

    void set_should_show_line_box_borders(bool state) { m_should_show_line_box_borders = state; }

private:
    HeadlessBrowserPageClient(WebView& view)
        : m_view(view)
        , m_page(make<Web::Page>(*this))
    {
    }

    WebView& m_view;
    NonnullOwnPtr<Web::Page> m_page;

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

unsigned get_modifiers_from_qt_event(QMouseEvent const& event)
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

void WebView::mouseMoveEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto buttons = get_buttons_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_event(*event);
    m_page_client->page().handle_mousemove(to_content(position), buttons, modifiers);
}

void WebView::mousePressEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_event(*event);
    m_page_client->page().handle_mousedown(to_content(position), button, modifiers);
}

void WebView::mouseReleaseEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_event(*event);
    m_page_client->page().handle_mouseup(to_content(position), button, modifiers);
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

class HeadlessImageDecoderClient : public Web::ImageDecoding::Decoder {
public:
    static NonnullRefPtr<HeadlessImageDecoderClient> create()
    {
        return adopt_ref(*new HeadlessImageDecoderClient());
    }

    virtual ~HeadlessImageDecoderClient() override = default;

    virtual Optional<Web::ImageDecoding::DecodedImage> decode_image(ReadonlyBytes data) override
    {
        auto decoder = Gfx::ImageDecoder::try_create(data);

        if (!decoder)
            return Web::ImageDecoding::DecodedImage { false, 0, Vector<Web::ImageDecoding::Frame> {} };

        if (!decoder->frame_count())
            return Web::ImageDecoding::DecodedImage { false, 0, Vector<Web::ImageDecoding::Frame> {} };

        Vector<Web::ImageDecoding::Frame> frames;
        for (size_t i = 0; i < decoder->frame_count(); ++i) {
            auto frame_or_error = decoder->frame(i);
            if (frame_or_error.is_error()) {
                frames.append({ {}, 0 });
            } else {
                auto frame = frame_or_error.release_value();
                frames.append({ move(frame.image), static_cast<size_t>(frame.duration) });
            }
        }

        return Web::ImageDecoding::DecodedImage {
            decoder->is_animated(),
            static_cast<u32>(decoder->loop_count()),
            frames,
        };
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

void initialize_web_engine()
{
    Web::ImageDecoding::Decoder::initialize(HeadlessImageDecoderClient::create());
    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(HeadlessWebSocketClientManager::create());

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
