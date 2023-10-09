/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFWindowController.h"

#import "MacPDFDocument.h"
#import "MacPDFOutlineViewDataSource.h"

@interface MacPDFWindowController ()
{
    MacPDFDocument* _pdfDocument;
    IBOutlet MacPDFView* _pdfView;

    MacPDFOutlineViewDataSource* _outlineDataSource;
    NSOutlineView* _outlineView;
}
@end

@implementation MacPDFWindowController

- (instancetype)initWithDocument:(MacPDFDocument*)document
{
    auto const style_mask = NSWindowStyleMaskTitled | NSWindowStyleMaskClosable | NSWindowStyleMaskMiniaturizable | NSWindowStyleMaskResizable | NSWindowStyleMaskFullSizeContentView;
    NSWindow* window = [[NSWindow alloc] initWithContentRect:NSMakeRect(0, 0, 600, 800)
                                                   styleMask:style_mask
                                                     backing:NSBackingStoreBuffered
                                                       defer:YES];

    if (self = [super initWithWindow:window]; !self)
        return nil;

    _pdfView = [[MacPDFView alloc] initWithFrame:NSZeroRect];
    [_pdfView setDelegate:self];

    NSSplitViewController* split_view = [[NSSplitViewController alloc] initWithNibName:nil bundle:nil];
    [split_view addSplitViewItem:[self makeSidebarSplitItem]];
    [split_view addSplitViewItem:[NSSplitViewItem splitViewItemWithViewController:[self viewControllerForView:_pdfView]]];

    // Autosave if the sidebar is open or not, and how far.
    // autosaveName only works if identifier is set too.
    // identifier docs: "For programmatically created views, you typically set this value
    // after creating the item but before adding it to a window. [...] For views and controls
    // in a window, the value you specify for this string must be unique on a per-window basis."
    split_view.splitView.autosaveName = @"MacPDFSplitView";
    split_view.splitView.identifier = @"MacPDFSplitViewId";

    window.contentViewController = split_view;

    NSToolbar* toolbar = [[NSToolbar alloc] initWithIdentifier:@"MacPDFToolbar"];
    toolbar.delegate = self;
    toolbar.displayMode = NSToolbarDisplayModeIconOnly;
    [window setToolbar:toolbar];

    _pdfDocument = document;
    return self;
}

- (NSViewController*)viewControllerForView:(NSView*)view
{
    NSViewController* view_controller = [[NSViewController alloc] initWithNibName:nil bundle:nil];
    view_controller.view = view;
    return view_controller;
}

- (NSSplitViewItem*)makeSidebarSplitItem
{
    _outlineView = [[NSOutlineView alloc] initWithFrame:NSZeroRect];

    _outlineView.style = NSTableViewStyleSourceList;
    _outlineView.focusRingType = NSFocusRingTypeNone;

    // FIXME: Implement data source support for autosaveExpandedItems and use that.

    // rowSizeStyle does not default to NSTableViewRowSizeStyleDefault, but needs to be set to it for outline views in sourcelist style.
    _outlineView.rowSizeStyle = NSTableViewRowSizeStyleDefault;

    NSTableColumn* column = [[NSTableColumn alloc] initWithIdentifier:@"OutlineColumn"];
    column.editable = NO;
    [_outlineView addTableColumn:column];

    NSScrollView* scrollView = [[NSScrollView alloc] initWithFrame:NSZeroRect];
    scrollView.hasVerticalScroller = YES;
    scrollView.documentView = _outlineView;

    // The scroll view knows to put things only in the safe area, but it doesn't clip to it.
    // So momentum scrolling would let things draw above it, which looks weird.
    // Put the scroll view in a containing view and make the containing view limit the scroll view to
    // the safe area, so that it gets clipped.
    NSView* view = [[NSView alloc] initWithFrame:NSZeroRect];
    [view addSubview:scrollView];

    [scrollView.topAnchor constraintEqualToAnchor:view.safeAreaLayoutGuide.topAnchor].active = YES;
    [scrollView.leftAnchor constraintEqualToAnchor:view.safeAreaLayoutGuide.leftAnchor].active = YES;
    [scrollView.rightAnchor constraintEqualToAnchor:view.safeAreaLayoutGuide.rightAnchor].active = YES;
    [scrollView.bottomAnchor constraintEqualToAnchor:view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    scrollView.translatesAutoresizingMaskIntoConstraints = NO;

    NSSplitViewItem* item = [NSSplitViewItem sidebarWithViewController:[self viewControllerForView:view]];
    item.collapseBehavior = NSSplitViewItemCollapseBehaviorPreferResizingSplitViewWithFixedSiblings;

    // This only has an effect on the very first run.
    // Later, the collapsed state is loaded from the sidebar's autosave data.
    item.collapsed = YES;

    return item;
}

- (void)pdfDidInitialize
{
    [_pdfView setDocument:_pdfDocument.pdf->make_weak_ptr()];
    [self pageChanged];

    // FIXME: Only set data source when sidebar is open.
    _outlineDataSource = [[MacPDFOutlineViewDataSource alloc] initWithOutline:_pdfDocument.pdf->outline()];
    _outlineView.dataSource = _outlineDataSource;
    _outlineView.delegate = self;
}

- (IBAction)showGoToPageDialog:(id)sender
{
    auto alert = [[NSAlert alloc] init];
    alert.messageText = @"Page Number";
    [alert addButtonWithTitle:@"Go"];
    [alert addButtonWithTitle:@"Cancel"];

    auto textField = [[NSTextField alloc] initWithFrame:NSMakeRect(0, 0, 100, 24)];
    NSNumberFormatter* formatter = [[NSNumberFormatter alloc] init];
    formatter.numberStyle = NSNumberFormatterNoStyle; // Integers only.
    [textField setFormatter:formatter];
    [textField setIntValue:[_pdfView page]];

    alert.accessoryView = textField;
    alert.window.initialFirstResponder = textField;

    [alert beginSheetModalForWindow:self.window
                  completionHandler:^(NSModalResponse response) {
                      if (response == NSAlertFirstButtonReturn)
                          [self->_pdfView goToPage:[textField intValue]];
                  }];
}

#pragma mark - MacPDFViewDelegate

- (void)pageChanged
{
    [self.window setSubtitle:
                     [NSString stringWithFormat:@"Page %d of %d", [_pdfView page], _pdfDocument.pdf->get_page_count()]];
}

#pragma mark - NSToolbarDelegate

- (NSArray<NSToolbarItemIdentifier>*)toolbarAllowedItemIdentifiers:(NSToolbar*)toolbar
{
    return [self toolbarDefaultItemIdentifiers:toolbar];
}

- (NSArray<NSToolbarItemIdentifier>*)toolbarDefaultItemIdentifiers:(NSToolbar*)toolbar
{
    // NSToolbarToggleSidebarItemIdentifier sends toggleSidebar: along the responder chain,
    // which NSSplitViewController conveniently implements.
    return @[
        NSToolbarToggleSidebarItemIdentifier,
        NSToolbarSidebarTrackingSeparatorItemIdentifier,
    ];
}

- (NSToolbarItem*)toolbar:(NSToolbar*)toolbar
        itemForItemIdentifier:(NSToolbarItemIdentifier)itemIdentifier
    willBeInsertedIntoToolbar:(BOOL)flag
{
    // Not called for standard identifiers, but the implementation of the method must exist, or else:
    // ERROR: invalid delegate <MacPDFWindowController: 0x600003054c80> (does not implement all required methods)
    return nil;
}

#pragma mark - NSOutlineViewDelegate

- (void)outlineViewSelectionDidChange:(NSNotification*)notification
{
    NSInteger row = _outlineView.selectedRow;
    if (row == -1)
        return;

    OutlineItemWrapper* item = [_outlineView itemAtRow:row];
    if (auto page = [item page]; page.has_value())
        [_pdfView goToPage:page.value()];
}

@end
