/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <bits/search.h>
#include <search.h>

struct search_tree_node* new_tree_node(void const* key)
{
    auto* node = static_cast<struct search_tree_node*>(malloc(sizeof(struct search_tree_node)));

    if (!node)
        return nullptr;

    node->key = key;
    node->left = nullptr;
    node->right = nullptr;

    return node;
}

void delete_node_recursive(struct search_tree_node* node)
{
    if (!node)
        return;

    delete_node_recursive(node->left);
    delete_node_recursive(node->right);

    free(node);
}

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tsearch.html
void* tsearch(void const* key, void** rootp, int (*comparator)(void const*, void const*))
{
    if (!rootp)
        return nullptr;

    if (!*rootp) {
        *rootp = new_tree_node(key);
        return *rootp;
    }

    auto node = static_cast<struct search_tree_node*>(*rootp);

    while (node != nullptr) {
        auto comp = comparator(key, node->key);

        if (comp < 0 && node->left) {
            node = node->left;
        } else if (comp < 0 && !node->left) {
            node->left = new_tree_node(key);
            return node->left;
        } else if (comp > 0 && node->right) {
            node = node->right;
        } else if (comp > 0 && !node->right) {
            node->right = new_tree_node(key);
            return node->right;
        } else {
            return node;
        }
    }

    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tfind.html
void* tfind(void const* key, void* const* rootp, int (*comparator)(void const*, void const*))
{
    if (!rootp)
        return nullptr;

    auto node = static_cast<struct search_tree_node*>(*rootp);

    while (node != nullptr) {
        auto comp = comparator(key, node->key);

        if (comp < 0)
            node = node->left;
        else if (comp > 0)
            node = node->right;
        else
            return node;
    }

    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tdelete.html
void* tdelete(void const*, void**, int (*)(void const*, void const*))
{
    dbgln("FIXME: Implement tdelete()");
    return nullptr;
}

static void twalk_internal(const struct search_tree_node* node, void (*action)(void const*, VISIT, int), int depth)
{
    if (!node)
        return;

    if (!node->right && !node->left) {
        action(node, leaf, depth);
        return;
    }

    action(node, preorder, depth);
    twalk_internal(node->left, action, depth + 1);
    action(node, postorder, depth);
    twalk_internal(node->right, action, depth + 1);
    action(node, endorder, depth);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/twalk.html
void twalk(void const* rootp, void (*action)(void const*, VISIT, int))
{
    auto node = static_cast<const struct search_tree_node*>(rootp);

    twalk_internal(node, action, 0);
}
}
