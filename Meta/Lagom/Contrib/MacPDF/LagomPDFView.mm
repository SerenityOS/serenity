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

@interface LagomPDFView ()
{
    RefPtr<Core::MappedFile> _file;
    RefPtr<PDF::Document> _doc;
    NSBitmapImageRep* _rep;
}
@end

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

static NSBitmapImageRep* ns_from_gfx(NonnullRefPtr<Gfx::Bitmap> bitmap_p)
{
    auto& bitmap = bitmap_p.leak_ref();
    CGBitmapInfo info = kCGBitmapByteOrder32Little | (CGBitmapInfo)kCGImageAlphaFirst;
    auto data = CGDataProviderCreateWithData(
        &bitmap, bitmap.begin(), bitmap.size_in_bytes(),
        [](void* p, void const*, size_t) {
            (void)adopt_ref(*reinterpret_cast<Gfx::Bitmap*>(p));
        });
    auto space = CGColorSpaceCreateDeviceRGB();
    auto cgbmp = CGImageCreate(bitmap.width(), bitmap.height(), 8,
        32, bitmap.pitch(), space,
        info, data, nullptr, false, kCGRenderingIntentDefault);
    CGColorSpaceRelease(space);
    CGDataProviderRelease(data);
    auto* bmp = [[NSBitmapImageRep alloc] initWithCGImage:cgbmp];
    CGImageRelease(cgbmp);
    return bmp;
}

@implementation LagomPDFView

- (PDF::PDFErrorOr<NonnullRefPtr<PDF::Document>>)load
{
    auto source_root = DeprecatedString("/Users/thakis/src/serenity");
    Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root));

    NSLog(@"before file");
    _file = TRY(Core::MappedFile::map("/Users/thakis/src/hack/sample.pdf"sv));
    NSLog(@"got file");
    auto document = TRY(PDF::Document::create(_file->bytes()));
    TRY(document->initialize());
    return document;
}

- (void)drawRect:(NSRect)rect
{
    static bool did_load = false;
    if (!did_load) {
        did_load = true;
        if (auto doc_or = [self load]; !doc_or.is_error()) {
            _doc = doc_or.value();
            if (auto bitmap_or = render(*_doc); !bitmap_or.is_error())
                _rep = ns_from_gfx(bitmap_or.value());
        } else {
            NSLog(@"failed to load: %@", @(doc_or.error().message().characters()));
        }
    }
    [_rep drawAtPoint:NSMakePoint(0, 0)];
}

@end
