#include "HistoryEntry.h"

struct _LadybirdHistoryEntry {
    GInitiallyUnowned parent_instance;

    char* url;
    char* title;
};

enum {
    PROP_0,
    PROP_URL,
    PROP_TITLE,
    NUM_PROPS,
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdHistoryEntry, ladybird_history_entry, G_TYPE_INITIALLY_UNOWNED)

char const* ladybird_history_entry_get_url(LadybirdHistoryEntry* self)
{
    g_return_val_if_fail(LADYBIRD_IS_HISTORY_ENTRY(self), nullptr);

    return self->url;
}

void ladybird_history_entry_set_url(LadybirdHistoryEntry* self, char const* url)
{
    g_return_if_fail(LADYBIRD_IS_HISTORY_ENTRY(self));
    if (g_set_str(&self->url, url))
        g_object_notify_by_pspec(G_OBJECT(self), props[PROP_URL]);
}

char const* ladybird_history_entry_get_title(LadybirdHistoryEntry* self)
{
    g_return_val_if_fail(LADYBIRD_IS_HISTORY_ENTRY(self), nullptr);

    return self->title;
}

void ladybird_history_entry_set_title(LadybirdHistoryEntry* self, char const* title)
{
    g_return_if_fail(LADYBIRD_IS_HISTORY_ENTRY(self));
    if (g_set_str(&self->title, title))
        g_object_notify_by_pspec(G_OBJECT(self), props[PROP_TITLE]);
}

static void ladybird_history_entry_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdHistoryEntry* self = LADYBIRD_HISTORY_ENTRY(object);

    switch (prop_id) {
    case PROP_URL:
        g_value_set_string(value, self->url);
        break;

    case PROP_TITLE:
        g_value_set_string(value, self->title);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_history_entry_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdHistoryEntry* self = LADYBIRD_HISTORY_ENTRY(object);

    switch (prop_id) {
    case PROP_URL:
        ladybird_history_entry_set_url(self, g_value_get_string(value));
        break;

    case PROP_TITLE:
        ladybird_history_entry_set_title(self, g_value_get_string(value));
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_history_entry_finalize(GObject* object)
{
    LadybirdHistoryEntry* self = LADYBIRD_HISTORY_ENTRY(object);

    g_clear_pointer(&self->url, g_free);
    g_clear_pointer(&self->title, g_free);

    printf("!!! Finalizing history entry at %p\n", object);

    G_OBJECT_CLASS(ladybird_history_entry_parent_class)->finalize(object);
}

static void ladybird_history_entry_init([[maybe_unused]] LadybirdHistoryEntry* self)
{
}

static void ladybird_history_entry_class_init(LadybirdHistoryEntryClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_history_entry_get_property;
    object_class->set_property = ladybird_history_entry_set_property;
    object_class->finalize = ladybird_history_entry_finalize;

    constexpr GParamFlags param_flags = GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);

    props[PROP_URL] = g_param_spec_string("url", nullptr, nullptr, nullptr, param_flags);
    props[PROP_TITLE] = g_param_spec_string("title", nullptr, nullptr, nullptr, param_flags);
    g_object_class_install_properties(object_class, NUM_PROPS, props);
}

LadybirdHistoryEntry* ladybird_history_entry_new(char const* url)
{
    return LADYBIRD_HISTORY_ENTRY(g_object_new(LADYBIRD_TYPE_HISTORY_ENTRY,
        "url", url,
        nullptr));
}

G_END_DECLS
