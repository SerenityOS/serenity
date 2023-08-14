#include "Application.h"
#include "Window.h"

struct _LadybirdApplication {
    AdwApplication parent_instance;
};

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdApplication, ladybird_application, ADW_TYPE_APPLICATION)

static void open_new_window([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    LadybirdApplication* app = LADYBIRD_APPLICATION(user_data);
    LadybirdWindow* window = ladybird_window_new(app, true);
    gtk_window_present(GTK_WINDOW(window));
}

static void show_shortcuts([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);
    GtkBuilder* builder = gtk_builder_new_from_resource("/org/serenityos/ladybird-gtk4/shortcuts-dialog.ui");
    GtkWindow* dialog = GTK_WINDOW(gtk_builder_get_object(builder, "shortcuts_dialog"));
    gtk_window_set_transient_for(dialog, gtk_application_get_active_window(app));
    gtk_window_present(GTK_WINDOW(dialog));
    g_object_unref(builder);
}

static void show_about([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);

    char const* developers[] = {
        "Sergey Bugaev",
        nullptr
    };

    AdwAboutWindow* about_window = ADW_ABOUT_WINDOW(g_object_new(ADW_TYPE_ABOUT_WINDOW,
        "application-name", "Ladybird",
        "version", "WIP",
        "application-icon", "application-x-executable", // TODO: we need an icon!
        "developer-name", "SerenityOS developers",
        "website", "https://ladybird.dev",
        "issue-url", "https://github.com/SerenityOS/serenity/issues",
        "copyright", "© 2023 SerenityOS developers",
        "license-type", GTK_LICENSE_BSD,
        "developers", developers,
        "comments", "Ladybird is a browser based on LibWeb web engine and LibJS JavaScript engine,"
                    " developed by a large team of contributors as a part of the SerenityOS project.",
        nullptr));

    adw_about_window_add_link(about_window, "SerenityOS website", "https://serenityos.org");

    GtkWindow* active_window = gtk_application_get_active_window(app);
    if (active_window)
        gtk_window_set_transient_for(GTK_WINDOW(about_window), active_window);
    gtk_window_present(GTK_WINDOW(about_window));
}

static void do_quit([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GApplication* app = G_APPLICATION(user_data);
    g_application_quit(app);
}

static GActionEntry const app_entries[] = {
    { "new-window", open_new_window, nullptr, nullptr, nullptr, { 0 } },
    { "shortcuts", show_shortcuts, nullptr, nullptr, nullptr, { 0 } },
    { "about", show_about, nullptr, nullptr, nullptr, { 0 } },
    { "quit", do_quit, nullptr, nullptr, nullptr, { 0 } }
};

static char const* const new_window_accels[] = { "<Primary>n", nullptr };
static char const* const shortcuts_accels[] = { "<Primary>question", nullptr };
static char const* const quit_accels[] = { "<Primary>q", nullptr };

static void ladybird_application_activate(GApplication* app)
{
    // Chain up (for no good reason...)
    G_APPLICATION_CLASS(ladybird_application_parent_class)->activate(app);

    LadybirdWindow* window = ladybird_window_new(LADYBIRD_APPLICATION(app), true);
    gtk_window_present(GTK_WINDOW(window));
}

static void ladybird_application_open(GApplication* app, GFile** files, int num_files, char const* hint)
{
    // Chain up (for no good reason...)
    G_APPLICATION_CLASS(ladybird_application_parent_class)->open(app, files, num_files, hint);

    // Look for a window to add the tabs to.
    LadybirdWindow* window = LADYBIRD_WINDOW(gtk_application_get_active_window(GTK_APPLICATION(app)));
    if (!window)
        window = ladybird_window_new(LADYBIRD_APPLICATION(app), false);

    for (int i = 0; i < num_files; i++)
        ladybird_window_open_file(window, files[i]);

    gtk_window_present(GTK_WINDOW(window));
}

static void ladybird_application_init([[maybe_unused]] LadybirdApplication* self)
{
    GtkApplication* gtk_app = GTK_APPLICATION(self);

    g_action_map_add_action_entries(G_ACTION_MAP(self), app_entries, G_N_ELEMENTS(app_entries), self);
    gtk_application_set_accels_for_action(gtk_app, "app.new-window", new_window_accels);
    gtk_application_set_accels_for_action(gtk_app, "app.shortcuts", shortcuts_accels);
    gtk_application_set_accels_for_action(gtk_app, "app.quit", quit_accels);
}

static void ladybird_application_class_init(LadybirdApplicationClass* klass)
{
    GApplicationClass* g_application_class = G_APPLICATION_CLASS(klass);

    g_application_class->activate = ladybird_application_activate;
    g_application_class->open = ladybird_application_open;
}

LadybirdApplication* ladybird_application_new(void)
{
    return LADYBIRD_APPLICATION(g_object_new(LADYBIRD_TYPE_APPLICATION,
        "application-id", "org.serenityos.ladybird-gtk4",
        "flags", G_APPLICATION_HANDLES_OPEN,
        nullptr));
}

G_END_DECLS
