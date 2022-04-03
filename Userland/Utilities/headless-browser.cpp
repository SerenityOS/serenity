/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Rect.h>
#include <LibMain/Main.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/ImageDecoding.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/WebSockets/WebSocket.h>

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

    void set_screen_rect(Gfx::IntRect screen_rect)
    {
        m_screen_rect = screen_rect;
        page().top_level_browsing_context().set_viewport_rect(screen_rect);
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

    virtual void page_did_set_document_in_top_level_browsing_context(Web::DOM::Document*) override
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

class HeadlessImageDecoderClient : public Web::ImageDecoding::Decoder {
public:
    static NonnullRefPtr<HeadlessImageDecoderClient> create()
    {
        return adopt_ref(*new HeadlessImageDecoderClient());
    }

    virtual ~HeadlessImageDecoderClient() override = default;

    virtual Optional<Web::ImageDecoding::DecodedImage> decode_image(ReadonlyBytes) override
    {
        return {};
    }

private:
    explicit HeadlessImageDecoderClient() = default;
};

class HeadlessRequestServer : public Web::ResourceLoaderConnector {
public:
    static NonnullRefPtr<HeadlessRequestServer> create()
    {
        return adopt_ref(*new HeadlessRequestServer());
    }

    virtual ~HeadlessRequestServer() override { }

    virtual void prefetch_dns(AK::URL const&) override { }
    virtual void preconnect(AK::URL const&) override { }

    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(String const&, AK::URL const&, HashMap<String, String> const&, ReadonlyBytes, Core::ProxyData const&) override
    {
        return {};
    }

private:
    HeadlessRequestServer() { }
};

class HeadlessWebSocketClientManager : public Web::WebSockets::WebSocketClientManager {
public:
    static NonnullRefPtr<HeadlessWebSocketClientManager> create()
    {
        return adopt_ref(*new HeadlessWebSocketClientManager());
    }

    virtual ~HeadlessWebSocketClientManager() override { }

    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> connect(AK::URL const&, String const&) override
    {
        return {};
    }

private:
    HeadlessWebSocketClientManager() { }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int take_screenshot_after = 1;
    StringView url;
    StringView fonts_database;
    StringView favicon_path;
    StringView theme_path;
    StringView error_page_url;

    Core::EventLoop event_loop;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("This utility runs the Browser in headless mode.");
    args_parser.add_option(take_screenshot_after, "Take a screenshot after [n] seconds (default: 1)", "screenshot", 's', "n");
    args_parser.add_option(fonts_database, "Path of the fonts on your system", "fonts", 'f', "font-database-path");
    args_parser.add_option(favicon_path, "Path of the default favicon", "favicon", 'i', "default-favicon-path");
    args_parser.add_option(theme_path, "Path of the system theme", "theme", 't', "default-theme-path");
    args_parser.add_option(error_page_url, "URL for the error page", "error-page", 'e', "error-page-url");
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    Web::ImageDecoding::Decoder::initialize(HeadlessImageDecoderClient::create());
    Web::ResourceLoader::initialize(HeadlessRequestServer::create());
    Web::WebSockets::WebSocketClientManager::initialize(HeadlessWebSocketClientManager::create());

    if (!favicon_path.is_empty())
        Web::FrameLoader::set_default_favicon_path(favicon_path);

    if (!fonts_database.is_empty())
        Gfx::FontDatabase::set_default_fonts_lookup_path(fonts_database);

    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    if (!error_page_url.is_empty())
        Web::FrameLoader::set_error_page_url(error_page_url);

    auto page_client = HeadlessBrowserPageClient::create();

    if (!theme_path.is_empty())
        page_client->setup_palette(Gfx::load_system_theme(theme_path));
    else
        page_client->setup_palette(Gfx::load_system_theme("/res/themes/Default.ini"));

    dbgln("Loading {}", url);
    page_client->load(AK::URL(url));

    // FIXME: Allow passing these values as arguments
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
