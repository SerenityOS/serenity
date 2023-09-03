/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/SVG/SVGDecodedImageData.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

class SVGDecodedImageData::SVGPageClient final : public PageClient {
public:
    explicit SVGPageClient(Page& host_page)
        : m_host_page(host_page)
    {
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
    virtual void paint(DevicePixelRect const&, Gfx::Bitmap&) override { }
};

ErrorOr<NonnullRefPtr<SVGDecodedImageData>> SVGDecodedImageData::create(Page& host_page, AK::URL const& url, ByteBuffer data)
{
    auto page_client = make<SVGPageClient>(host_page);
    auto page = make<Page>(*page_client);
    page_client->m_svg_page = page.ptr();
    auto browsing_context = HTML::BrowsingContext::create_a_new_top_level_browsing_context(*page);
    auto response = Fetch::Infrastructure::Response::create(browsing_context->vm());
    response->url_list().append(url);
    HTML::NavigationParams navigation_params {
        .id = {},
        .request = nullptr,
        .response = response,
        .origin = HTML::Origin {},
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = HTML::CrossOriginOpenerPolicy {},
        .coop_enforcement_result = HTML::CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .browsing_context = browsing_context,
        .navigable = nullptr,
    };
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", move(navigation_params)).release_value_but_fixme_should_propagate_errors();
    browsing_context->set_active_document(document);

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

    return adopt_nonnull_ref_or_enomem(new (nothrow) SVGDecodedImageData(move(page), move(page_client), move(document), move(svg_root)));
}

SVGDecodedImageData::SVGDecodedImageData(NonnullOwnPtr<Page> page, NonnullOwnPtr<SVGPageClient> page_client, JS::Handle<DOM::Document> document, JS::Handle<SVG::SVGSVGElement> root_element)
    : m_page(move(page))
    , m_page_client(move(page_client))
    , m_document(move(document))
    , m_root_element(move(root_element))
{
}

SVGDecodedImageData::~SVGDecodedImageData() = default;

void SVGDecodedImageData::render(Gfx::IntSize size) const
{
    m_document->browsing_context()->set_viewport_rect({ 0, 0, size.width(), size.height() });
    m_document->update_layout();

    Gfx::Painter painter(*m_bitmap);
    PaintContext context(painter, m_page_client->palette(), m_page_client->device_pixels_per_css_pixel());

    m_document->paintable()->paint_all_phases(context);
}

RefPtr<Gfx::Bitmap const> SVGDecodedImageData::bitmap(size_t, Gfx::IntSize size) const
{
    if (size.is_empty())
        return nullptr;

    if (m_bitmap && m_bitmap->size() == size)
        return m_bitmap;

    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size).release_value_but_fixme_should_propagate_errors();
    render(size);
    return m_bitmap;
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
    if (width.has_value() && height.has_value())
        return *width / *height;

    if (auto const& viewbox = m_root_element->view_box(); viewbox.has_value())
        return CSSPixels::nearest_value_for(viewbox->width) / CSSPixels::nearest_value_for(viewbox->height);

    return {};
}

}
