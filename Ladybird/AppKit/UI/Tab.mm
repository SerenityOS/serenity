/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/String.h>
#include <Ladybird/Utilities.h>
#include <LibCore/Resource.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibURL/URL.h>

#import <Application/ApplicationDelegate.h>
#import <UI/Inspector.h>
#import <UI/InspectorController.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <UI/TabController.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 1000;
static constexpr CGFloat const WINDOW_HEIGHT = 800;

@interface Tab () <LadybirdWebViewObserver>

@property (nonatomic, strong) NSString* title;
@property (nonatomic, strong) NSImage* favicon;

@property (nonatomic, strong) InspectorController* inspector_controller;

@end

@implementation Tab

@dynamic title;

+ (NSImage*)defaultFavicon
{
    static NSImage* default_favicon;
    static dispatch_once_t token;

    dispatch_once(&token, ^{
        auto default_favicon_path = MUST(Core::Resource::load_from_uri("resource://icons/16x16/app-browser.png"sv));
        auto* ns_default_favicon_path = Ladybird::string_to_ns_string(default_favicon_path->filesystem_path());

        default_favicon = [[NSImage alloc] initWithContentsOfFile:ns_default_favicon_path];
    });

    return default_favicon;
}

- (instancetype)init
{
    auto screen_rect = [[NSScreen mainScreen] frame];
    auto position_x = (NSWidth(screen_rect) - WINDOW_WIDTH) / 2;
    auto position_y = (NSHeight(screen_rect) - WINDOW_HEIGHT) / 2;

    auto window_rect = NSMakeRect(position_x, position_y, WINDOW_WIDTH, WINDOW_HEIGHT);
    auto style_mask = NSWindowStyleMaskTitled | NSWindowStyleMaskClosable | NSWindowStyleMaskMiniaturizable | NSWindowStyleMaskResizable;

    self = [super initWithContentRect:window_rect
                            styleMask:style_mask
                              backing:NSBackingStoreBuffered
                                defer:NO];

    if (self) {
        // Remember last window position
        self.frameAutosaveName = @"window";

        self.web_view = [[LadybirdWebView alloc] init:self];
        [self.web_view setPostsBoundsChangedNotifications:YES];

        self.favicon = [Tab defaultFavicon];
        self.title = @"New Tab";
        [self updateTabTitleAndFavicon];

        [self setTitleVisibility:NSWindowTitleHidden];
        [self setIsVisible:YES];

        auto* scroll_view = [[NSScrollView alloc] initWithFrame:[self frame]];
        [scroll_view setHasVerticalScroller:YES];
        [scroll_view setHasHorizontalScroller:YES];
        [scroll_view setLineScroll:24];

        [scroll_view setContentView:self.web_view];
        [scroll_view setDocumentView:[[NSView alloc] init]];

        [[NSNotificationCenter defaultCenter]
            addObserver:self
               selector:@selector(onContentScroll:)
                   name:NSViewBoundsDidChangeNotification
                 object:[scroll_view contentView]];

        [self setContentView:scroll_view];
    }

    return self;
}

#pragma mark - Public methods

- (void)tabWillClose
{
    if (self.inspector_controller != nil) {
        [self.inspector_controller.window close];
    }
}

- (void)openInspector:(id)sender
{
    if (self.inspector_controller != nil) {
        [self.inspector_controller.window makeKeyAndOrderFront:sender];
        return;
    }

    self.inspector_controller = [[InspectorController alloc] init:self];
    [self.inspector_controller showWindow:nil];
}

- (void)onInspectorClosed
{
    self.inspector_controller = nil;
}

- (void)inspectElement:(id)sender
{
    [self openInspector:sender];

    auto* inspector = (Inspector*)[self.inspector_controller window];
    [inspector selectHoveredElement];
}

#pragma mark - Private methods

- (TabController*)tabController
{
    return (TabController*)[self windowController];
}

- (void)updateTabTitleAndFavicon
{
    auto* favicon_attachment = [[NSTextAttachment alloc] init];
    favicon_attachment.image = self.favicon;

    // By default, the image attachment will "automatically adapt to the surrounding font and color
    // attributes in attributed strings". Therefore, we specify a clear color here to prevent the
    // favicon from having a weird tint.
    auto* favicon_attribute = (NSMutableAttributedString*)[NSMutableAttributedString attributedStringWithAttachment:favicon_attachment];
    [favicon_attribute addAttribute:NSForegroundColorAttributeName
                              value:[NSColor clearColor]
                              range:NSMakeRange(0, [favicon_attribute length])];

    // By default, the text attachment will be aligned to the bottom of the string. We have to manually
    // try to center it vertically.
    // FIXME: Figure out a way to programmatically arrive at a good NSBaselineOffsetAttributeName. Using
    //        half the distance between the font's line height and the height of the favicon produces a
    //        value that results in the title being aligned too low still.
    auto* title_attributes = @{
        NSForegroundColorAttributeName : [NSColor textColor],
        NSBaselineOffsetAttributeName : @3
    };

    auto* title_attribute = [[NSAttributedString alloc] initWithString:self.title
                                                            attributes:title_attributes];

    auto* spacing_attribute = [[NSAttributedString alloc] initWithString:@"  "];

    auto* title_and_favicon = [[NSMutableAttributedString alloc] init];
    [title_and_favicon appendAttributedString:favicon_attribute];
    [title_and_favicon appendAttributedString:spacing_attribute];
    [title_and_favicon appendAttributedString:title_attribute];

    [[self tab] setAttributedTitle:title_and_favicon];
}

- (void)onContentScroll:(NSNotification*)notification
{
    [[self web_view] handleScroll];
}

#pragma mark - LadybirdWebViewObserver

- (String const&)onCreateNewTab:(URL::URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* delegate = (ApplicationDelegate*)[NSApp delegate];

    auto* controller = [delegate createNewTab:url
                                      fromTab:self
                                  activateTab:activate_tab];

    auto* tab = (Tab*)[controller window];
    return [[tab web_view] handle];
}

- (String const&)onCreateNewTab:(StringView)html
                            url:(URL::URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* delegate = (ApplicationDelegate*)[NSApp delegate];

    auto* controller = [delegate createNewTab:html
                                          url:url
                                      fromTab:self
                                  activateTab:activate_tab];

    auto* tab = (Tab*)[controller window];
    return [[tab web_view] handle];
}

- (void)loadURL:(URL::URL const&)url
{
    [[self tabController] loadURL:url];
}

- (void)onLoadStart:(URL::URL const&)url isRedirect:(BOOL)is_redirect
{
    [[self tabController] onLoadStart:url isRedirect:is_redirect];

    self.title = Ladybird::string_to_ns_string(url.serialize());
    self.favicon = [Tab defaultFavicon];
    [self updateTabTitleAndFavicon];

    if (self.inspector_controller != nil) {
        auto* inspector = (Inspector*)[self.inspector_controller window];
        [inspector reset];
    }
}

- (void)onLoadFinish:(URL::URL const&)url
{
    if (self.inspector_controller != nil) {
        auto* inspector = (Inspector*)[self.inspector_controller window];
        [inspector inspect];
    }
}

- (void)onTitleChange:(ByteString const&)title
{
    [[self tabController] onTitleChange:title];

    self.title = Ladybird::string_to_ns_string(title);
    [self updateTabTitleAndFavicon];
}

- (void)onFaviconChange:(Gfx::Bitmap const&)bitmap
{
    static constexpr size_t FAVICON_SIZE = 16;

    auto png = Gfx::PNGWriter::encode(bitmap);
    if (png.is_error()) {
        return;
    }

    auto* data = [NSData dataWithBytes:png.value().data()
                                length:png.value().size()];

    auto* favicon = [[NSImage alloc] initWithData:data];
    [favicon setResizingMode:NSImageResizingModeStretch];
    [favicon setSize:NSMakeSize(FAVICON_SIZE, FAVICON_SIZE)];

    self.favicon = favicon;
    [self updateTabTitleAndFavicon];
}

- (void)onAudioPlayStateChange:(Web::HTML::AudioPlayState)play_state
{
    switch (play_state) {
    case Web::HTML::AudioPlayState::Paused:
        [[self tab] setAccessoryView:nil];
        break;

    case Web::HTML::AudioPlayState::Playing:
        auto* icon = [NSImage imageNamed:NSImageNameTouchBarAudioOutputVolumeHighTemplate];
        auto* button = [NSButton buttonWithImage:icon target:nil action:nil];

        // FIXME: Add a click handler to mute the tab.
        NSButtonCell* cell = [button cell];
        [cell setImageDimsWhenDisabled:NO];
        [button setEnabled:NO];

        [[self tab] setAccessoryView:button];
        break;
    }
}

- (void)onNavigateBack
{
    [[self tabController] navigateBack:nil];
}

- (void)onNavigateForward
{
    [[self tabController] navigateForward:nil];
}

- (void)onReload
{
    [[self tabController] reload:nil];
}

#pragma mark - NSWindow

- (void)setIsVisible:(BOOL)flag
{
    [[self web_view] handleVisibility:flag];
    [super setIsVisible:flag];
}

- (void)setIsMiniaturized:(BOOL)flag
{
    [[self web_view] handleVisibility:!flag];
    [super setIsMiniaturized:flag];
}

@end
