/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFView.h"

#include <LibGfx/Bitmap.h>
#include <LibPDF/Document.h>
#include <LibPDF/Renderer.h>

@interface MacPDFView ()
{
    WeakPtr<PDF::Document> _doc;
    NSBitmapImageRep* _cachedBitmap;
    int _page_index;
    __weak id<MacPDFViewDelegate> _delegate;
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

@implementation MacPDFView

// Called from MacPDFDocument.
- (void)setDocument:(WeakPtr<PDF::Document>)doc
{
    NSLog(@"doc set");
    _doc = move(doc);
    _page_index = 0;

    [self invalidateCachedBitmap];
}

- (void)goToPage:(int)page
{
    if (!_doc)
        return;

    int new_index = max(0, min(page - 1, _doc->get_page_count() - 1));
    if (new_index == _page_index)
        return;

    _page_index = new_index;
    [self invalidateRestorableState];
    [self invalidateCachedBitmap];
    [_delegate pageChanged];
}

- (int)page
{
    return _page_index + 1;
}

- (void)setDelegate:(id<MacPDFViewDelegate>)delegate
{
    _delegate = delegate;
}

#pragma mark - Drawing

- (void)invalidateCachedBitmap
{
    _cachedBitmap = nil;
    [self setNeedsDisplay:YES];
}

- (void)ensureCachedBitmapIsUpToDate
{
    if (!_doc || _doc->get_page_count() == 0)
        return;

    NSSize pixel_size = [self convertSizeToBacking:self.bounds.size];
    if (NSEqualSizes([_cachedBitmap size], pixel_size))
        return;

    if (auto bitmap_or = render(*_doc, _page_index, pixel_size); !bitmap_or.is_error())
        _cachedBitmap = ns_from_gfx(bitmap_or.value());
}

- (void)drawRect:(NSRect)rect
{
    [self ensureCachedBitmapIsUpToDate];
    [_cachedBitmap drawInRect:self.bounds];
}

#pragma mark - Keyboard handling

- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (void)keyDown:(NSEvent*)event
{
    // Calls moveLeft: or moveRight: below.
    [self interpretKeyEvents:@[ event ]];
}

// Called on left arrow.
- (IBAction)moveLeft:(id)sender
{
    int current_page = _page_index + 1;
    [self goToPage:current_page - 1];
}

// Called on right arrow.
- (IBAction)moveRight:(id)sender
{
    int current_page = _page_index + 1;
    [self goToPage:current_page + 1];
}

#pragma mark - State restoration

- (void)encodeRestorableStateWithCoder:(NSCoder*)coder
{
    [coder encodeInt:_page_index forKey:@"PageIndex"];
    NSLog(@"encodeRestorableStateWithCoder encoded %d", _page_index);
}

- (void)restoreStateWithCoder:(NSCoder*)coder
{
    if ([coder containsValueForKey:@"PageIndex"]) {
        int page_index = [coder decodeIntForKey:@"PageIndex"];
        _page_index = min(max(0, page_index), _doc->get_page_count() - 1);
        NSLog(@"encodeRestorableStateWithCoder restored %d", _page_index);
        [self invalidateCachedBitmap];
        [_delegate pageChanged];
    }
}

@end
