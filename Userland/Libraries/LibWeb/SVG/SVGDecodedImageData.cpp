/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/PaintingCommandExecutorCPU.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/SVG/SVGDecodedImageData.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGDecodedImageData);

class SVGDecodedImageData::SVGPageClient final : public PageClient {
    JS_CELL(SVGDecodedImageData::SVGPageClient, PageClient);

public:
    static JS::NonnullGCPtr<SVGPageClient> create(JS::VM& vm, Page& page)
    {
        return vm.heap().allocate_without_realm<SVGPageClient>(page);
    }

    virtual ~SVGPageClient() override = default;

    Page& m_host_page;
    Page* m_svg_page { nullptr };

    virtual Page& page() override { return *m_svg_page; }
    virtual Page const& page() const override { return *m_svg_page; }
    virtual bool is_connection_open() const override { return false; }
    virtual Gfx::Palette palette() const override { return m_host_page.client().palette(); }
    virtual DevicePixelRect screen_rect() const override { return {}; }
    virtual double device_pixels_per_css_pixel() const override { return 1.0; }
    virtual CSS::PreferredColorScheme preferred_color_scheme() const override { return m_host_page.client().preferred_color_scheme(); }
    virtual void request_file(FileRequest) override { }
    virtual void paint(DevicePixelRect const&, Gfx::Bitmap&, Web::PaintOptions = {}) override { }

private:
    explicit SVGPageClient(Page& host_page)
        : m_host_page(host_page)
    {
    }
};

ErrorOr<JS::NonnullGCPtr<SVGDecodedImageData>> SVGDecodedImageData::create(JS::Realm& realm, JS::NonnullGCPtr<Page> host_page, AK::URL const& url, ByteBuffer data)
{
    auto page_client = SVGPageClient::create(Bindings::main_thread_vm(), host_page);
    auto page = Page::create(Bindings::main_thread_vm(), *page_client);
    page_client->m_svg_page = page.ptr();
    page->set_top_level_traversable(MUST(Web::HTML::TraversableNavigable::create_a_new_top_level_traversable(*page, nullptr, {})));
    JS::NonnullGCPtr<HTML::Navigable> navigable = page->top_level_traversable();
    auto response = Fetch::Infrastructure::Response::create(navigable->vm());
    response->url_list().append(url);
    HTML::NavigationParams navigation_params {
        .id = {},
        .navigable = navigable,
        .request = nullptr,
        .response = response,
        .fetch_controller = nullptr,
        .commit_early_hints = nullptr,
        .coop_enforcement_result = HTML::CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .origin = HTML::Origin {},
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = HTML::CrossOriginOpenerPolicy {},
        .about_base_url = {},
    };
    // FIXME: Use Navigable::navigate() instead of manually replacing the navigable's document.
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html"_string, navigation_params).release_value_but_fixme_should_propagate_errors();
    navigable->set_ongoing_navigation({});
    navigable->active_document()->destroy();
    navigable->active_session_history_entry()->document_state->set_document(document);

    auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
    parser->run(document->url());

    // Perform some DOM surgery to make the SVG root element be the first child of the Document.
    // FIXME: This is a huge hack until we figure out how to actually parse separate SVG files.
    auto* svg_root = document->body()->first_child_of_type<SVG::SVGSVGElement>();
    if (!svg_root)
        return Error::from_string_literal("SVGDecodedImageData: Invalid SVG input");

    svg_root->remove();
    document->remove_all_children();

    MUST(document->append_child(*svg_root));

    return realm.heap().allocate<SVGDecodedImageData>(realm, page, page_client, document, *svg_root);
}

SVGDecodedImageData::SVGDecodedImageData(JS::NonnullGCPtr<Page> page, JS::NonnullGCPtr<SVGPageClient> page_client, JS::NonnullGCPtr<DOM::Document> document, JS::NonnullGCPtr<SVG::SVGSVGElement> root_element)
    : m_page(page)
    , m_page_client(page_client)
    , m_document(document)
    , m_root_element(root_element)
{
}

SVGDecodedImageData::~SVGDecodedImageData() = default;

void SVGDecodedImageData::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_page);
    visitor.visit(m_document);
    visitor.visit(m_page_client);
    visitor.visit(m_root_element);
}

RefPtr<Gfx::Bitmap> SVGDecodedImageData::render(Gfx::IntSize size) const
{
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size).release_value_but_fixme_should_propagate_errors();
    VERIFY(m_document->navigable());
    m_document->navigable()->set_viewport_rect({ 0, 0, size.width(), size.height() });
    m_document->update_layout();

    Painting::CommandList painting_commands;
    Painting::RecordingPainter recording_painter(painting_commands);
    PaintContext context(recording_painter, m_page_client->palette(), m_page_client->device_pixels_per_css_pixel());

    m_document->paintable()->paint_all_phases(context);

    Painting::PaintingCommandExecutorCPU executor { *bitmap };
    painting_commands.execute(executor);

    return bitmap;
}

RefPtr<Gfx::ImmutableBitmap> SVGDecodedImageData::bitmap(size_t, Gfx::IntSize size) const
{
    if (size.is_empty())
        return nullptr;

    if (m_immutable_bitmap && m_immutable_bitmap->size() == size)
        return m_immutable_bitmap;

    m_immutable_bitmap = Gfx::ImmutableBitmap::create(*render(size));
    return m_immutable_bitmap;
}

Optional<CSSPixels> SVGDecodedImageData::intrinsic_width() const
{
    // https://www.w3.org/TR/SVG2/coords.html#SizingSVGInCSS
    m_document->update_style();
    auto const* root_element_style = m_root_element->computed_css_values();
    VERIFY(root_element_style);
    auto const& width_value = root_element_style->size_value(CSS::PropertyID::Width);
    if (width_value.is_length() && width_value.length().is_absolute())
        return width_value.length().absolute_length_to_px();
    return {};
}

Optional<CSSPixels> SVGDecodedImageData::intrinsic_height() const
{
    // https://www.w3.org/TR/SVG2/coords.html#SizingSVGInCSS
    m_document->update_style();
    auto const* root_element_style = m_root_element->computed_css_values();
    VERIFY(root_element_style);
    auto const& height_value = root_element_style->size_value(CSS::PropertyID::Height);
    if (height_value.is_length() && height_value.length().is_absolute())
        return height_value.length().absolute_length_to_px();
    return {};
}

Optional<CSSPixelFraction> SVGDecodedImageData::intrinsic_aspect_ratio() const
{
    // https://www.w3.org/TR/SVG2/coords.html#SizingSVGInCSS
    auto width = intrinsic_width();
    auto height = intrinsic_height();
    if (height.has_value() && *height == 0)
        return {};

    if (width.has_value() && height.has_value())
        return *width / *height;

    if (auto const& viewbox = m_root_element->view_box(); viewbox.has_value())
        return CSSPixels::nearest_value_for(viewbox->width) / CSSPixels::nearest_value_for(viewbox->height);

    return {};
}

}
