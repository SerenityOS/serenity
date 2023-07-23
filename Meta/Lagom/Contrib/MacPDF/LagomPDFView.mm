//
//  PDFView.m
//  SerenityPDF
//
//  Created by Nico Weber on 7/22/23.
//

#import "LagomPDFView.h"

// #define USING_AK_GLOBALLY 0

#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Bitmap.h>
#include <LibPDF/Document.h>
#include <LibPDF/Renderer.h>

RefPtr<Core::MappedFile> s_file;

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Document>> load()
{
    auto source_root = DeprecatedString("/Users/thakis/src/serenity");
    Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root));

    NSLog(@"before file");
    s_file = TRY(Core::MappedFile::map("/Users/thakis/src/hack/sample.pdf"sv));
    NSLog(@"got file");
    auto document = TRY(PDF::Document::create(s_file->bytes()));
    TRY(document->initialize());
    return document;
}

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render(PDF::Document& document)
{
    NSLog(@"num pages %@", @(document.get_page_count()));
    int page_index = 0;
    auto page = TRY(document.get_page(page_index));
    auto page_size = Gfx::IntSize { 800, round_to<int>(800 * page.media_box.height() / page.media_box.width()) };

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, page_size));

    auto errors = PDF::Renderer::render(document, page, bitmap, PDF::RenderingPreferences {});
    if (errors.is_error()) {
        for (auto const& error : errors.error().errors())
            NSLog(@"warning: %@", @(error.message().characters()));
    }

    return bitmap;
}

@implementation LagomPDFView

- (void)drawRect:(NSRect)rect
{
    static auto doc_or = load();
    if (!doc_or.is_error()) {
        auto doc = doc_or.value();
        auto bitmap_or = render(*doc);
        if (!bitmap_or.is_error()) {
            auto& bitmap_o = bitmap_or.value();
            auto& bitmap = bitmap_o.leak_ref();
            auto space = CGColorSpaceCreateDeviceRGB();
            CGBitmapInfo info = kCGBitmapByteOrder32Little | kCGImageAlphaFirst;
            auto data = CGDataProviderCreateWithData(
                &bitmap, bitmap.begin(), bitmap.size_in_bytes(),
                [](void* p, void const*, size_t) { /* XXX adoptRef again */ });
            auto cgbmp = CGImageCreate(bitmap.width(), bitmap.height(), 8,
                32, bitmap.width() * 4, space,
                info, data, nullptr, false, kCGRenderingIntentDefault);
            auto* nsbmp = [[NSBitmapImageRep alloc] initWithCGImage:cgbmp];
            [nsbmp drawAtPoint:NSMakePoint(0, 0)];
            CGImageRelease(cgbmp);
        }
    } else {
        NSLog(@"failed to load: %@", @(doc_or.error().message().characters()));
    }
}

@end
