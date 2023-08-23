/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/TemporaryChange.h>
#include <AK/URL.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ShareableBitmap.h>
#include <UI/LadybirdWebViewBridge.h>

#import <Application/ApplicationDelegate.h>
#import <UI/Event.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <UI/TabController.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr NSInteger CONTEXT_MENU_PLAY_PAUSE_TAG = 1;
static constexpr NSInteger CONTEXT_MENU_MUTE_UNMUTE_TAG = 2;
static constexpr NSInteger CONTEXT_MENU_CONTROLS_TAG = 3;
static constexpr NSInteger CONTEXT_MENU_LOOP_TAG = 4;

// Calls to [NSCursor hide] and [NSCursor unhide] must be balanced. We use this struct to ensure
// we only call [NSCursor hide] once and to ensure that we do call [NSCursor unhide].
// https://developer.apple.com/documentation/appkit/nscursor#1651301
struct HideCursor {
    HideCursor()
    {
        [NSCursor hide];
    }

    ~HideCursor()
    {
        [NSCursor unhide];
    }
};

@interface LadybirdWebView ()
{
    OwnPtr<Ladybird::WebViewBridge> m_web_view_bridge;

    URL m_context_menu_url;
    Gfx::ShareableBitmap m_context_menu_bitmap;

    Optional<HideCursor> m_hidden_cursor;
}

@property (nonatomic, strong) NSMenu* page_context_menu;
@property (nonatomic, strong) NSMenu* link_context_menu;
@property (nonatomic, strong) NSMenu* image_context_menu;
@property (nonatomic, strong) NSMenu* video_context_menu;
@property (nonatomic, strong) NSTextField* status_label;
@property (nonatomic, strong) NSAlert* dialog;

@end

@implementation LadybirdWebView

@synthesize page_context_menu = _page_context_menu;
@synthesize link_context_menu = _link_context_menu;
@synthesize image_context_menu = _image_context_menu;
@synthesize video_context_menu = _video_context_menu;
@synthesize status_label = _status_label;

- (instancetype)init
{
    if (self = [super init]) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        auto* screens = [NSScreen screens];

        Vector<Gfx::IntRect> screen_rects;
        screen_rects.ensure_capacity([screens count]);

        for (id screen in screens) {
            auto screen_rect = Ladybird::ns_rect_to_gfx_rect([screen frame]);
            screen_rects.unchecked_append(screen_rect);
        }

        auto device_pixel_ratio = [[NSScreen mainScreen] backingScaleFactor];

        m_web_view_bridge = MUST(Ladybird::WebViewBridge::create(move(screen_rects), device_pixel_ratio, [delegate webdriverContentIPCPath]));
        [self setWebViewCallbacks];

        auto* area = [[NSTrackingArea alloc] initWithRect:[self bounds]
                                                  options:NSTrackingActiveInKeyWindow | NSTrackingInVisibleRect | NSTrackingMouseMoved
                                                    owner:self
                                                 userInfo:nil];
        [self addTrackingArea:area];
    }

    return self;
}

#pragma mark - Public methods

- (void)load:(URL const&)url
{
    m_web_view_bridge->load(url);
}

- (void)handleResize
{
    [self updateViewportRect:Ladybird::WebViewBridge::ForResize::Yes];
    [self updateStatusLabelPosition];
}

- (void)handleScroll
{
    [self updateViewportRect:Ladybird::WebViewBridge::ForResize::No];
    [self updateStatusLabelPosition];
}

- (void)handleVisibility:(BOOL)is_visible
{
    m_web_view_bridge->set_system_visibility_state(is_visible);
}

#pragma mark - Private methods

- (void)updateViewportRect:(Ladybird::WebViewBridge::ForResize)for_resize
{
    auto content_rect = [self frame];
    auto document_rect = [[self documentView] frame];
    auto device_pixel_ratio = m_web_view_bridge->device_pixel_ratio();

    auto position = [&](auto content_size, auto document_size, auto scroll) {
        return max(0, (document_size - content_size) * device_pixel_ratio * scroll);
    };

    auto horizontal_scroll = [[[self scrollView] horizontalScroller] floatValue];
    auto vertical_scroll = [[[self scrollView] verticalScroller] floatValue];

    auto ns_viewport_rect = NSMakeRect(
        position(content_rect.size.width, document_rect.size.width, horizontal_scroll),
        position(content_rect.size.height, document_rect.size.height, vertical_scroll),
        content_rect.size.width,
        content_rect.size.height);

    auto viewport_rect = Ladybird::ns_rect_to_gfx_rect(ns_viewport_rect);
    m_web_view_bridge->set_viewport_rect(viewport_rect, for_resize);
}

- (void)updateStatusLabelPosition
{
    static constexpr CGFloat LABEL_INSET = 10;

    if (_status_label == nil || [[self status_label] isHidden]) {
        return;
    }

    auto visible_rect = [self visibleRect];
    auto status_label_rect = [self.status_label frame];

    auto position = NSMakePoint(LABEL_INSET, visible_rect.origin.y + visible_rect.size.height - status_label_rect.size.height - LABEL_INSET);
    [self.status_label setFrameOrigin:position];
}

- (void)setWebViewCallbacks
{
    m_web_view_bridge->on_did_layout = [self](auto content_size) {
        auto inverse_device_pixel_ratio = m_web_view_bridge->inverse_device_pixel_ratio();
        [[self documentView] setFrameSize:NSMakeSize(content_size.width() * inverse_device_pixel_ratio, content_size.height() * inverse_device_pixel_ratio)];
    };

    m_web_view_bridge->on_ready_to_paint = [self]() {
        [self setNeedsDisplay:YES];
    };

    m_web_view_bridge->on_new_tab = [](auto activate_tab) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        auto* controller = [delegate createNewTab:"about:blank"sv activateTab:activate_tab];

        auto* tab = (Tab*)[controller window];
        auto* web_view = [tab web_view];

        return web_view->m_web_view_bridge->handle();
    };

    m_web_view_bridge->on_activate_tab = [self]() {
        [[self tab] orderFront:nil];
    };

    m_web_view_bridge->on_close = [self]() {
        [[self tab] close];
    };

    m_web_view_bridge->on_load_start = [self](auto const& url, bool is_redirect) {
        [[self tabController] onLoadStart:url isRedirect:is_redirect];
        [[self tab] onLoadStart:url];

        if (_status_label != nil) {
            [self.status_label setHidden:YES];
        }
    };

    m_web_view_bridge->on_title_change = [self](auto const& title) {
        [[self tabController] onTitleChange:title];

        auto* ns_title = Ladybird::string_to_ns_string(title);
        [[self tab] onTitleChange:ns_title];
    };

    m_web_view_bridge->on_favicon_change = [self](auto const& bitmap) {
        static constexpr size_t FAVICON_SIZE = 16;

        auto png = Gfx::PNGWriter::encode(bitmap);
        if (png.is_error()) {
            return;
        }

        auto* data = [NSData dataWithBytes:png.value().data() length:png.value().size()];

        auto* favicon = [[NSImage alloc] initWithData:data];
        [favicon setResizingMode:NSImageResizingModeStretch];
        [favicon setSize:NSMakeSize(FAVICON_SIZE, FAVICON_SIZE)];

        [[self tab] onFaviconChange:favicon];
    };

    m_web_view_bridge->on_scroll_to_point = [self](auto position) {
        [self scrollToPoint:Ladybird::gfx_point_to_ns_point(position)];
        [[self scrollView] reflectScrolledClipView:self];
        [self updateViewportRect:Ladybird::WebViewBridge::ForResize::No];
    };

    m_web_view_bridge->on_cursor_change = [self](auto cursor) {
        if (cursor == Gfx::StandardCursor::Hidden) {
            if (!m_hidden_cursor.has_value()) {
                m_hidden_cursor.emplace();
            }

            return;
        }

        m_hidden_cursor.clear();

        switch (cursor) {
        case Gfx::StandardCursor::Arrow:
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Crosshair:
            [[NSCursor crosshairCursor] set];
            break;
        case Gfx::StandardCursor::IBeam:
            [[NSCursor IBeamCursor] set];
            break;
        case Gfx::StandardCursor::ResizeHorizontal:
            [[NSCursor resizeLeftRightCursor] set];
            break;
        case Gfx::StandardCursor::ResizeVertical:
            [[NSCursor resizeUpDownCursor] set];
            break;
        case Gfx::StandardCursor::ResizeDiagonalTLBR:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::ResizeDiagonalBLTR:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::ResizeColumn:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::ResizeRow:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Hand:
            [[NSCursor pointingHandCursor] set];
            break;
        case Gfx::StandardCursor::Help:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Drag:
            [[NSCursor closedHandCursor] set];
            break;
        case Gfx::StandardCursor::DragCopy:
            [[NSCursor dragCopyCursor] set];
            break;
        case Gfx::StandardCursor::Move:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor dragCopyCursor] set];
            break;
        case Gfx::StandardCursor::Wait:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Disallowed:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Eyedropper:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        case Gfx::StandardCursor::Zoom:
            // FIXME: AppKit does not have a corresponding cursor, so we should make one.
            [[NSCursor arrowCursor] set];
            break;
        default:
            break;
        }
    };

    m_web_view_bridge->on_navigate_back = [self]() {
        [[self tabController] navigateBack:nil];
    };

    m_web_view_bridge->on_navigate_forward = [self]() {
        [[self tabController] navigateForward:nil];
    };

    m_web_view_bridge->on_refresh = [self]() {
        [[self tabController] reload:nil];
    };

    m_web_view_bridge->on_tooltip_entered = [self](auto const& tooltip) {
        self.toolTip = Ladybird::string_to_ns_string(tooltip);
    };

    m_web_view_bridge->on_tooltip_left = [self]() {
        self.toolTip = nil;
    };

    m_web_view_bridge->on_link_hover = [self](auto const& url) {
        auto* url_string = Ladybird::string_to_ns_string(url.serialize());
        [self.status_label setStringValue:url_string];
        [self.status_label sizeToFit];
        [self.status_label setHidden:NO];

        [self updateStatusLabelPosition];
    };

    m_web_view_bridge->on_link_unhover = [self]() {
        [self.status_label setHidden:YES];
    };

    m_web_view_bridge->on_link_click = [self](auto const& url, auto const& target, unsigned modifiers) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];

        if (modifiers == Mod_Super) {
            [delegate createNewTab:url activateTab:Web::HTML::ActivateTab::No];
        } else if (target == "_blank"sv) {
            [delegate createNewTab:url activateTab:Web::HTML::ActivateTab::Yes];
        } else {
            [[self tabController] load:url];
        }
    };

    m_web_view_bridge->on_link_middle_click = [](auto url, auto, unsigned) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        [delegate createNewTab:url activateTab:Web::HTML::ActivateTab::No];
    };

    m_web_view_bridge->on_context_menu_request = [self](auto position) {
        auto* event = Ladybird::create_context_menu_mouse_event(self, position);
        [NSMenu popUpContextMenu:self.page_context_menu withEvent:event forView:self];
    };

    m_web_view_bridge->on_link_context_menu_request = [self](auto const& url, auto position) {
        TemporaryChange change_url { m_context_menu_url, url };

        auto* event = Ladybird::create_context_menu_mouse_event(self, position);
        [NSMenu popUpContextMenu:self.link_context_menu withEvent:event forView:self];
    };

    m_web_view_bridge->on_image_context_menu_request = [self](auto const& url, auto position, auto const& bitmap) {
        TemporaryChange change_url { m_context_menu_url, url };
        TemporaryChange change_bitmap { m_context_menu_bitmap, bitmap };

        auto* event = Ladybird::create_context_menu_mouse_event(self, position);
        [NSMenu popUpContextMenu:self.image_context_menu withEvent:event forView:self];
    };

    m_web_view_bridge->on_media_context_menu_request = [self](auto position, auto const& menu) {
        if (!menu.is_video) {
            NSLog(@"TODO: Implement audio context menu once audio elements are supported");
            return;
        }

        TemporaryChange change_url { m_context_menu_url, menu.media_url };

        auto* play_pause_menu_item = [self.video_context_menu itemWithTag:CONTEXT_MENU_PLAY_PAUSE_TAG];
        auto* mute_unmute_menu_item = [self.video_context_menu itemWithTag:CONTEXT_MENU_MUTE_UNMUTE_TAG];
        auto* controls_menu_item = [self.video_context_menu itemWithTag:CONTEXT_MENU_CONTROLS_TAG];
        auto* loop_menu_item = [self.video_context_menu itemWithTag:CONTEXT_MENU_LOOP_TAG];

        if (menu.is_playing) {
            [play_pause_menu_item setTitle:@"Pause"];
        } else {
            [play_pause_menu_item setTitle:@"Play"];
        }

        if (menu.is_muted) {
            [mute_unmute_menu_item setTitle:@"Unmute"];
        } else {
            [mute_unmute_menu_item setTitle:@"Mute"];
        }

        auto controls_state = menu.has_user_agent_controls ? NSControlStateValueOn : NSControlStateValueOff;
        [controls_menu_item setState:controls_state];

        auto loop_state = menu.is_looping ? NSControlStateValueOn : NSControlStateValueOff;
        [loop_menu_item setState:loop_state];

        auto* event = Ladybird::create_context_menu_mouse_event(self, position);
        [NSMenu popUpContextMenu:self.video_context_menu withEvent:event forView:self];
    };

    m_web_view_bridge->on_request_alert = [self](auto const& message) {
        auto* ns_message = Ladybird::string_to_ns_string(message);

        self.dialog = [[NSAlert alloc] init];
        [self.dialog setMessageText:ns_message];

        [self.dialog beginSheetModalForWindow:[self window]
                            completionHandler:^(NSModalResponse) {
                                m_web_view_bridge->alert_closed();
                                self.dialog = nil;
                            }];
    };

    m_web_view_bridge->on_request_confirm = [self](auto const& message) {
        auto* ns_message = Ladybird::string_to_ns_string(message);

        self.dialog = [[NSAlert alloc] init];
        [[self.dialog addButtonWithTitle:@"OK"] setTag:NSModalResponseOK];
        [[self.dialog addButtonWithTitle:@"Cancel"] setTag:NSModalResponseCancel];
        [self.dialog setMessageText:ns_message];

        [self.dialog beginSheetModalForWindow:[self window]
                            completionHandler:^(NSModalResponse response) {
                                m_web_view_bridge->confirm_closed(response == NSModalResponseOK);
                                self.dialog = nil;
                            }];
    };

    m_web_view_bridge->on_request_prompt = [self](auto const& message, auto const& default_) {
        auto* ns_message = Ladybird::string_to_ns_string(message);
        auto* ns_default = Ladybird::string_to_ns_string(default_);

        auto* input = [[NSTextField alloc] initWithFrame:NSMakeRect(0, 0, 200, 24)];
        [input setStringValue:ns_default];

        self.dialog = [[NSAlert alloc] init];
        [[self.dialog addButtonWithTitle:@"OK"] setTag:NSModalResponseOK];
        [[self.dialog addButtonWithTitle:@"Cancel"] setTag:NSModalResponseCancel];
        [self.dialog setMessageText:ns_message];
        [self.dialog setAccessoryView:input];

        self.dialog.window.initialFirstResponder = input;

        [self.dialog beginSheetModalForWindow:[self window]
                            completionHandler:^(NSModalResponse response) {
                                Optional<String> text;

                                if (response == NSModalResponseOK) {
                                    text = Ladybird::ns_string_to_string([input stringValue]);
                                }

                                m_web_view_bridge->prompt_closed(move(text));
                                self.dialog = nil;
                            }];
    };

    m_web_view_bridge->on_request_set_prompt_text = [self](auto const& message) {
        if (self.dialog == nil || [self.dialog accessoryView] == nil) {
            return;
        }

        auto* ns_message = Ladybird::string_to_ns_string(message);

        auto* input = (NSTextField*)[self.dialog accessoryView];
        [input setStringValue:ns_message];
    };

    m_web_view_bridge->on_request_accept_dialog = [self]() {
        if (self.dialog == nil) {
            return;
        }

        [[self window] endSheet:[[self dialog] window]
                     returnCode:NSModalResponseOK];
    };

    m_web_view_bridge->on_request_dismiss_dialog = [self]() {
        if (self.dialog == nil) {
            return;
        }

        [[self window] endSheet:[[self dialog] window]
                     returnCode:NSModalResponseCancel];
    };

    m_web_view_bridge->on_get_all_cookies = [](auto const& url) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        return [delegate cookieJar].get_all_cookies(url);
    };

    m_web_view_bridge->on_get_named_cookie = [](auto const& url, auto const& name) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        return [delegate cookieJar].get_named_cookie(url, name);
    };

    m_web_view_bridge->on_get_cookie = [](auto const& url, auto source) -> DeprecatedString {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        return [delegate cookieJar].get_cookie(url, source);
    };

    m_web_view_bridge->on_set_cookie = [](auto const& url, auto const& cookie, auto source) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        [delegate cookieJar].set_cookie(url, cookie, source);
    };

    m_web_view_bridge->on_update_cookie = [](auto const& cookie) {
        auto* delegate = (ApplicationDelegate*)[NSApp delegate];
        [delegate cookieJar].update_cookie(cookie);
    };

    m_web_view_bridge->on_restore_window = [self]() {
        [[self window] setIsMiniaturized:NO];
        [[self window] orderFront:nil];
    };

    m_web_view_bridge->on_reposition_window = [self](auto const& position) {
        auto frame = [[self window] frame];
        frame.origin = Ladybird::gfx_point_to_ns_point(position);
        [[self window] setFrame:frame display:YES];

        return Ladybird::ns_point_to_gfx_point([[self window] frame].origin);
    };

    m_web_view_bridge->on_resize_window = [self](auto const& size) {
        auto frame = [[self window] frame];
        frame.size = Ladybird::gfx_size_to_ns_size(size);
        [[self window] setFrame:frame display:YES];

        return Ladybird::ns_size_to_gfx_size([[self window] frame].size);
    };

    m_web_view_bridge->on_maximize_window = [self]() {
        auto frame = [[NSScreen mainScreen] frame];
        [[self window] setFrame:frame display:YES];

        return Ladybird::ns_rect_to_gfx_rect([[self window] frame]);
    };

    m_web_view_bridge->on_minimize_window = [self]() {
        [[self window] setIsMiniaturized:YES];

        return Ladybird::ns_rect_to_gfx_rect([[self window] frame]);
    };

    m_web_view_bridge->on_fullscreen_window = [self]() {
        if (([[self window] styleMask] & NSWindowStyleMaskFullScreen) == 0) {
            [[self window] toggleFullScreen:nil];
        }

        return Ladybird::ns_rect_to_gfx_rect([[self window] frame]);
    };
}

- (Tab*)tab
{
    return (Tab*)[self window];
}

- (TabController*)tabController
{
    return (TabController*)[[self tab] windowController];
}

- (NSScrollView*)scrollView
{
    return (NSScrollView*)[self superview];
}

static void copy_text_to_clipboard(StringView text)
{
    auto* string = Ladybird::string_to_ns_string(text);

    auto* pasteBoard = [NSPasteboard generalPasteboard];
    [pasteBoard clearContents];
    [pasteBoard setString:string forType:NSPasteboardTypeString];
}

- (void)copy:(id)sender
{
    copy_text_to_clipboard(m_web_view_bridge->selected_text());
}

- (void)selectAll:(id)sender
{
    m_web_view_bridge->select_all();
}

- (void)takeVisibleScreenshot:(id)sender
{
    auto result = m_web_view_bridge->take_screenshot(WebView::ViewImplementation::ScreenshotType::Visible);
    (void)result; // FIXME: Display an error if this failed.
}

- (void)takeFullScreenshot:(id)sender
{
    auto result = m_web_view_bridge->take_screenshot(WebView::ViewImplementation::ScreenshotType::Full);
    (void)result; // FIXME: Display an error if this failed.
}

- (void)openLink:(id)sender
{
    m_web_view_bridge->on_link_click(m_context_menu_url, {}, 0);
}

- (void)openLinkInNewTab:(id)sender
{
    m_web_view_bridge->on_link_middle_click(m_context_menu_url, {}, 0);
}

- (void)copyLink:(id)sender
{
    copy_text_to_clipboard(m_context_menu_url.serialize());
}

- (void)copyImage:(id)sender
{
    auto* bitmap = m_context_menu_bitmap.bitmap();
    if (bitmap == nullptr) {
        return;
    }

    auto png = Gfx::PNGWriter::encode(*bitmap);
    if (png.is_error()) {
        return;
    }

    auto* data = [NSData dataWithBytes:png.value().data() length:png.value().size()];

    auto* pasteBoard = [NSPasteboard generalPasteboard];
    [pasteBoard clearContents];
    [pasteBoard setData:data forType:NSPasteboardTypePNG];
}

- (void)toggleMediaPlayState:(id)sender
{
    m_web_view_bridge->toggle_media_play_state();
}

- (void)toggleMediaMuteState:(id)sender
{
    m_web_view_bridge->toggle_media_mute_state();
}

- (void)toggleMediaControlsState:(id)sender
{
    m_web_view_bridge->toggle_media_controls_state();
}

- (void)toggleMediaLoopState:(id)sender
{
    m_web_view_bridge->toggle_media_loop_state();
}

#pragma mark - Properties

- (NSMenu*)page_context_menu
{
    if (!_page_context_menu) {
        _page_context_menu = [[NSMenu alloc] initWithTitle:@"Page Context Menu"];

        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Go Back"
                                                               action:@selector(navigateBack:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Go Forward"
                                                               action:@selector(navigateForward:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Reload"
                                                               action:@selector(reload:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[NSMenuItem separatorItem]];

        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy"
                                                               action:@selector(copy:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Select All"
                                                               action:@selector(selectAll:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[NSMenuItem separatorItem]];

        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Take Visible Screenshot"
                                                               action:@selector(takeVisibleScreenshot:)
                                                        keyEquivalent:@""]];
        [_page_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Take Full Screenshot"
                                                               action:@selector(takeFullScreenshot:)
                                                        keyEquivalent:@""]];
    }

    return _page_context_menu;
}

- (NSMenu*)link_context_menu
{
    if (!_link_context_menu) {
        _link_context_menu = [[NSMenu alloc] initWithTitle:@"Link Context Menu"];

        [_link_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open"
                                                               action:@selector(openLink:)
                                                        keyEquivalent:@""]];
        [_link_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open in New Tab"
                                                               action:@selector(openLinkInNewTab:)
                                                        keyEquivalent:@""]];
        [_link_context_menu addItem:[NSMenuItem separatorItem]];

        [_link_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy URL"
                                                               action:@selector(copyLink:)
                                                        keyEquivalent:@""]];
    }

    return _link_context_menu;
}

- (NSMenu*)image_context_menu
{
    if (!_image_context_menu) {
        _image_context_menu = [[NSMenu alloc] initWithTitle:@"Image Context Menu"];

        [_image_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Image"
                                                                action:@selector(openLink:)
                                                         keyEquivalent:@""]];
        [_image_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Image in New Tab"
                                                                action:@selector(openLinkInNewTab:)
                                                         keyEquivalent:@""]];
        [_image_context_menu addItem:[NSMenuItem separatorItem]];

        [_image_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy Image"
                                                                action:@selector(copyImage:)
                                                         keyEquivalent:@""]];
        [_image_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy Image URL"
                                                                action:@selector(copyLink:)
                                                         keyEquivalent:@""]];
    }

    return _image_context_menu;
}

- (NSMenu*)video_context_menu
{
    if (!_video_context_menu) {
        _video_context_menu = [[NSMenu alloc] initWithTitle:@"Video Context Menu"];

        auto* play_pause_menu_item = [[NSMenuItem alloc] initWithTitle:@"Play"
                                                                action:@selector(toggleMediaPlayState:)
                                                         keyEquivalent:@""];
        [play_pause_menu_item setTag:CONTEXT_MENU_PLAY_PAUSE_TAG];

        auto* mute_unmute_menu_item = [[NSMenuItem alloc] initWithTitle:@"Mute"
                                                                 action:@selector(toggleMediaMuteState:)
                                                          keyEquivalent:@""];
        [mute_unmute_menu_item setTag:CONTEXT_MENU_MUTE_UNMUTE_TAG];

        auto* controls_menu_item = [[NSMenuItem alloc] initWithTitle:@"Controls"
                                                              action:@selector(toggleMediaControlsState:)
                                                       keyEquivalent:@""];
        [controls_menu_item setTag:CONTEXT_MENU_CONTROLS_TAG];

        auto* loop_menu_item = [[NSMenuItem alloc] initWithTitle:@"Loop"
                                                          action:@selector(toggleMediaLoopState:)
                                                   keyEquivalent:@""];
        [loop_menu_item setTag:CONTEXT_MENU_LOOP_TAG];

        [_video_context_menu addItem:play_pause_menu_item];
        [_video_context_menu addItem:mute_unmute_menu_item];
        [_video_context_menu addItem:controls_menu_item];
        [_video_context_menu addItem:loop_menu_item];
        [_video_context_menu addItem:[NSMenuItem separatorItem]];

        [_video_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Video"
                                                                action:@selector(openLink:)
                                                         keyEquivalent:@""]];
        [_video_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Video in New Tab"
                                                                action:@selector(openLinkInNewTab:)
                                                         keyEquivalent:@""]];
        [_video_context_menu addItem:[NSMenuItem separatorItem]];

        [_video_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy Video URL"
                                                                action:@selector(copyLink:)
                                                         keyEquivalent:@""]];
    }

    return _video_context_menu;
}

- (NSTextField*)status_label
{
    if (!_status_label) {
        _status_label = [NSTextField labelWithString:@""];
        [_status_label setDrawsBackground:YES];
        [_status_label setBordered:YES];
        [_status_label setHidden:YES];

        [self addSubview:_status_label];
    }

    return _status_label;
}

#pragma mark - NSView

- (void)drawRect:(NSRect)rect
{
    auto paintable = m_web_view_bridge->paintable();
    if (!paintable.has_value()) {
        [super drawRect:rect];
        return;
    }

    auto [bitmap, bitmap_size] = *paintable;
    VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRA8888);

    static constexpr size_t BITS_PER_COMPONENT = 8;
    static constexpr size_t BITS_PER_PIXEL = 32;
    static constexpr size_t COMPONENTS_PER_PIXEL = 4;

    auto* context = [[NSGraphicsContext currentContext] CGContext];
    CGContextSaveGState(context);

    auto device_pixel_ratio = m_web_view_bridge->device_pixel_ratio();
    auto inverse_device_pixel_ratio = m_web_view_bridge->inverse_device_pixel_ratio();

    CGContextScaleCTM(context, inverse_device_pixel_ratio, inverse_device_pixel_ratio);

    auto* provider = CGDataProviderCreateWithData(nil, bitmap.scanline_u8(0), bitmap.size_in_bytes(), nil);
    auto image_rect = CGRectMake(rect.origin.x * device_pixel_ratio, rect.origin.y * device_pixel_ratio, bitmap_size.width(), bitmap_size.height());

    // Ideally, this would be NSBitmapImageRep, but the equivalent factory initWithBitmapDataPlanes: does
    // not seem to actually respect endianness. We need NSBitmapFormatThirtyTwoBitLittleEndian, but the
    // resulting image is always big endian. CGImageCreate actually does respect the endianness.
    auto* bitmap_image = CGImageCreate(
        bitmap_size.width(),
        bitmap_size.height(),
        BITS_PER_COMPONENT,
        BITS_PER_PIXEL,
        COMPONENTS_PER_PIXEL * bitmap.width(),
        CGColorSpaceCreateDeviceRGB(),
        kCGBitmapByteOrder32Little | kCGImageAlphaFirst,
        provider,
        nil,
        NO,
        kCGRenderingIntentDefault);

    auto* image = [[NSImage alloc] initWithCGImage:bitmap_image size:NSZeroSize];
    [image drawInRect:image_rect];

    CGContextRestoreGState(context);
    CGImageRelease(bitmap_image);

    [super drawRect:rect];
}

- (void)viewDidEndLiveResize
{
    [super viewDidEndLiveResize];
    [self handleResize];
}

- (BOOL)isFlipped
{
    // The origin of a NSScrollView is the lower-left corner, with the y-axis extending upwards. Instead,
    // we want the origin to be the top-left corner, with the y-axis extending downward.
    return YES;
}

- (void)mouseMoved:(NSEvent*)event
{
    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::None);
    m_web_view_bridge->mouse_move_event(position, button, modifiers);
}

- (void)mouseDown:(NSEvent*)event
{
    [[self window] makeFirstResponder:self];

    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Primary);

    if (event.clickCount % 2 == 0) {
        m_web_view_bridge->mouse_double_click_event(position, button, modifiers);
    } else {
        m_web_view_bridge->mouse_down_event(position, button, modifiers);
    }
}

- (void)mouseUp:(NSEvent*)event
{
    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Primary);
    m_web_view_bridge->mouse_up_event(position, button, modifiers);
}

- (void)mouseDragged:(NSEvent*)event
{
    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Primary);
    m_web_view_bridge->mouse_move_event(position, button, modifiers);
}

- (void)rightMouseDown:(NSEvent*)event
{
    [[self window] makeFirstResponder:self];

    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Secondary);

    if (event.clickCount % 2 == 0) {
        m_web_view_bridge->mouse_double_click_event(position, button, modifiers);
    } else {
        m_web_view_bridge->mouse_down_event(position, button, modifiers);
    }
}

- (void)rightMouseUp:(NSEvent*)event
{
    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Secondary);
    m_web_view_bridge->mouse_up_event(position, button, modifiers);
}

- (void)rightMouseDragged:(NSEvent*)event
{
    auto [position, button, modifiers] = Ladybird::ns_event_to_mouse_event(event, self, GUI::MouseButton::Secondary);
    m_web_view_bridge->mouse_move_event(position, button, modifiers);
}

- (void)keyDown:(NSEvent*)event
{
    auto [key_code, modifiers, code_point] = Ladybird::ns_event_to_key_event(event);
    m_web_view_bridge->key_down_event(key_code, modifiers, code_point);
}

- (void)keyUp:(NSEvent*)event
{
    auto [key_code, modifiers, code_point] = Ladybird::ns_event_to_key_event(event);
    m_web_view_bridge->key_up_event(key_code, modifiers, code_point);
}

@end
