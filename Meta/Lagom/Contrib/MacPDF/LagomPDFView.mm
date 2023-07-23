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
    int _page_index;
}
@end

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render(PDF::Document& document, int page_index, NSSize size)
{
    auto page = TRY(document.get_page(page_index));

    Gfx::IntSize page_size;
    page_size.set_width(size.width);
    page_size.set_height(size.height);

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

- (void)pageChanged
{
    if (!_doc || _doc->get_page_count() == 0)
        return;
    NSSize pixel_size = [self convertSizeToBacking:self.bounds.size];
    if (auto bitmap_or = render(*_doc, _page_index, pixel_size); !bitmap_or.is_error())
        _rep = ns_from_gfx(bitmap_or.value());
}

- (void)drawRect:(NSRect)rect
{
    static bool did_load = false;
    if (!did_load) {
        _page_index = 0;
        did_load = true;
        if (auto doc_or = [self load]; !doc_or.is_error()) {
            _doc = doc_or.value();
            [self pageChanged];
        } else {
            NSLog(@"failed to load: %@", @(doc_or.error().message().characters()));
        }
    }
    [_rep drawInRect:self.bounds];
}

- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (void)keyDown:(NSEvent*)event
{
    [self interpretKeyEvents:@[ event ]];
}

- (IBAction)moveLeft:(id)sender
{
    if (_page_index > 0) {
        _page_index--;
        [self pageChanged];
        [self setNeedsDisplay:YES];
    }
}

- (IBAction)moveRight:(id)sender
{
    if (_page_index < _doc->get_page_count() - 1) {
        _page_index++;
        [self pageChanged];
        [self setNeedsDisplay:YES];
    }
}
@end
