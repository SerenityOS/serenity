#include "LocationEntry.h"

struct _LadybirdLocationEntry {
    GtkEntry parent_instance;
};

enum {
    PROP_0,
    NUM_PROPS,
};

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdLocationEntry, ladybird_location_entry, GTK_TYPE_ENTRY)

static void ladybird_location_entry_init(LadybirdLocationEntry* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    gtk_widget_init_template(widget);
}

static void ladybird_location_entry_dispose(GObject* object)
{
    gtk_widget_dispose_template(GTK_WIDGET(object), LADYBIRD_TYPE_LOCATION_ENTRY);

    G_OBJECT_CLASS(ladybird_location_entry_parent_class)->dispose(object);
}

static void ladybird_location_entry_class_init(LadybirdLocationEntryClass* klass)
{
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = ladybird_location_entry_dispose;

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/Ladybird-gtk4/location-entry.ui");
}

G_END_DECLS
