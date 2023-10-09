/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFOutlineViewDataSource.h"

@interface OutlineItemWrapper ()
{
    RefPtr<PDF::OutlineItem> _item;
}
@end

@implementation OutlineItemWrapper
- (instancetype)initWithItem:(NonnullRefPtr<PDF::OutlineItem>)item
{
    if (self = [super init]; !self)
        return nil;
    _item = move(item);
    return self;
}

- (Optional<u32>)page
{
    return _item->dest.page.map([](u32 page_index) { return page_index + 1; });
}

- (OutlineItemWrapper*)child:(NSInteger)index
{
    return [[OutlineItemWrapper alloc] initWithItem:_item->children[index]];
}

- (NSInteger)numberOfChildren
{
    return _item->children.size();
}

- (NSString*)objectValue
{
    return [NSString stringWithFormat:@"%s", _item->title.characters()]; // FIXME: encoding?
}
@end

@interface MacPDFOutlineViewDataSource ()
{
    RefPtr<PDF::OutlineDict> _outline;
}
@end

@implementation MacPDFOutlineViewDataSource

- (instancetype)initWithOutline:(RefPtr<PDF::OutlineDict>)outline
{
    if (self = [super init]; !self)
        return nil;
    _outline = move(outline);
    return self;
}

#pragma mark - NSOutlineViewDataSource

- (id)outlineView:(NSOutlineView*)outlineView child:(NSInteger)index ofItem:(nullable id)item
{
    if (item)
        return [(OutlineItemWrapper*)item child:index];

    return [[OutlineItemWrapper alloc] initWithItem:_outline->children[index]];
}

- (BOOL)outlineView:(NSOutlineView*)outlineView isItemExpandable:(id)item
{
    return [self outlineView:outlineView numberOfChildrenOfItem:item] > 0;
}

- (NSInteger)outlineView:(NSOutlineView*)outlineView numberOfChildrenOfItem:(nullable id)item
{
    if (item)
        return [(OutlineItemWrapper*)item numberOfChildren];

    return _outline ? _outline->children.size() : 0;
}

- (id)outlineView:(NSOutlineView*)outlineView objectValueForTableColumn:(nullable NSTableColumn*)tableColumn byItem:(nullable id)item
{
    return [(OutlineItemWrapper*)item objectValue];
}

@end
