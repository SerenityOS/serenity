//
//  AppDelegate.h
//  SerenityPDF
//
//  Created by Nico Weber on 7/22/23.
//

// Several AK types conflict with MacOS types.
#define FixedPoint FixedPointMacOS
#import <Cocoa/Cocoa.h>
#undef FixedPoint

@interface AppDelegate : NSObject <NSApplicationDelegate>

@end
