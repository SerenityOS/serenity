/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFOutlineViewDataSource.h"

@interface OutlineItemWrapper ()
{
    // Either _groupName or the other fields are set.
    NSString* _groupName;

    RefPtr<PDF::OutlineItem> _item;
    NSInteger _index;
    __weak OutlineItemWrapper* _parent;
}
@end

@implementation OutlineItemWrapper
- (instancetype)initWithItem:(NonnullRefPtr<PDF::OutlineItem>)item index:(NSInteger)index parent:(OutlineItemWrapper*)parent
{
    if (self = [super init]; !self)
        return nil;
    _item = move(item);
    _index = index;
    _parent = parent;
    _groupName = nil;
    return self;
}

- (instancetype)initWithGroupName:(nonnull NSString*)groupName
{
    if (self = [super init]; !self)
        return nil;
    _groupName = groupName;
    return self;
}

- (BOOL)isGroupItem
{
    return _groupName != nil;
}

- (Optional<u32>)page
{
    if ([self isGroupItem])
        return {};
    return _item->dest.page.map([](u32 page_index) { return page_index + 1; });
}

- (NonnullRefPtr<PDF::OutlineItem>)child:(NSInteger)index
{
    return _item->children[index];
}

- (NSInteger)numberOfChildren
{
    if ([self isGroupItem])
        return 0;
    return _item->children.size();
}

- (NSString*)objectValue
{
    if (_groupName)
        return _groupName;
    return [NSString stringWithFormat:@"%s", _item->title.characters()]; // FIXME: encoding?
}

- (NSInteger)index
{
    return _index;
}

- (OutlineItemWrapper*)parent
{
    return _parent;
}
@end

@interface MacPDFOutlineViewDataSource ()
{
    RefPtr<PDF::OutlineDict> _outline;
    HashMap<PDF::OutlineItem*, OutlineItemWrapper*> _wrappers;
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
    if (item) {
        auto item_wrapper = (OutlineItemWrapper*)item;
        return _wrappers.ensure([item_wrapper child:index].ptr(), [&]() {
            return [[OutlineItemWrapper alloc] initWithItem:[item_wrapper child:index] index:index parent:item_wrapper];
        });
    }

    if (index == 0) {
        return _wrappers.ensure(nullptr, [&]() {
            bool has_outline = _outline && !_outline->children.is_empty();
            // FIXME: Maybe put filename here instead?
            return [[OutlineItemWrapper alloc] initWithGroupName:has_outline ? @"Outline" : @"(No outline)"];
        });
    }

    return _wrappers.ensure(_outline->children[index - 1].ptr(), [&]() {
        return [[OutlineItemWrapper alloc] initWithItem:_outline->children[index - 1] index:index - 1 parent:nil];
    });
}

- (BOOL)outlineView:(NSOutlineView*)outlineView isItemExpandable:(id)item
{
    return [self outlineView:outlineView numberOfChildrenOfItem:item] > 0;
}

- (NSInteger)outlineView:(NSOutlineView*)outlineView numberOfChildrenOfItem:(nullable id)item
{
    if (item)
        return [(OutlineItemWrapper*)item numberOfChildren];

    return 1 + (_outline ? _outline->children.size() : 0);
}

- (id)outlineView:(NSOutlineView*)outlineView objectValueForTableColumn:(nullable NSTableColumn*)tableColumn byItem:(nullable id)item
{
    return [(OutlineItemWrapper*)item objectValue];
}

- (id)outlineView:(NSOutlineView*)outlineView itemForPersistentObject:(id)object
{
    OutlineItemWrapper* outline_item = nil;
    for (NSNumber* number in object) {
        NSInteger index = number.integerValue;
        if (!outline_item)
            ++index;
        if (index >= [self outlineView:outlineView numberOfChildrenOfItem:outline_item])
            return nil;
        outline_item = [self outlineView:outlineView child:index ofItem:outline_item];
    }
    return outline_item;
}

- (id)outlineView:(NSOutlineView*)outlineView persistentObjectForItem:(id)item
{
    NSMutableArray* array = [@[] mutableCopy];
    OutlineItemWrapper* outline_item = item;
    while (outline_item) {
        [array addObject:@(outline_item.index)];
        outline_item = outline_item.parent;
    }
    return [[array reverseObjectEnumerator] allObjects];
}

@end
