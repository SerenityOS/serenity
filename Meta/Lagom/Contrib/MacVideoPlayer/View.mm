/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "View.h"

#include <LibMedia/PlaybackManager.h>

@interface View ()
{
    Media::PlaybackManager* _manager;
    NSBitmapImageRep* _currentFrame;
    NSString* _errorMessage;
}
@end

static NSBitmapImageRep* ns_from_gfx(RefPtr<Gfx::Bitmap> bitmap_p)
{
    if (!bitmap_p)
        return nil;
    auto* bitmap = bitmap_p.leak_ref();
    CGBitmapInfo info = (CGBitmapInfo)(kCGBitmapByteOrder32Little | (CGBitmapInfo)kCGImageAlphaFirst);
    auto data = CGDataProviderCreateWithData(
        bitmap, bitmap->begin(), bitmap->size_in_bytes(),
        [](void* p, void const*, size_t) {
            (void)adopt_ref(*reinterpret_cast<Gfx::Bitmap*>(p));
        });
    auto space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    auto cgbmp = CGImageCreate(bitmap->width(), bitmap->height(), 8,
        32, bitmap->pitch(), space,
        info, data, nullptr, false, kCGRenderingIntentDefault);
    CGColorSpaceRelease(space);
    CGDataProviderRelease(data);
    auto* bmp = [[NSBitmapImageRep alloc] initWithCGImage:cgbmp];
    CGImageRelease(cgbmp);
    return bmp;
}

@implementation View

- (void)setPlaybackManager:(Media::PlaybackManager*)manager
{
    _manager = manager;
    if (!_manager)
        return;

    __weak View* weak_self = self;

    _manager->on_video_frame = [weak_self](auto frame) {
        View* strong_self = weak_self;
        if (strong_self) {
            auto* ns_frame = ns_from_gfx(move(frame));
            strong_self->_currentFrame = ns_frame;
            strong_self->_errorMessage = nil;
            [strong_self setNeedsDisplay:YES];
        }
    };

    _manager->on_decoder_error = [weak_self](auto error) {
        View* strong_self = weak_self;
        if (strong_self) {
            auto error_string = error.description();
            strong_self->_currentFrame = nil;
            strong_self->_errorMessage = [NSString stringWithFormat:@"Decoder error: %.*s", (int)error_string.length(), error_string.characters_without_null_termination()];
            [strong_self setNeedsDisplay:YES];
        }
    };

    _manager->on_fatal_playback_error = [weak_self](auto const& error) {
        View* strong_self = weak_self;
        if (strong_self) {
            auto error_string = error.string_literal();
            strong_self->_currentFrame = nil;
            strong_self->_errorMessage = [NSString stringWithFormat:@"Fatal playback error: %.*s", (int)error_string.length(), error_string.characters_without_null_termination()];
            [strong_self setNeedsDisplay:YES];
        }
    };

    _manager->resume_playback();
}

- (void)drawRect:(NSRect)rect
{
    [[NSColor blackColor] setFill];
    NSRectFill(rect);

    if (_errorMessage) {
        NSFont* font = [NSFont preferredFontForTextStyle:NSFontTextStyleTitle1 options:@{}];

        NSMutableParagraphStyle* paragraph_style = [[NSMutableParagraphStyle defaultParagraphStyle] mutableCopy];
        paragraph_style.alignment = NSTextAlignmentCenter;

        NSRect bounding_rect = self.bounds;
        bounding_rect.size.height *= 0.66;

        [_errorMessage drawInRect:bounding_rect
                   withAttributes:@{
                       NSFontAttributeName : font,
                       NSForegroundColorAttributeName : [NSColor whiteColor],
                       NSParagraphStyleAttributeName : paragraph_style,
                   }];
    }

    if (!_currentFrame)
        return;

    NSSize view_size = self.bounds.size;
    NSSize image_size = [_currentFrame size];

    CGFloat view_aspect = view_size.width / view_size.height;
    CGFloat image_aspect = image_size.width / image_size.height;

    NSRect destination_rect;
    if (image_aspect > view_aspect) {
        CGFloat height = view_size.width / image_aspect;
        destination_rect = NSMakeRect(0, (view_size.height - height) / 2, view_size.width, height);
    } else {
        CGFloat width = view_size.height * image_aspect;
        destination_rect = NSMakeRect((view_size.width - width) / 2, 0, width, view_size.height);
    }

    [_currentFrame drawInRect:destination_rect];
}

@end
