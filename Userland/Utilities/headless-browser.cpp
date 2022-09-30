/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/IODevice.h>
#include <LibCore/MemoryStream.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibGemini/GeminiRequest.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Rect.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/HttpsJob.h>
#include <LibHTTP/Job.h>
#include <LibMain/Main.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWeb/Platform/FontPluginSerenity.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <LibWebSocket/WebSocket.h>

class HeadlessBrowserPageClient final : public Web::PageClient {
public:
    static NonnullOwnPtr<HeadlessBrowserPageClient> create()
    {
        return adopt_own(*new HeadlessBrowserPageClient());
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
        context.set_should_show_line_box_borders(false);
        context.set_viewport_rect(content_rect);
        context.set_has_focus(true);
        layout_root->paint_all_phases(context);
    }

    void setup_palette(Core::AnonymousBuffer theme_buffer)
    {
        m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    }

    void set_viewport_rect(Gfx::IntRect viewport_rect)
    {
        page().top_level_browsing_context().set_viewport_rect(viewport_rect);
    }

    void set_screen_rect(Gfx::IntRect screen_rect)
    {
        m_screen_rect = screen_rect;
    }

    // ^Web::PageClient
    virtual Gfx::Palette palette() const override
    {
        return Gfx::Palette(*m_palette_impl);
    }

    virtual Gfx::IntRect screen_rect() const override
    {
        return m_screen_rect;
    }

    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override
    {
        return m_preferred_color_scheme;
    }

    virtual void page_did_change_title(String const&) override
    {
    }

    virtual void page_did_start_loading(AK::URL const&) override
    {
    }

    virtual void page_did_finish_loading(AK::URL const&) override
    {
    }

    virtual void page_did_change_selection() override
    {
    }

    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override
    {
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

    virtual void page_did_hover_link(AK::URL const&) override
    {
    }

    virtual void page_did_unhover_link() override
    {
    }

    virtual void page_did_invalidate(Gfx::IntRect const&) override
    {
    }

    virtual void page_did_change_favicon(Gfx::Bitmap const&) override
    {
    }

    virtual void page_did_layout() override
    {
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

private:
    HeadlessBrowserPageClient()
        : m_page(make<Web::Page>(*this))
    {
    }

    NonnullOwnPtr<Web::Page> m_page;

    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Gfx::IntRect m_screen_rect { 0, 0, 800, 600 };
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };
};

class ImageCodecPluginHeadless : public Web::Platform::ImageCodecPlugin {
public:
    ImageCodecPluginHeadless() = default;
    virtual ~ImageCodecPluginHeadless() override = default;

    virtual Optional<Web::Platform::DecodedImage> decode_image(ReadonlyBytes data) override
    {
        auto decoder = Gfx::ImageDecoder::try_create(data);

        if (!decoder)
            return Web::Platform::DecodedImage { false, 0, Vector<Web::Platform::Frame> {} };

        if (!decoder->frame_count())
            return Web::Platform::DecodedImage { false, 0, Vector<Web::Platform::Frame> {} };

        Vector<Web::Platform::Frame> frames;
        for (size_t i = 0; i < decoder->frame_count(); ++i) {
            auto frame_or_error = decoder->frame(i);
            if (frame_or_error.is_error()) {
                frames.append({ {}, 0 });
            } else {
                auto frame = frame_or_error.release_value();
                frames.append({ move(frame.image), static_cast<size_t>(frame.duration) });
            }
        }

        return Web::Platform::DecodedImage {
            decoder->is_animated(),
            static_cast<u32>(decoder->loop_count()),
            frames,
        };
    }
};

static HashTable<RefPtr<Web::ResourceLoaderConnectorRequest>> s_all_requests;

class HeadlessRequestServer : public Web::ResourceLoaderConnector {
public:
    class HTTPHeadlessRequest
        : public Web::ResourceLoaderConnectorRequest
        , public Weakable<HTTPHeadlessRequest> {
    public:
        static ErrorOr<NonnullRefPtr<HTTPHeadlessRequest>> create(String const& method, AK::URL const& url, HashMap<String, String> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&)
        {
            auto stream_backing_buffer = TRY(ByteBuffer::create_uninitialized(1 * MiB));
            auto underlying_socket = TRY(Core::Stream::TCPSocket::connect(url.host(), url.port().value_or(80)));
            TRY(underlying_socket->set_blocking(false));
            auto socket = TRY(Core::Stream::BufferedSocket<Core::Stream::TCPSocket>::create(move(underlying_socket)));

            HTTP::HttpRequest request;
            if (method.equals_ignoring_case("head"sv))
                request.set_method(HTTP::HttpRequest::HEAD);
            else if (method.equals_ignoring_case("get"sv))
                request.set_method(HTTP::HttpRequest::GET);
            else if (method.equals_ignoring_case("post"sv))
                request.set_method(HTTP::HttpRequest::POST);
            else
                request.set_method(HTTP::HttpRequest::Invalid);
            request.set_url(move(url));
            request.set_headers(request_headers);
            request.set_body(TRY(ByteBuffer::copy(request_body)));

            return adopt_ref(*new HTTPHeadlessRequest(move(request), move(socket), move(stream_backing_buffer)));
        }

        virtual ~HTTPHeadlessRequest() override
        {
        }

        virtual void set_should_buffer_all_input(bool) override
        {
        }

        virtual bool stop() override
        {
            return false;
        }

        virtual void stream_into(Core::Stream::Stream&) override
        {
        }

    private:
        HTTPHeadlessRequest(HTTP::HttpRequest&& request, NonnullOwnPtr<Core::Stream::BufferedSocketBase> socket, ByteBuffer&& stream_backing_buffer)
            : m_stream_backing_buffer(move(stream_backing_buffer))
            , m_output_stream(Core::Stream::MemoryStream::construct(m_stream_backing_buffer.bytes()).release_value_but_fixme_should_propagate_errors())
            , m_socket(move(socket))
            , m_job(HTTP::Job::construct(move(request), *m_output_stream))
        {
            m_job->on_headers_received = [weak_this = make_weak_ptr()](auto& response_headers, auto response_code) mutable {
                if (auto strong_this = weak_this.strong_ref()) {
                    strong_this->m_response_code = response_code;
                    for (auto& header : response_headers) {
                        strong_this->m_response_headers.set(header.key, header.value);
                    }
                }
            };
            m_job->on_finish = [weak_this = make_weak_ptr()](bool success) mutable {
                Core::deferred_invoke([weak_this, success]() mutable {
                    if (auto strong_this = weak_this.strong_ref()) {
                        ReadonlyBytes response_bytes { strong_this->m_output_stream->bytes().data(), strong_this->m_output_stream->offset() };
                        auto response_buffer = ByteBuffer::copy(response_bytes).release_value_but_fixme_should_propagate_errors();
                        strong_this->on_buffered_request_finish(success, strong_this->m_output_stream->offset(), strong_this->m_response_headers, strong_this->m_response_code, response_buffer);
                    }
                });
            };
            m_job->start(*m_socket);
        }

        Optional<u32> m_response_code;
        ByteBuffer m_stream_backing_buffer;
        NonnullOwnPtr<Core::Stream::MemoryStream> m_output_stream;
        NonnullOwnPtr<Core::Stream::BufferedSocketBase> m_socket;
        NonnullRefPtr<HTTP::Job> m_job;
        HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;
    };

    class HTTPSHeadlessRequest
        : public Web::ResourceLoaderConnectorRequest
        , public Weakable<HTTPSHeadlessRequest> {
    public:
        static ErrorOr<NonnullRefPtr<HTTPSHeadlessRequest>> create(String const& method, AK::URL const& url, HashMap<String, String> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&)
        {
            auto stream_backing_buffer = TRY(ByteBuffer::create_uninitialized(1 * MiB));
            auto underlying_socket = TRY(TLS::TLSv12::connect(url.host(), url.port().value_or(443)));
            TRY(underlying_socket->set_blocking(false));
            auto socket = TRY(Core::Stream::BufferedSocket<TLS::TLSv12>::create(move(underlying_socket)));

            HTTP::HttpRequest request;
            if (method.equals_ignoring_case("head"sv))
                request.set_method(HTTP::HttpRequest::HEAD);
            else if (method.equals_ignoring_case("get"sv))
                request.set_method(HTTP::HttpRequest::GET);
            else if (method.equals_ignoring_case("post"sv))
                request.set_method(HTTP::HttpRequest::POST);
            else
                request.set_method(HTTP::HttpRequest::Invalid);
            request.set_url(move(url));
            request.set_headers(request_headers);
            request.set_body(TRY(ByteBuffer::copy(request_body)));

            return adopt_ref(*new HTTPSHeadlessRequest(move(request), move(socket), move(stream_backing_buffer)));
        }

        virtual ~HTTPSHeadlessRequest() override
        {
        }

        virtual void set_should_buffer_all_input(bool) override
        {
        }

        virtual bool stop() override
        {
            return false;
        }

        virtual void stream_into(Core::Stream::Stream&) override
        {
        }

    private:
        HTTPSHeadlessRequest(HTTP::HttpRequest&& request, NonnullOwnPtr<Core::Stream::BufferedSocketBase> socket, ByteBuffer&& stream_backing_buffer)
            : m_stream_backing_buffer(move(stream_backing_buffer))
            , m_output_stream(Core::Stream::MemoryStream::construct(m_stream_backing_buffer.bytes()).release_value_but_fixme_should_propagate_errors())
            , m_socket(move(socket))
            , m_job(HTTP::HttpsJob::construct(move(request), *m_output_stream))
        {
            m_job->on_headers_received = [weak_this = make_weak_ptr()](auto& response_headers, auto response_code) mutable {
                if (auto strong_this = weak_this.strong_ref()) {
                    strong_this->m_response_code = response_code;
                    for (auto& header : response_headers) {
                        strong_this->m_response_headers.set(header.key, header.value);
                    }
                }
            };
            m_job->on_finish = [weak_this = make_weak_ptr()](bool success) mutable {
                Core::deferred_invoke([weak_this, success]() mutable {
                    if (auto strong_this = weak_this.strong_ref()) {
                        ReadonlyBytes response_bytes { strong_this->m_output_stream->bytes().data(), strong_this->m_output_stream->offset() };
                        auto response_buffer = ByteBuffer::copy(response_bytes).release_value_but_fixme_should_propagate_errors();
                        strong_this->on_buffered_request_finish(success, strong_this->m_output_stream->offset(), strong_this->m_response_headers, strong_this->m_response_code, response_buffer);
                    }
                });
            };
            m_job->start(*m_socket);
        }

        Optional<u32> m_response_code;
        ByteBuffer m_stream_backing_buffer;
        NonnullOwnPtr<Core::Stream::MemoryStream> m_output_stream;
        NonnullOwnPtr<Core::Stream::BufferedSocketBase> m_socket;
        NonnullRefPtr<HTTP::HttpsJob> m_job;
        HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;
    };

    class GeminiHeadlessRequest
        : public Web::ResourceLoaderConnectorRequest
        , public Weakable<GeminiHeadlessRequest> {
    public:
        static ErrorOr<NonnullRefPtr<GeminiHeadlessRequest>> create(String const&, AK::URL const& url, HashMap<String, String> const&, ReadonlyBytes, Core::ProxyData const&)
        {
            auto stream_backing_buffer = TRY(ByteBuffer::create_uninitialized(1 * MiB));
            auto underlying_socket = TRY(Core::Stream::TCPSocket::connect(url.host(), url.port().value_or(80)));
            TRY(underlying_socket->set_blocking(false));
            auto socket = TRY(Core::Stream::BufferedSocket<Core::Stream::TCPSocket>::create(move(underlying_socket)));

            Gemini::GeminiRequest request;
            request.set_url(url);

            return adopt_ref(*new GeminiHeadlessRequest(move(request), move(socket), move(stream_backing_buffer)));
        }

        virtual ~GeminiHeadlessRequest() override
        {
        }

        virtual void set_should_buffer_all_input(bool) override
        {
        }

        virtual bool stop() override
        {
            return false;
        }

        virtual void stream_into(Core::Stream::Stream&) override
        {
        }

    private:
        GeminiHeadlessRequest(Gemini::GeminiRequest&& request, NonnullOwnPtr<Core::Stream::BufferedSocketBase> socket, ByteBuffer&& stream_backing_buffer)
            : m_stream_backing_buffer(move(stream_backing_buffer))
            , m_output_stream(Core::Stream::MemoryStream::construct(m_stream_backing_buffer.bytes()).release_value_but_fixme_should_propagate_errors())
            , m_socket(move(socket))
            , m_job(Gemini::Job::construct(move(request), *m_output_stream))
        {
            m_job->on_headers_received = [weak_this = make_weak_ptr()](auto& response_headers, auto response_code) mutable {
                if (auto strong_this = weak_this.strong_ref()) {
                    strong_this->m_response_code = response_code;
                    for (auto& header : response_headers) {
                        strong_this->m_response_headers.set(header.key, header.value);
                    }
                }
            };
            m_job->on_finish = [weak_this = make_weak_ptr()](bool success) mutable {
                Core::deferred_invoke([weak_this, success]() mutable {
                    if (auto strong_this = weak_this.strong_ref()) {
                        ReadonlyBytes response_bytes { strong_this->m_output_stream->bytes().data(), strong_this->m_output_stream->offset() };
                        auto response_buffer = ByteBuffer::copy(response_bytes).release_value_but_fixme_should_propagate_errors();
                        strong_this->on_buffered_request_finish(success, strong_this->m_output_stream->offset(), strong_this->m_response_headers, strong_this->m_response_code, response_buffer);
                    }
                });
            };
            m_job->start(*m_socket);
        }

        Optional<u32> m_response_code;
        ByteBuffer m_stream_backing_buffer;
        NonnullOwnPtr<Core::Stream::MemoryStream> m_output_stream;
        NonnullOwnPtr<Core::Stream::BufferedSocketBase> m_socket;
        NonnullRefPtr<Gemini::Job> m_job;
        HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;
    };

    static NonnullRefPtr<HeadlessRequestServer> create()
    {
        return adopt_ref(*new HeadlessRequestServer());
    }

    virtual ~HeadlessRequestServer() override { }

    virtual void prefetch_dns(AK::URL const&) override { }
    virtual void preconnect(AK::URL const&) override { }

    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(String const& method, AK::URL const& url, HashMap<String, String> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const& proxy) override
    {
        RefPtr<Web::ResourceLoaderConnectorRequest> request;
        if (url.scheme().equals_ignoring_case("http"sv)) {
            auto request_or_error = HTTPHeadlessRequest::create(method, url, request_headers, request_body, proxy);
            if (request_or_error.is_error())
                return {};
            request = request_or_error.release_value();
        }
        if (url.scheme().equals_ignoring_case("https"sv)) {
            auto request_or_error = HTTPSHeadlessRequest::create(method, url, request_headers, request_body, proxy);
            if (request_or_error.is_error())
                return {};
            request = request_or_error.release_value();
        }
        if (url.scheme().equals_ignoring_case("gemini"sv)) {
            auto request_or_error = GeminiHeadlessRequest::create(method, url, request_headers, request_body, proxy);
            if (request_or_error.is_error())
                return {};
            request = request_or_error.release_value();
        }
        if (request)
            s_all_requests.set(request);
        return request;
    }

private:
    HeadlessRequestServer() { }
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int take_screenshot_after = 1;
    StringView url;
    StringView resources_folder;
    StringView error_page_url;
    StringView ca_certs_path;

    Core::EventLoop event_loop;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("This utility runs the Browser in headless mode.");
    args_parser.add_option(take_screenshot_after, "Take a screenshot after [n] seconds (default: 1)", "screenshot", 's', "n");
    args_parser.add_option(resources_folder, "Path of the base resources folder (defaults to /res)", "resources", 'r', "resources-root-path");
    args_parser.add_option(error_page_url, "URL for the error page (defaults to file:///res/html/error.html)", "error-page", 'e', "error-page-url");
    args_parser.add_option(ca_certs_path, "The bundled ca certificates file", "certs", 'c', "ca-certs-path");
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::FontPlugin::install(*new Web::Platform::FontPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new ImageCodecPluginHeadless);
    Web::ResourceLoader::initialize(HeadlessRequestServer::create());
    Web::WebSockets::WebSocketClientManager::initialize(HeadlessWebSocketClientManager::create());

    if (!resources_folder.is_empty()) {
        Web::FrameLoader::set_default_favicon_path(LexicalPath::join(resources_folder, "icons/16x16/app-browser.png"sv).string());
        Gfx::FontDatabase::set_default_fonts_lookup_path(LexicalPath::join(resources_folder, "fonts"sv).string());
    }
    if (!ca_certs_path.is_empty()) {
        auto config_result = Core::ConfigFile::open(ca_certs_path);
        if (config_result.is_error()) {
            dbgln("Failed to load CA Certificates: {}", config_result.error());
        } else {
            auto config = config_result.release_value();
            DefaultRootCACertificates::the().reload_certificates(config);
        }
    }

    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_window_title_font_query("Katica 10 700 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    if (!error_page_url.is_empty())
        Web::FrameLoader::set_error_page_url(error_page_url);

    auto page_client = HeadlessBrowserPageClient::create();

    if (!resources_folder.is_empty())
        page_client->setup_palette(Gfx::load_system_theme(LexicalPath::join(resources_folder, "themes/Default.ini"sv).string()));
    else
        page_client->setup_palette(Gfx::load_system_theme("/res/themes/Default.ini"));

    dbgln("Loading {}", url);
    page_client->load(AK::URL(url));

    // FIXME: Allow passing these values as arguments
    page_client->set_viewport_rect({ 0, 0, 800, 600 });
    page_client->set_screen_rect({ 0, 0, 800, 600 });

    dbgln("Taking screenshot after {} seconds !", take_screenshot_after);
    auto timer = Core::Timer::create_single_shot(
        take_screenshot_after * 1000,
        [page_client = move(page_client)]() mutable {
            // FIXME: Allow passing the output path as argument
            String output_file_path = "output.png";
            dbgln("Saving to {}", output_file_path);

            if (Core::File::exists(output_file_path))
                [[maybe_unused]]
                auto ignored = Core::File::remove(output_file_path, Core::File::RecursionMode::Disallowed, true);
            auto output_file = MUST(Core::File::open(output_file_path, Core::OpenMode::WriteOnly));

            auto output_rect = page_client->screen_rect();
            auto output_bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, output_rect.size()));

            page_client->paint(output_rect, output_bitmap);

            auto image_buffer = Gfx::PNGWriter::encode(output_bitmap);
            output_file->write(image_buffer.data(), image_buffer.size());

            exit(0);
        });
    timer->start();

    return event_loop.exec();
}
