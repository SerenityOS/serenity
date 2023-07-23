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

@interface LagomPDFView : NSView
{
}
@end
