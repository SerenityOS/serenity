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
    _pdfView.identifier = @"PDFView"; // To make state restoration work.
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

    _outlineView.floatsGroupRows = NO;
    _outlineView.focusRingType = NSFocusRingTypeNone;
    _outlineView.headerView = nil;

    // FIXME: Implement data source support for autosaveExpandedItems and use that.

    // rowSizeStyle does not default to NSTableViewRowSizeStyleDefault, but needs to be set to it for outline views in sourcelist style.
    _outlineView.rowSizeStyle = NSTableViewRowSizeStyleDefault;

    NSTableColumn* column = [[NSTableColumn alloc] initWithIdentifier:@"OutlineColumn"];
    column.editable = NO;
    [_outlineView addTableColumn:column];

    NSScrollView* scrollView = [[NSScrollView alloc] initWithFrame:NSZeroRect];
    scrollView.hasVerticalScroller = YES;
    scrollView.drawsBackground = NO;
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

- (IBAction)goToNextPage:(id)sender
{
    [_pdfView goToNextPage:sender];
}

- (IBAction)goToPreviousPage:(id)sender
{
    [_pdfView goToPreviousPage:sender];
}

- (BOOL)validateMenuItem:(NSMenuItem*)item
{
    if ([_pdfView validateMenuItem:item])
        return YES;
    if (item.action == @selector(showGoToPageDialog:))
        return _pdfDocument.pdf ? YES : NO;
    return NO;
}

- (IBAction)toggleShowClippingPaths:(id)sender
{
    [_pdfView toggleShowClippingPaths:sender];
}

- (IBAction)toggleClipImages:(id)sender
{
    [_pdfView toggleClipImages:sender];
}

- (IBAction)toggleClipPaths:(id)sender
{
    [_pdfView toggleClipPaths:sender];
}

- (IBAction)toggleClipText:(id)sender
{
    [_pdfView toggleClipText:sender];
}

- (IBAction)toggleShowImages:(id)sender
{
    [_pdfView toggleShowImages:sender];
}

- (IBAction)toggleShowHiddenText:(id)sender
{
    [_pdfView toggleShowHiddenText:sender];
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

- (BOOL)outlineView:(NSOutlineView*)outlineView isGroupItem:(id)item
{
    return [item isGroupItem];
}

- (BOOL)outlineView:(NSOutlineView*)outlineView shouldSelectItem:(id)item
{
    return ![self outlineView:outlineView isGroupItem:item];
}

// "This method is required if you wish to turn on the use of NSViews instead of NSCells."
- (NSView*)outlineView:(NSOutlineView*)outlineView viewForTableColumn:(NSTableColumn*)tableColumn item:(id)item
{
    // "The implementation of this method will usually call -[tableView makeViewWithIdentifier:[tableColumn identifier] owner:self]
    //  in order to reuse a previous view, or automatically unarchive an associated prototype view for that identifier."

    // Figure 1-5 in "Understanding Table Views" at
    // https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/TableView/TableViewOverview/TableViewOverview.html
    // describes what makeViewWithIdentifier:owner: does: It tries to cache views, so that if an item scrolls out of view
    // and then back in again, the old view can be reused, without having to allocate a new one.
    // It also tries to load the view from a xib if it doesn't exist. We don't use a xib though, so we have
    // to create the view in code if it's not already cached.

    // After calling this method to create a view, the framework assigns its objectValue to what's
    // returned by outlineView:objectValueForTableColumn:byItem: from the data source.
    // NSTableCellView implements objectValue, but it doesn't do anything with it. We have to manually
    // bind assignment to its objectValue field to update concrete views.
    // This is done here using Cocoa bindings.
    // Alternatively, we could also get the data from the data model directly and assign it to
    // the text field's stringValue, but then we'd call outlineView:objectValueForTableColumn:byItem:
    // twice, and this somewhat roundabout method here seems to be how the framework wants to be used.

    NSTableCellView* cellView = [outlineView makeViewWithIdentifier:tableColumn.identifier owner:self];
    if (!cellView) {
        cellView = [[NSTableCellView alloc] init];
        cellView.identifier = tableColumn.identifier;

        NSTextField* textField = [NSTextField labelWithString:@""];
        textField.lineBreakMode = NSLineBreakByTruncatingTail;
        textField.allowsExpansionToolTips = YES;

        // https://stackoverflow.com/a/29725553/551986
        // "If your cell view is an NSTableCellView, that class also responds to -setObjectValue:. [...]
        //  However, an NSTableCellView does not inherently do anything with the object value. It just holds it.
        //  What you can then do is have the subviews bind to it through the objectValue property."
        [textField bind:@"objectValue" toObject:cellView withKeyPath:@"objectValue" options:nil];

        [cellView addSubview:textField];
        cellView.textField = textField;
    }

    return cellView;
}

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
