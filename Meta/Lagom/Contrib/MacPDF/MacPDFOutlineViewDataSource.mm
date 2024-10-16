/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFOutlineViewDataSource.h"

@interface OutlineItemWrapper ()
{
    // Only one of those two is set.
    RefPtr<PDF::OutlineItem> _item;
    NSString* _groupName;
}
@end

@implementation OutlineItemWrapper
- (instancetype)initWithItem:(NonnullRefPtr<PDF::OutlineItem>)item
{
    if (self = [super init]; !self)
        return nil;
    _item = move(item);
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

- (OutlineItemWrapper*)child:(NSInteger)index
{
    return [[OutlineItemWrapper alloc] initWithItem:_item->children[index]];
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

    auto title_view = _item->title.bytes_as_string_view();
    NSData* title_data = [NSData dataWithBytes:title_view.characters_without_null_termination() length:title_view.length()];
    NSString* title = [[NSString alloc] initWithData:title_data encoding:NSUTF8StringEncoding];

    // Newlines confuse NSOutlineView, at least in sidebar style (even with `usesSingleLineMode` set to YES on the cell view's text field).
    title = [[title componentsSeparatedByCharactersInSet:[NSCharacterSet newlineCharacterSet]] componentsJoinedByString:@" "];

    return title;
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

    if (index == 0) {
        bool has_outline = _outline && !_outline->children.is_empty();
        // FIXME: Maybe put filename here instead?
        return [[OutlineItemWrapper alloc] initWithGroupName:has_outline ? @"Outline" : @"(No outline)"];
    }
    return [[OutlineItemWrapper alloc] initWithItem:_outline->children[index - 1]];
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

@end
