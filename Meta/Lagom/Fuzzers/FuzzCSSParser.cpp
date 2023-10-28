/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>

namespace {

struct Globals {
    Globals();
} globals;

Globals::Globals()
{
    // FIXME: Somehow pick a suitable font location and default query
    Core::ResourceImplementation::install(make<Core::ResourceImplementationFile>("/usr/share/fonts"_string));
    Gfx::FontDatabase::set_default_font_query("Liberation Sans 10 400 0");
    Gfx::FontDatabase::the().load_all_fonts_from_uri("resource:///"sv);

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    MUST(Web::Bindings::initialize_main_thread_vm());
}

class FuzzPageClient final : public Web::PageClient {
public:
    explicit FuzzPageClient()
        : m_page(make<Web::Page>(*this))
    {
    }

    virtual ~FuzzPageClient() override = default;

    NonnullOwnPtr<Web::Page> m_page;

    virtual Web::Page& page() override { return *m_page; }
    virtual Web::Page const& page() const override { return *m_page; }
    virtual bool is_connection_open() const override { return false; }
    virtual Gfx::Palette palette() const override { return m_page->palette(); }
    virtual Web::DevicePixelRect screen_rect() const override { return {}; }
    virtual double device_pixels_per_css_pixel() const override { return 1.0; }
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override { return m_page->preferred_color_scheme(); }
    virtual void request_file(Web::FileRequest) override { }
    virtual void paint(Web::DevicePixelRect const&, Gfx::Bitmap&) override { }
};

}

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    // Set up a fake browser environment so that the CSS objects we construct don't freak out.
    // FIXME: There's got to be a better way to do this "correctly"

    Core::EventLoop event_loop;

    auto page_client = make<FuzzPageClient>();
    JS::NonnullGCPtr<Web::HTML::Navigable> navigable = page_client->page().top_level_traversable();

    auto response = Web::Fetch::Infrastructure::Response::create(navigable->vm());
    response->url_list().append(URL());

    Web::HTML::NavigationParams navigation_params {
        .id = {},
        .navigable = navigable,
        .request = nullptr,
        .response = response,
        .fetch_controller = nullptr,
        .commit_early_hints = nullptr,
        .coop_enforcement_result = Web::HTML::CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .origin = Web::HTML::Origin {},
        .policy_container = Web::HTML::PolicyContainer {},
        .final_sandboxing_flag_set = Web::HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = Web::HTML::CrossOriginOpenerPolicy {},
        .about_base_url = {},
    };
    auto document = MUST(Web::DOM::Document::create_and_initialize(Web::DOM::Document::Type::HTML, "text/html", navigation_params));
    (void)Web::parse_css_stylesheet(Web::CSS::Parser::ParsingContext(document->realm()), { data, size });
    return 0;
}
