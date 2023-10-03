/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFDocument.h"

#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>

#import "MacPDFWindowController.h"
#include <LibPDF/Document.h>

@interface MacPDFDocument ()
{
    NSData* _data; // Strong, _doc refers to it.
    RefPtr<PDF::Document> _doc;
    MacPDFWindowController* _windowController;
}
@end

@implementation MacPDFDocument

- (PDF::Document*)pdf
{
    return _doc;
}

- (void)promptForPassword:(NSWindow*)window
{
    auto alert = [[NSAlert alloc] init];
    alert.messageText = @"Password";
    [alert addButtonWithTitle:@"OK"];
    [alert addButtonWithTitle:@"Cancel"];

    auto textField = [[NSSecureTextField alloc] initWithFrame:NSMakeRect(0, 0, 200, 24)];
    alert.accessoryView = textField;
    alert.window.initialFirstResponder = textField;

    // Without this, the window's not visible yet and the sheet can't attach to it.
    // FIXME: This causes the window to change position after restoring, so this isn't quite right either.
    //        Probably nicest to put the password prompt right in the window, instead of in a sheet.
    [window orderFront:self];

    [alert beginSheetModalForWindow:window
                  completionHandler:^(NSModalResponse response) {
                      if (response == NSAlertFirstButtonReturn) {
                          NSString* password = [textField stringValue];
                          StringView password_view { [password UTF8String], strlen([password UTF8String]) };
                          if (!self->_doc->security_handler()->try_provide_user_password(password_view)) {
                              warnln("invalid password '{}'", password);
                              [self performSelector:@selector(promptForPassword:) withObject:window];
                              return;
                          }
                          [self initializePDF];
                      } else if (response == NSAlertSecondButtonReturn) {
                          [self close];
                      }
                  }];
}

- (PDF::PDFErrorOr<NonnullRefPtr<PDF::Document>>)load:(NSData*)data
{
    // Runs on background thread, can't interact with UI.
    auto document = TRY(PDF::Document::create(ReadonlyBytes { [data bytes], [data length] }));
    return document;
}

- (void)initializePDF
{
    // FIXME: on background thread?
    if (auto err = _doc->initialize(); err.is_error()) {
        // FIXME: show error?
        NSLog(@"failed to load 2: %@", @(err.error().message().characters()));
    } else {
        [_windowController pdfDidInitialize];
    }
}

- (void)makeWindowControllers
{
    _windowController = [[MacPDFWindowController alloc] initWithDocument:self];
    [self addWindowController:_windowController];
    [self windowIsReady];
}

- (void)windowIsReady
{
    if (_doc) {
        if (auto handler = _doc->security_handler(); handler && !handler->has_user_password()) {
            [self promptForPassword:_windowController.window];
            return;
        }
        [self initializePDF];
    }
}

- (NSData*)dataOfType:(NSString*)typeName error:(NSError**)outError
{
    if (outError) {
        *outError = [NSError errorWithDomain:NSOSStatusErrorDomain code:unimpErr userInfo:nil];
    }
    return nil;
}

- (BOOL)readFromData:(NSData*)data ofType:(NSString*)typeName error:(NSError**)outError
{
    if (![[UTType typeWithIdentifier:typeName] conformsToType:UTTypePDF]) {
        if (outError) {
            *outError = [NSError errorWithDomain:NSOSStatusErrorDomain
                                            code:unimpErr
                                        userInfo:nil];
        }
        return NO;
    }

    if (auto doc_or = [self load:data]; !doc_or.is_error()) {
        _doc = doc_or.value();
        _data = data;
        return YES;
    } else {
        NSLog(@"failed to load: %@", @(doc_or.error().message().characters()));
        if (outError) {
            *outError = [NSError errorWithDomain:NSOSStatusErrorDomain
                                            code:unimpErr
                                        userInfo:nil];
        }
        return NO;
    }
}

+ (BOOL)autosavesInPlace
{
    return YES;
}

+ (BOOL)canConcurrentlyReadDocumentsOfType:(NSString*)typeName
{
    // Run readFromData:ofType:error: on background thread:
    return YES;
}

- (BOOL)isEntireFileLoaded
{
    return NO;
}

@end
