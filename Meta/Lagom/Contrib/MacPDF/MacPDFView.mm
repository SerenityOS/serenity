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
    PDF::RenderingPreferences _preferences;
}
@end

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render(PDF::Document& document, int page_index, NSSize size, PDF::RenderingPreferences const& preferences)
{
    auto page = TRY(document.get_page(page_index));

    auto page_size = Gfx::IntSize { size.width, size.height };
    if (int rotation_count = (page.rotate / 90) % 4; rotation_count % 2 == 1)
        page_size = Gfx::IntSize { page_size.height(), page_size.width() };

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, page_size));

    auto errors = PDF::Renderer::render(document, page, bitmap, Color::White, preferences);
    if (errors.is_error()) {
        for (auto const& error : errors.error().errors())
            NSLog(@"warning: %@", @(error.message().characters()));
    }

    return TRY(PDF::Renderer::apply_page_rotation(bitmap, page));
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
    _doc = move(doc);
    _page_index = 0;

    [self addObserver:self
           forKeyPath:@"safeAreaRect"
              options:NSKeyValueObservingOptionNew
              context:nil];

    [self invalidateCachedBitmap];
}

- (void)observeValueForKeyPath:(NSString*)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSKeyValueChangeKey, id>*)change
                       context:(void*)context
{
    // AppKit by default doesn't invalidate a view if safeAreaRect changes but the view's bounds don't change.
    // This happens for example when toggling the visibility of the toolbar with a full-size content view.
    // We do want a repaint in this case.
    VERIFY([keyPath isEqualToString:@"safeAreaRect"]);
    VERIFY(object == self);
    [self setNeedsDisplay:YES];
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

    NSSize pixel_size = [self convertSizeToBacking:self.safeAreaRect.size];
    if (NSEqualSizes([_cachedBitmap size], pixel_size))
        return;

    if (auto bitmap_or = render(*_doc, _page_index, pixel_size, _preferences); !bitmap_or.is_error())
        _cachedBitmap = ns_from_gfx(bitmap_or.value());
}

- (void)drawRect:(NSRect)rect
{
    [self ensureCachedBitmapIsUpToDate];
    [_cachedBitmap drawInRect:self.safeAreaRect];
}

#pragma mark - Keyboard handling

- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (IBAction)goToNextPage:(id)sender
{
    int current_page = _page_index + 1;
    [self goToPage:current_page + 1];
}

- (IBAction)goToPreviousPage:(id)sender
{
    int current_page = _page_index + 1;
    [self goToPage:current_page - 1];
}

- (BOOL)validateMenuItem:(NSMenuItem*)item
{
    if ([item action] == @selector(goToNextPage:))
        return _doc ? (_page_index < (int)_doc->get_page_count() - 1) : NO;
    if ([item action] == @selector(goToPreviousPage:))
        return _doc ? (_page_index > 0) : NO;
    if ([item action] == @selector(toggleShowClippingPaths:)) {
        [item setState:_preferences.show_clipping_paths ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    if ([item action] == @selector(toggleClipImages:)) {
        [item setState:_preferences.clip_images ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    if ([item action] == @selector(toggleClipPaths:)) {
        [item setState:_preferences.clip_paths ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    if ([item action] == @selector(toggleClipText:)) {
        [item setState:_preferences.clip_text ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    if ([item action] == @selector(toggleShowImages:)) {
        [item setState:_preferences.show_images ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    if ([item action] == @selector(toggleShowHiddenText:)) {
        [item setState:_preferences.show_hidden_text ? NSControlStateValueOn : NSControlStateValueOff];
        return _doc ? YES : NO;
    }
    return NO;
}

- (IBAction)toggleShowClippingPaths:(id)sender
{
    if (_doc) {
        _preferences.show_clipping_paths = !_preferences.show_clipping_paths;
        [self invalidateCachedBitmap];
    }
}

- (IBAction)toggleClipImages:(id)sender
{
    if (_doc) {
        _preferences.clip_images = !_preferences.clip_images;
        [self invalidateCachedBitmap];
    }
}

- (IBAction)toggleClipPaths:(id)sender
{
    if (_doc) {
        _preferences.clip_paths = !_preferences.clip_paths;
        [self invalidateCachedBitmap];
    }
}

- (IBAction)toggleClipText:(id)sender
{
    if (_doc) {
        _preferences.clip_text = !_preferences.clip_text;
        [self invalidateCachedBitmap];
    }
}

- (IBAction)toggleShowImages:(id)sender
{
    if (_doc) {
        _preferences.show_images = !_preferences.show_images;
        [self invalidateCachedBitmap];
    }
}

- (IBAction)toggleShowHiddenText:(id)sender
{
    if (_doc) {
        _preferences.show_hidden_text = !_preferences.show_hidden_text;
        [self invalidateCachedBitmap];
    }
}

- (void)keyDown:(NSEvent*)event
{
    // Calls moveLeft: or moveRight: below.
    [self interpretKeyEvents:@[ event ]];
}

// Called on down arrow.
- (IBAction)moveDown:(id)sender
{
    [self goToNextPage:self];
}

// Called on left arrow.
- (IBAction)moveLeft:(id)sender
{
    [self goToPreviousPage:self];
}

// Called on right arrow.
- (IBAction)moveRight:(id)sender
{
    [self goToNextPage:self];
}

// Called on up arrow.
- (IBAction)moveUp:(id)sender
{
    [self goToPreviousPage:self];
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
