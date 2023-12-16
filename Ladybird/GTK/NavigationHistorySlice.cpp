/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NavigationHistorySlice.h"
#include "HistoryEntry.h"
#include "NavigationHistory.h"

struct _LadybirdNavigationHistorySlice {
    GObject parent_instance;

    LadybirdNavigationHistory* history;
    size_t position;
    bool back;
};

enum {
    PROP_0,
    PROP_ITEM_TYPE,
    PROP_N_ITEMS,
    PROP_HISTORY,
    PROP_BACK,
    NUM_PROPS
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

static void ladybird_navigation_history_slice_init_list_model(GListModelInterface* iface);

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdNavigationHistorySlice, ladybird_navigation_history_slice, G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE(G_TYPE_LIST_MODEL, ladybird_navigation_history_slice_init_list_model))

static void items_changed(LadybirdNavigationHistorySlice* self, guint changed_position, guint removed, guint added)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY_SLICE(self));

    // Cheat: we *know* that the only time LadybirdNavigationHistory emits items-changed
    // is from ladybird_navigation_history_push(). So we can greatly simplify the logic
    // here.
    g_return_if_fail(added == 1);
    g_return_if_fail((changed_position == 0 && self->position == 0) || (changed_position == self->position + 1));

    // Neutralize the handler below, we're going to handle everything here.
    size_t old_position = self->position;
    self->position = ladybird_navigation_history_get_current_position(self->history);

    if (self->back) {
        g_list_model_items_changed(G_LIST_MODEL(self), old_position, added, 0);
    } else {
        if (removed > 0)
            g_list_model_items_changed(G_LIST_MODEL(self), 0, removed, 0);
    }
}

static void current_position_changed(LadybirdNavigationHistorySlice* self)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY_SLICE(self));

    size_t old_position = self->position;
    size_t new_position = ladybird_navigation_history_get_current_position(self->history);
    if (old_position == new_position)
        return;
    self->position = new_position;

    if (old_position < new_position) {
        size_t diff = new_position - old_position;
        if (self->back) {
            g_list_model_items_changed(G_LIST_MODEL(self), old_position, 0, diff);
        } else {
            g_list_model_items_changed(G_LIST_MODEL(self), 0, diff, 0);
        }
    } else {
        size_t diff = old_position - new_position;
        if (self->back) {
            g_list_model_items_changed(G_LIST_MODEL(self), new_position, diff, 0);
        } else {
            g_list_model_items_changed(G_LIST_MODEL(self), 0, 0, diff);
        }
    }
}

static void ladybird_navigation_history_slice_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdNavigationHistorySlice* self = LADYBIRD_NAVIGATION_HISTORY_SLICE(object);

    switch (prop_id) {
    case PROP_ITEM_TYPE:
        g_value_set_gtype(value, LADYBIRD_TYPE_HISTORY_ENTRY);
        break;

    case PROP_N_ITEMS:
        g_value_set_uint(value, g_list_model_get_n_items(G_LIST_MODEL(self)));
        break;

    case PROP_BACK:
        g_value_set_boolean(value, self->back);
        break;

    case PROP_HISTORY:
        g_value_set_object(value, self->history);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_navigation_history_slice_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdNavigationHistorySlice* self = LADYBIRD_NAVIGATION_HISTORY_SLICE(object);

    switch (prop_id) {
    case PROP_BACK:
        self->back = g_value_get_boolean(value);
        break;

    case PROP_HISTORY:
        self->history = LADYBIRD_NAVIGATION_HISTORY(g_value_dup_object(value));
        self->position = ladybird_navigation_history_get_current_position(self->history);
        g_signal_connect_object(self->history, "notify::current-position", G_CALLBACK(current_position_changed), self, G_CONNECT_SWAPPED);
        g_signal_connect_object(self->history, "items-changed", G_CALLBACK(items_changed), self, G_CONNECT_SWAPPED);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_navigation_history_slice_dispose(GObject* object)
{
    LadybirdNavigationHistorySlice* self = LADYBIRD_NAVIGATION_HISTORY_SLICE(object);

    if (self->history) {
        g_signal_handlers_disconnect_by_data(self->history, self);
        g_clear_object(&self->history);
    }

    G_OBJECT_CLASS(ladybird_navigation_history_slice_parent_class)->dispose(object);
}

static void ladybird_navigation_history_slice_init([[maybe_unused]] LadybirdNavigationHistorySlice* self)
{
}

static GType ladybird_navigation_history_slice_get_item_type(GListModel* model)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY_SLICE(model), G_TYPE_NONE);

    return LADYBIRD_TYPE_HISTORY_ENTRY;
}

static guint ladybird_navigation_history_slice_get_n_items(GListModel* model)
{
    LadybirdNavigationHistorySlice* self = LADYBIRD_NAVIGATION_HISTORY_SLICE(model);

    if (self->back)
        return self->position;
    size_t n_items = g_list_model_get_n_items(G_LIST_MODEL(self->history));
    if (n_items == 0)
        return 0;
    return n_items - self->position - 1;
}

guint ladybird_navigation_history_slice_map_position(LadybirdNavigationHistorySlice* self, guint position)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY_SLICE(self), 0);

    if (!self->back)
        position += 1 + self->position;

    return position;
}

static void* ladybird_navigation_history_slice_get_item(GListModel* model, guint position)
{
    LadybirdNavigationHistorySlice* self = LADYBIRD_NAVIGATION_HISTORY_SLICE(model);

    position = ladybird_navigation_history_slice_map_position(self, position);
    return g_list_model_get_item(G_LIST_MODEL(self->history), position);
}

static void ladybird_navigation_history_slice_init_list_model(GListModelInterface* iface)
{
    iface->get_item_type = ladybird_navigation_history_slice_get_item_type;
    iface->get_n_items = ladybird_navigation_history_slice_get_n_items;
    iface->get_item = ladybird_navigation_history_slice_get_item;
}

static void ladybird_navigation_history_slice_class_init(LadybirdNavigationHistorySliceClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_navigation_history_slice_get_property;
    object_class->set_property = ladybird_navigation_history_slice_set_property;
    object_class->dispose = ladybird_navigation_history_slice_dispose;

    constexpr GParamFlags ro_param_flags = GParamFlags(G_PARAM_READABLE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);
    constexpr GParamFlags co_param_flags = GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY | G_PARAM_CONSTRUCT_ONLY);

    props[PROP_ITEM_TYPE] = g_param_spec_gtype("item-type", nullptr, nullptr, G_TYPE_OBJECT, ro_param_flags);
    props[PROP_N_ITEMS] = g_param_spec_uint("n-items", nullptr, nullptr, 0, G_MAXUINT, 0, ro_param_flags);
    props[PROP_HISTORY] = g_param_spec_object("history", nullptr, nullptr, LADYBIRD_TYPE_NAVIGATION_HISTORY, co_param_flags);
    props[PROP_BACK] = g_param_spec_boolean("back", nullptr, nullptr, false, co_param_flags);

    g_object_class_install_properties(object_class, NUM_PROPS, props);
}

GListModel* ladybird_navigation_history_slice_new_back(LadybirdNavigationHistory* history)
{
    return G_LIST_MODEL(g_object_new(LADYBIRD_TYPE_NAVIGATION_HISTORY_SLICE,
        "history", history,
        "back", (gboolean) true,
        nullptr));
}

GListModel* ladybird_navigation_history_slice_new_forward(LadybirdNavigationHistory* history)
{
    return G_LIST_MODEL(g_object_new(LADYBIRD_TYPE_NAVIGATION_HISTORY_SLICE,
        "history", history,
        "back", (gboolean) false,
        nullptr));
}

G_END_DECLS
