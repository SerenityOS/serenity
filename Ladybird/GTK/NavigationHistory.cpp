#include "NavigationHistory.h"
#include "HistoryEntry.h"
#include <AK/Vector.h>

struct _LadybirdNavigationHistory {
    GObject parent_instance;

    AK::Vector<LadybirdHistoryEntry*> entries;
    size_t current_position;
};

enum {
    PROP_0,
    PROP_ITEM_TYPE,
    PROP_N_ITEMS,
    PROP_CAN_NAVIGATE_BACK,
    PROP_CAN_NAVIGATE_FORWARD,
    PROP_CURRENT_POSITION,
    PROP_CURRENT_ENTRY,
    NUM_PROPS,
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

static void ladybird_navigation_history_init_list_model(GListModelInterface* iface);

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdNavigationHistory, ladybird_navigation_history, G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE(G_TYPE_LIST_MODEL, ladybird_navigation_history_init_list_model))

void ladybird_navigation_history_push(LadybirdNavigationHistory* self, LadybirdHistoryEntry* entry)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self));
    g_return_if_fail(LADYBIRD_IS_HISTORY_ENTRY(entry));

    size_t remove_count = 0;
    if (!self->entries.is_empty()) {
        for (size_t i = self->current_position + 1; i < self->entries.size(); i++)
            g_object_unref(self->entries[i]);
        remove_count = self->entries.size() - self->current_position - 1;
        self->entries.remove(self->current_position + 1, remove_count);
        VERIFY(self->current_position + 1 == self->entries.size());
    }

    self->entries.append(g_object_ref_sink(entry));
    self->current_position = self->entries.size() - 1;

    g_list_model_items_changed(G_LIST_MODEL(self), self->current_position, remove_count, 1);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_N_ITEMS]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_BACK]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_FORWARD]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_POSITION]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_ENTRY]);
}

size_t ladybird_navigation_history_get_current_position(LadybirdNavigationHistory* self)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self), 0);

    return self->current_position;
}

void ladybird_navigation_history_set_current_position(LadybirdNavigationHistory* self, size_t position)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self));
    g_return_if_fail(position < self->entries.size());

    if (self->current_position == position)
        return;
    self->current_position = position;

    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_BACK]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_FORWARD]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_POSITION]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_ENTRY]);
}

LadybirdHistoryEntry* ladybird_navigation_history_get_current_entry(LadybirdNavigationHistory* self)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self), nullptr);

    if (self->entries.is_empty())
        return nullptr;
    return self->entries[self->current_position];
}

bool ladybird_navigation_history_can_navigate_back(LadybirdNavigationHistory* self)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self), false);

    return self->current_position > 0;
}

bool ladybird_navigation_history_can_navigate_forward(LadybirdNavigationHistory* self)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self), false);

    return self->current_position + 1 < self->entries.size();
}

void ladybird_navigation_history_navigate(LadybirdNavigationHistory* self, int delta)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self));

    if (delta < 0 && size_t(-delta) > self->current_position) {
        self->current_position = 0;
    } else if (delta > 0 && self->current_position + delta >= self->entries.size()) {
        self->current_position = self->entries.size() - 1;
    } else {
        self->current_position += delta;
    }

    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_BACK]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_FORWARD]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_POSITION]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_ENTRY]);
}

void ladybird_navigation_history_navigate_to_entry(LadybirdNavigationHistory* self, LadybirdHistoryEntry* entry)
{
    g_return_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(self));
    g_return_if_fail(LADYBIRD_IS_HISTORY_ENTRY(entry));

    auto optional_index = self->entries.find_first_index(entry);
    g_return_if_fail(optional_index.has_value());

    self->current_position = optional_index.value();

    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_BACK]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CAN_NAVIGATE_FORWARD]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_POSITION]);
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_CURRENT_ENTRY]);
}

static void ladybird_navigation_history_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(object);

    switch (prop_id) {
    case PROP_ITEM_TYPE:
        g_value_set_gtype(value, LADYBIRD_TYPE_HISTORY_ENTRY);
        break;

    case PROP_N_ITEMS:
        g_value_set_uint(value, self->entries.size());
        break;

    case PROP_CAN_NAVIGATE_BACK:
        g_value_set_boolean(value, ladybird_navigation_history_can_navigate_back(self));
        break;

    case PROP_CAN_NAVIGATE_FORWARD:
        g_value_set_boolean(value, ladybird_navigation_history_can_navigate_forward(self));
        break;

    case PROP_CURRENT_POSITION:
        g_value_set_uint(value, self->current_position);
        break;

    case PROP_CURRENT_ENTRY:
        g_value_set_object(value, ladybird_navigation_history_get_current_entry(self));
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_navigation_history_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(object);

    switch (prop_id) {
    case PROP_CURRENT_POSITION:
        ladybird_navigation_history_set_current_position(self, g_value_get_uint(value));
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_navigation_history_dispose(GObject* object)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(object);

    for (LadybirdHistoryEntry* entry : self->entries)
        g_object_unref(entry);
    self->entries.clear();
    self->current_position = 0;
    // Should we emit the signals here?

    G_OBJECT_CLASS(ladybird_navigation_history_parent_class)->dispose(object);
}

static void ladybird_navigation_history_finalize(GObject* object)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(object);

    self->entries.~Vector();

    G_OBJECT_CLASS(ladybird_navigation_history_parent_class)->finalize(object);
}

static void ladybird_navigation_history_init(LadybirdNavigationHistory* self)
{
    new (&self->entries) Vector<LadybirdHistoryEntry*>;
}

static GType ladybird_navigation_history_get_item_type(GListModel* model)
{
    g_return_val_if_fail(LADYBIRD_IS_NAVIGATION_HISTORY(model), G_TYPE_NONE);

    return LADYBIRD_TYPE_HISTORY_ENTRY;
}

static guint ladybird_navigation_history_get_n_items(GListModel* model)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(model);

    return self->entries.size();
}

static void* ladybird_navigation_history_get_item(GListModel* model, guint position)
{
    LadybirdNavigationHistory* self = LADYBIRD_NAVIGATION_HISTORY(model);
    g_return_val_if_fail(position < self->entries.size(), nullptr);

    return g_object_ref(self->entries[position]);
}

static void ladybird_navigation_history_init_list_model(GListModelInterface* iface)
{
    iface->get_item_type = ladybird_navigation_history_get_item_type;
    iface->get_n_items = ladybird_navigation_history_get_n_items;
    iface->get_item = ladybird_navigation_history_get_item;
}

static void ladybird_navigation_history_class_init(LadybirdNavigationHistoryClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_navigation_history_get_property;
    object_class->set_property = ladybird_navigation_history_set_property;
    object_class->dispose = ladybird_navigation_history_dispose;
    object_class->finalize = ladybird_navigation_history_finalize;

    constexpr GParamFlags param_flags = GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);
    constexpr GParamFlags ro_param_flags = GParamFlags(G_PARAM_READABLE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);

    props[PROP_ITEM_TYPE] = g_param_spec_gtype("item-type", nullptr, nullptr, G_TYPE_OBJECT, ro_param_flags);
    props[PROP_N_ITEMS] = g_param_spec_uint("n-items", nullptr, nullptr, 0, G_MAXUINT, 0, ro_param_flags);
    props[PROP_CAN_NAVIGATE_BACK] = g_param_spec_boolean("can-navigate-back", nullptr, nullptr, false, ro_param_flags);
    props[PROP_CAN_NAVIGATE_FORWARD] = g_param_spec_boolean("can-navigate-forward", nullptr, nullptr, false, ro_param_flags);
    props[PROP_CURRENT_POSITION] = g_param_spec_uint("current-position", nullptr, nullptr, 0, G_MAXUINT, 0, param_flags);
    props[PROP_CURRENT_ENTRY] = g_param_spec_object("current-entry", nullptr, nullptr, LADYBIRD_TYPE_HISTORY_ENTRY, ro_param_flags);

    g_object_class_install_properties(object_class, NUM_PROPS, props);
}

LadybirdNavigationHistory* ladybird_navigation_history_new(void)
{
    return LADYBIRD_NAVIGATION_HISTORY(g_object_new(LADYBIRD_TYPE_NAVIGATION_HISTORY, nullptr));
}

G_END_DECLS
