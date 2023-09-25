//
//  PDFView.h
//  SerenityPDF
//
//  Created by Nico Weber on 7/22/23.
//

#pragma once

// Several AK types conflict with MacOS types.
#define FixedPoint FixedPointMacOS
#import <Cocoa/Cocoa.h>
#undef FixedPoint

#include <AK/WeakPtr.h>
#include <LibPDF/Document.h>

@protocol LagomPDFViewDelegate
- (void)pageChanged;
@end

@interface LagomPDFView : NSView

- (void)setDocument:(WeakPtr<PDF::Document>)doc;
- (void)goToPage:(int)page;
- (int)page;

- (void)setDelegate:(id<LagomPDFViewDelegate>)delegate;

@end
