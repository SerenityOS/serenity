/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LocationEntry.h"
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringView.h>

#ifdef HAVE_LIBSOUP
#    include <libsoup/soup.h>
#endif

#ifdef ENABLE_PUBLIC_SUFFIX_DOWNLOAD
#    include <LibPublicSuffix/PublicSuffixData.h>
#endif

struct _LadybirdLocationEntry {
    GtkEntry parent_instance;
};

enum {
    PROP_0,
    NUM_PROPS,
};

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdLocationEntry, ladybird_location_entry, GTK_TYPE_ENTRY)

static Optional<StringView> find_base_domain(char const* text)
{
    GUri* uri = g_uri_parse(text, G_URI_FLAGS_PARSE_RELAXED, nullptr);
    if (!uri)
        return {};
    AK::ScopeGuard unref_uri = [uri]() {
        g_uri_unref(uri);
    };

    char const* host = g_uri_get_host(uri);
    if (!host)
        return {};

#ifdef HAVE_LIBSOUP

    char const* base_domain = soup_tld_get_base_domain(host, nullptr);
    if (!base_domain)
        return {};

    // This is quite horrible. Look for the base domain in the text.
    char const* base_domain_ptr = strstr(text, base_domain);
    if (!base_domain_ptr)
        return {};
    return StringView { base_domain_ptr, strlen(base_domain) };

#elif defined(ENABLE_PUBLIC_SUFFIX_DOWNLOAD)

    StringView host_sv { host, strlen(host) };
    if (auto r = PublicSuffix::PublicSuffixData::the()->get_public_suffix(host_sv); !r.is_error()) {
        if (auto v = r.release_value(); v.has_value()) {
            auto public_suffix = v.release_value();
            g_return_val_if_fail(host_sv.ends_with(public_suffix), Optional<StringView> {});
            size_t public_suffix_len = public_suffix.bytes().size();
            size_t public_suffix_start = host_sv.length() - public_suffix_len;
            // We know the public suffix, now find the complete base domain.
            auto trimmed_host = host_sv.substring_view(0, public_suffix_start).trim("."sv, TrimMode::Right);
            g_return_val_if_fail(host_sv.starts_with(trimmed_host), Optional<StringView> {});
            auto last_dot = trimmed_host.find_last('.');
            if (!last_dot.has_value()) {
                // Guess it's just the public suffix then.
                return host_sv.substring_view(public_suffix_start);
            } else {
                return host_sv.substring_view(last_dot.value() + 1);
            }
        }
    }
    return {};

#endif
}

static void update_text_attrs(LadybirdLocationEntry* self)
{
    PangoAttrList* attrs = pango_attr_list_new();
    char const* text = gtk_editable_get_text(GTK_EDITABLE(self));
    auto optional_base_domain = find_base_domain(text);
    if (optional_base_domain.has_value()) {
        StringView base_domain = optional_base_domain.release_value();

        constexpr guint16 max_alpha = 65535;

        // Make most of the text semi-transparent.
        pango_attr_list_insert(attrs, pango_attr_foreground_alpha_new(max_alpha / 2));
        // Make the base domain normal (opaque).
        PangoAttribute* normal = pango_attr_foreground_alpha_new(max_alpha);
        normal->start_index = base_domain.characters_without_null_termination() - text;
        normal->end_index = normal->start_index + base_domain.length();
        pango_attr_list_insert(attrs, normal);
    }
    gtk_entry_set_attributes(GTK_ENTRY(self), attrs);
    pango_attr_list_unref(attrs);
}

static void update_primary_icon(LadybirdLocationEntry* self)
{
    char const* text = gtk_editable_get_text(GTK_EDITABLE(self));
    char const* icon_name;
    bool sensitive = true;

    if (g_str_has_prefix(text, "https:") || g_str_has_prefix(text, "gemini:")) {
        icon_name = "channel-secure-symbolic";
    } else if (g_str_has_prefix(text, "http:")) {
        icon_name = "channel-insecure-symbolic";
    } else if (g_str_has_prefix(text, "file:")) {
        icon_name = "folder-symbolic";
    } else if (g_str_has_prefix(text, "data:")) {
        icon_name = "mail-attachment-symbolic";
    } else {
        icon_name = "system-search-symbolic";
        sensitive = text && *text;
    }
    gtk_entry_set_icon_from_icon_name(GTK_ENTRY(self), GTK_ENTRY_ICON_PRIMARY, icon_name);
    gtk_entry_set_icon_sensitive(GTK_ENTRY(self), GTK_ENTRY_ICON_PRIMARY, sensitive);
}

static void on_notify_text(LadybirdLocationEntry* self)
{
    update_text_attrs(self);
    update_primary_icon(self);
}

static void ladybird_location_entry_measure(GtkWidget* widget, GtkOrientation orientation, int for_size, int* minimum, int* natural, int* minimum_baseline, int* natural_baseline)
{
    // Workaround a GTK bug, which your version of GTK may or may not have.
    // GtkEntry, which we inherit from, can report its baseline wrongly in
    // presence of icons, which causes it to be taller than it should be. We
    // just unset the baseline to work around that.
    GTK_WIDGET_CLASS(ladybird_location_entry_parent_class)->measure(widget, orientation, for_size, minimum, natural, minimum_baseline, natural_baseline);
    *minimum_baseline = *natural_baseline = -1;
}

static void ladybird_location_entry_init(LadybirdLocationEntry* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    gtk_widget_init_template(widget);
    update_primary_icon(self);
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

    widget_class->measure = ladybird_location_entry_measure;

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/Ladybird-gtk4/location-entry.ui");
    gtk_widget_class_bind_template_callback(widget_class, on_notify_text);
}

G_END_DECLS
