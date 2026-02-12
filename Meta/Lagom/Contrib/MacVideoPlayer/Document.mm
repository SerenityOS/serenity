/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "Document.h"

#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>

#include <LibMedia/PlaybackManager.h>

@interface Document ()
{
    NSData* _data; // Strong, _doc refers to it.
    OwnPtr<Media::PlaybackManager> _playbackManager;
}
@end

@implementation Document

- (instancetype)init
{
    self = [super init];
    if (self) {
        // Add your subclass-specific initialization here.
    }
    return self;
}

+ (BOOL)autosavesInPlace
{
    return YES;
}

- (NSString*)windowNibName
{
    // Override returning the nib file name of the document
    // If you need to use a subclass of NSWindowController or if your document supports multiple NSWindowControllers, you should remove this method and override -makeWindowControllers instead.
    return @"Document";
}

- (void)windowControllerDidLoadNib:(NSWindowController*)aController
{
    [super windowControllerDidLoadNib:aController];
    if (_playbackManager)
        [_view setPlaybackManager:_playbackManager.ptr()];
}

- (NSData*)dataOfType:(NSString*)typeName error:(NSError**)outError
{
    // Insert code here to write your document to data of the specified type. If outError != NULL, ensure that you create and set an appropriate error if you return nil.
    // Alternatively, you could remove this method and override -fileWrapperOfType:error:, -writeToURL:ofType:error:, or -writeToURL:ofType:forSaveOperation:originalContentsURL:error: instead.
    [NSException raise:@"UnimplementedMethod" format:@"%@ is unimplemented", NSStringFromSelector(_cmd)];
    return nil;
}

- (BOOL)readFromData:(NSData*)data ofType:(NSString*)typeName error:(NSError**)outError
{
    if (![[UTType typeWithIdentifier:typeName] conformsToType:UTTypeMovie]) {
        if (outError) {
            *outError = [NSError errorWithDomain:NSOSStatusErrorDomain
                                            code:unimpErr
                                        userInfo:nil];
        }
        return NO;
    }

    auto manager_or = Media::PlaybackManager::from_data(ReadonlyBytes { [data bytes], [data length] });
    if (manager_or.is_error()) {
        auto description = manager_or.error().description();
        NSLog(@"failed to load: %.*s", (int)description.length(), description.characters_without_null_termination());
        if (outError) {
            *outError = [NSError errorWithDomain:NSOSStatusErrorDomain
                                            code:unimpErr
                                        userInfo:nil];
        }
        return NO;
    }

    _playbackManager = manager_or.release_value();
    _data = data;
    return YES;
}

- (BOOL)isEntireFileLoaded
{
    return NO;
}

@end
