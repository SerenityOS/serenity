/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFWindowController.h"

#import "MacPDFDocument.h"

@interface MacPDFWindowController ()
{
    MacPDFDocument* _pdfDocument;
    IBOutlet MacPDFView* _pdfView;
}
@end

@implementation MacPDFWindowController

- (instancetype)initWithDocument:(MacPDFDocument*)document
{
    if (self = [super initWithWindowNibName:@"MacPDFDocument" owner:self]; !self)
        return nil;

    _pdfDocument = document;
    return self;
}

- (void)windowDidLoad
{
    [super windowDidLoad];
    [_pdfView setDelegate:self];
    [_pdfDocument windowIsReady];
}

- (void)pdfDidInitialize
{
    [_pdfView setDocument:_pdfDocument.pdf->make_weak_ptr()];
    [self pageChanged];
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

    NSWindow* window = _pdfView.window;
    [alert beginSheetModalForWindow:window
                  completionHandler:^(NSModalResponse response) {
                      if (response == NSAlertFirstButtonReturn)
                          [self->_pdfView goToPage:[textField intValue]];
                  }];
}

#pragma mark - MacPDFViewDelegate

- (void)pageChanged
{
    [_pdfView.window setSubtitle:
                     [NSString stringWithFormat:@"Page %d of %d", [_pdfView page], _pdfDocument.pdf->get_page_count()]];
}

@end
