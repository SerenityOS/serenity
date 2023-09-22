//
//  LagomPDFDocument.m
//  SerenityPDF
//
//  Created by Nico Weber on 9/21/23.
//

#import "LagomPDFDocument.h"

#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>

#include <LibPDF/Document.h>

@interface LagomPDFDocument ()
{
    NSData* _data; // Strong, _doc refers to it.
    RefPtr<PDF::Document> _doc;
}
@end

@implementation LagomPDFDocument

- (PDF::PDFErrorOr<NonnullRefPtr<PDF::Document>>)load:(NSData*)data
{
    auto document = TRY(PDF::Document::create(ReadonlyBytes { [data bytes], [data length] }));
    TRY(document->initialize());
    return document;
}

- (NSString*)windowNibName
{
    // Override to return the nib file name of the document.
    // If you need to use a subclass of NSWindowController or if your document supports multiple NSWindowControllers, you should remove this method and override -makeWindowControllers instead.
    return @"LagomPDFDocument";
}

- (void)windowControllerDidLoadNib:(NSWindowController*)aController
{
    [super windowControllerDidLoadNib:aController];

    if (_doc) {
        [_pdfView setDocument:_doc->make_weak_ptr()];
    }
}

- (NSData*)dataOfType:(NSString*)typeName error:(NSError**)outError
{
    // Insert code here to write your document to data of the specified type. If outError != NULL, ensure that you create and set an appropriate error if you return nil.
    // Alternatively, you could remove this method and override -fileWrapperOfType:error:, -writeToURL:ofType:error:, or -writeToURL:ofType:forSaveOperation:originalContentsURL:error: instead.
    if (outError) {
        *outError = [NSError errorWithDomain:NSOSStatusErrorDomain code:unimpErr userInfo:nil];
    }
    return nil;
}

- (BOOL)readFromData:(NSData*)data ofType:(NSString*)typeName error:(NSError**)outError
{
    // Insert code here to read your document from the given data of the specified type. If outError != NULL, ensure that you create and set an appropriate error if you return NO.
    // Alternatively, you could remove this method and override -readFromFileWrapper:ofType:error: or -readFromURL:ofType:error: instead.
    // If you do, you should also override -isEntireFileLoaded to return NO if the contents are lazily loaded.

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
        //        [self pageChanged];

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

@end
