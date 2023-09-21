//
//  LagomPDFDocument.h
//  SerenityPDF
//
//  Created by Nico Weber on 9/21/23.
//

// Several AK types conflict with MacOS types.
#define FixedPoint FixedPointMacOS
#import <Cocoa/Cocoa.h>
#undef FixedPoint

NS_ASSUME_NONNULL_BEGIN

@interface LagomPDFDocument : NSDocument

@end

NS_ASSUME_NONNULL_END
