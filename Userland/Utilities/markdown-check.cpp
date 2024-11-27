/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/*
 * You may want to invoke the checker like this:
 * $ ninja -C Build/lagom
 * $ export SERENITY_SOURCE_DIR=/path/to/serenity
 * $ find AK Base Documentation Kernel Meta Ports Tests Userland -type f -name '*.md' -print0 | xargs -0 Build/lagom/bin/markdown-check README.md CONTRIBUTING.md
 */

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/RecursionDecision.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibManual/PageNode.h>
#include <LibManual/Path.h>
#include <LibManual/SectionNode.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Visitor.h>
#include <LibURL/URL.h>
#include <stdlib.h>

static bool is_missing_file_acceptable(ByteString const& filename)
{
    StringView const acceptable_missing_files[] = {
        // FIXME: Please write these manpages!
        "/usr/share/man/man2/exec.md"sv,
        "/usr/share/man/man2/fcntl.md"sv,
        "/usr/share/man/man2/fork.md"sv,
        "/usr/share/man/man2/ioctl.md"sv,
        "/usr/share/man/man2/listen.md"sv,
        "/usr/share/man/man2/mmap.md"sv,
        "/usr/share/man/man2/mprotect.md"sv,
        "/usr/share/man/man2/open.md"sv,
        "/usr/share/man/man2/ptrace.md"sv,
        "/usr/share/man/man5/perfcore.md"sv,
        // These ones are okay:
        "/home/anon/Tests/js-tests/test-common.js"sv,
        "/man1/index.html"sv,
        "/man2/index.html"sv,
        "/man3/index.html"sv,
        "/man4/index.html"sv,
        "/man5/index.html"sv,
        "/man6/index.html"sv,
        "/man7/index.html"sv,
        "/man8/index.html"sv,
        "index.html"sv,
    };
    for (auto const& suffix : acceptable_missing_files) {
        if (filename.ends_with(suffix))
            return true;
    }
    return false;
}

struct FileLink {
    ByteString file_path; // May be empty, but not null
    ByteString anchor;    // May be null ("foo.md", "bar.png"), may be empty ("baz.md#")
    ByteString label;     // May be empty, but not null
};

class MarkdownLinkage final : Markdown::Visitor {
public:
    ~MarkdownLinkage() = default;

    static MarkdownLinkage analyze(Markdown::Document const&, bool verbose);

    bool has_anchor(ByteString const& anchor) const { return m_anchors.contains(anchor); }
    HashTable<ByteString> const& anchors() const { return m_anchors; }
    bool has_invalid_link() const { return m_has_invalid_link; }
    Vector<FileLink> const& file_links() const { return m_file_links; }

private:
    MarkdownLinkage(bool verbose)
        : m_verbose(verbose)
    {
        auto const* source_directory = getenv("SERENITY_SOURCE_DIR");
        if (source_directory != nullptr) {
            m_serenity_source_directory = source_directory;
        } else {
            warnln("The environment variable SERENITY_SOURCE_DIR was not found. Link checking inside Serenity's filesystem will fail.");
        }
    }

    virtual RecursionDecision visit(Markdown::Heading const&) override;
    virtual RecursionDecision visit(Markdown::Text::LinkNode const&) override;

    HashTable<ByteString> m_anchors;
    Vector<FileLink> m_file_links;
    bool m_has_invalid_link { false };
    bool m_verbose { false };

    ByteString m_serenity_source_directory;
};

MarkdownLinkage MarkdownLinkage::analyze(Markdown::Document const& document, bool verbose)
{
    MarkdownLinkage linkage(verbose);

    document.walk(linkage);

    return linkage;
}

class StringCollector final : Markdown::Visitor {
public:
    StringCollector() = default;
    virtual ~StringCollector() = default;

    ByteString build() { return m_builder.to_byte_string(); }

    static ByteString from(Markdown::Heading const& heading)
    {
        StringCollector collector;
        heading.walk(collector);
        return collector.build();
    }

    static ByteString from(Markdown::Text::Node const& node)
    {
        StringCollector collector;
        node.walk(collector);
        return collector.build();
    }

private:
    virtual RecursionDecision visit(ByteString const& text) override
    {
        m_builder.append(text);
        return RecursionDecision::Recurse;
    }

    StringBuilder m_builder;
};

static ByteString slugify(ByteString const& text)
{
    // TODO: This feels like it belongs into LibWeb.
    ByteString slug = text.to_lowercase();
    // Reverse-engineered through github, using:
    // find AK/ Base/ Documentation/ Kernel/ Meta/ Ports/ Tests/ Userland/ -name '*.md' | xargs grep --color=always -Pin '^##+ .*[^a-z0-9 ?()`_:/!&|.$'"'"',<>"+-]' README.md
    slug = slug.replace(" "sv, "-"sv, ReplaceMode::All)
               .replace("!"sv, ""sv, ReplaceMode::All)
               .replace("?"sv, ""sv, ReplaceMode::All)
               .replace("("sv, ""sv, ReplaceMode::All)
               .replace(")"sv, ""sv, ReplaceMode::All)
               .replace(":"sv, ""sv, ReplaceMode::All)
               .replace("/"sv, "-"sv, ReplaceMode::All)
               .replace("&"sv, ""sv, ReplaceMode::All)
               .replace("|"sv, ""sv, ReplaceMode::All)
               .replace("."sv, ""sv, ReplaceMode::All)
               .replace("$"sv, ""sv, ReplaceMode::All)
               .replace("'"sv, ""sv, ReplaceMode::All)
               .replace(","sv, ""sv, ReplaceMode::All)
               .replace("\""sv, ""sv, ReplaceMode::All)
               .replace("+"sv, ""sv, ReplaceMode::All)
               .replace("\\"sv, ""sv, ReplaceMode::All)
               .replace("<"sv, ""sv, ReplaceMode::All)
               .replace(">"sv, ""sv, ReplaceMode::All);
    // What about "="?
    return slug;
}

RecursionDecision MarkdownLinkage::visit(Markdown::Heading const& heading)
{
    m_anchors.set(slugify(StringCollector::from(heading)));
    return RecursionDecision::Recurse;
}

RecursionDecision MarkdownLinkage::visit(Markdown::Text::LinkNode const& link_node)
{
    ByteString const& href = link_node.href;
    if (href.is_empty()) {
        // Nothing to do here.
        return RecursionDecision::Recurse;
    }

    auto url = URL::create_with_url_or_path(href);
    if (url.is_valid()) {
        if (url.scheme() == "https" || url.scheme() == "http") {
            if (m_verbose)
                outln("Not checking external link {}", href);
            return RecursionDecision::Recurse;
        }
        if (url.scheme() == "help") {
            if (url.host() != "man"_string) {
                warnln("help:// URL without 'man': {}", href);
                m_has_invalid_link = true;
                return RecursionDecision::Recurse;
            }
            if (url.path_segment_count() < 2) {
                warnln("help://man URL is missing section or page: {}", href);
                m_has_invalid_link = true;
                return RecursionDecision::Recurse;
            }

            // Remove leading '/' from the path.
            auto file = ByteString::formatted("{}/Base/usr/share/man/man{}.md", m_serenity_source_directory, URL::percent_decode(url.serialize_path()).substring(1));

            m_file_links.append({ file, ByteString(), StringCollector::from(*link_node.text) });
            return RecursionDecision::Recurse;
        }
        if (url.scheme() == "file") {
            auto file_path = URL::percent_decode(url.serialize_path());
            if (file_path.contains("man"sv) && file_path.ends_with(".md"sv)) {
                warnln("Inter-manpage link without the help:// scheme: {}\nPlease use help URLs of the form 'help://man/<section>/<subsection...>/<page>'", href);
                m_has_invalid_link = true;
                return RecursionDecision::Recurse;
            }
            // TODO: Check more possible links other than icons.
            if (file_path.starts_with("/res/icons/"sv)) {
                auto file = ByteString::formatted("{}/Base{}", m_serenity_source_directory, file_path);
                m_file_links.append({ file, ByteString(), StringCollector::from(*link_node.text) });
            } else if (file_path.starts_with("/bin"sv)) {
                StringBuilder builder;
                link_node.text->render_to_html(builder);
                auto link_text = builder.string_view();
                if (link_text != "Open"sv) {
                    warnln("Binary link named '{}' is not allowed, binary links must be called 'Open'. Linked binary: {}", link_text, href);
                    m_has_invalid_link = true;
                }
            } else if (m_verbose) {
                outln("Not checking local link {}", href);
            }
            return RecursionDecision::Recurse;
        }
    }

    ByteString label = StringCollector::from(*link_node.text);
    Optional<size_t> last_hash = href.find_last('#');
    if (last_hash.has_value()) {
        m_file_links.append({ href.substring(0, last_hash.value()), href.substring(last_hash.value() + 1), label });
    } else {
        m_file_links.append({ href, ByteString(), label });
    }

    return RecursionDecision::Recurse;
}

static ErrorOr<String> generate_link_graph(HashMap<NonnullRefPtr<Manual::PageNode const>, Vector<NonnullRefPtr<Manual::PageNode const>>> const& page_links)
{
    auto const header = "digraph manpage_links {\n"sv;
    StringBuilder builder;
    TRY(builder.try_append(header));

    // Not displayed to the user.
    HashMap<NonnullRefPtr<Manual::PageNode const>, String> page_identifiers;

    for (auto const& page : page_links.keys()) {
        auto path = TRY(page->path());
        StringBuilder identifier_builder;
        // Only allow alphanumerics, replace everything else with underscores.
        for (auto const& character : path.code_points()) {
            if (AK::is_ascii_alphanumeric(character))
                TRY(identifier_builder.try_append_code_point(character));
            else
                TRY(identifier_builder.try_append('_'));
        }
        auto const identifier = TRY(identifier_builder.to_string());
        TRY(builder.try_appendff("{} [label=\"{}({})\"];\n", identifier, TRY(page->name()), page->section_number()));
        TRY(page_identifiers.try_set(page, identifier));
    }

    for (auto const& from_page_list : page_links) {
        auto const& from_page = from_page_list.key;
        for (auto const& to_page : from_page_list.value) {
            auto const to_page_identifier = page_identifiers.get(to_page);
            // Target page doesn't actually exist; it's probably an ignored page.
            if (!to_page_identifier.has_value())
                continue;
            TRY(builder.try_appendff("{} -> {};\n", page_identifiers.get(from_page).value(), page_identifiers.get(to_page).value()));
        }
    }

    TRY(builder.try_append("}\n"sv));

    return builder.to_string();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    Vector<StringView> file_paths;
    bool output_link_graph { false };
    bool verbose_output { false };
    StringView base_path = "/"sv;
    args_parser.add_positional_argument(file_paths, "Path to markdown files to read and parse", "paths", Core::ArgsParser::Required::Yes);
    args_parser.add_option(base_path, "System base path (default: \"/\")", "base", 'b', "path");
    args_parser.add_option(output_link_graph, "Output a page link graph into \"manpage-links.gv\". The recommended tool to process this graph is `fdp`.", "link-graph", 'g');
    args_parser.add_option(verbose_output, "Print extra information about skipped links", "verbose", 'v');
    args_parser.parse(arguments);

    if (verbose_output)
        outln("Reading and parsing Markdown files ...");
    HashMap<ByteString, MarkdownLinkage> files;
    for (auto path : file_paths) {
        auto file_or_error = Core::File::open(path, Core::File::OpenMode::Read);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            // Since this should never happen anyway, fail early.
            return file_or_error.release_error();
        }
        auto file = file_or_error.release_value();

        auto content_buffer_or_error = file->read_until_eof();
        if (content_buffer_or_error.is_error()) {
            warnln("Failed to read {}: {}", path, file_or_error.error());
            // Since this should never happen anyway, fail early.
            return file_or_error.release_error();
        }
        auto content_buffer = content_buffer_or_error.release_value();

        auto content = StringView(content_buffer);
        auto document = Markdown::Document::parse(content);
        if (!document) {
            warnln("Failed to parse {} due to an unspecified error.", path);
            // Since this should never happen anyway, fail early.
            return 1;
        }
        files.set((TRY(FileSystem::real_path(path))), MarkdownLinkage::analyze(*document, verbose_output));
    }

    if (verbose_output)
        outln("Checking links ...");
    bool any_problems = false;
    for (auto const& file_item : files) {
        if (file_item.value.has_invalid_link()) {
            outln("File '{}' has invalid links.", file_item.key);
            any_problems = true;
            continue;
        }

        auto file_lexical_path = LexicalPath(file_item.key);
        auto file_dir = file_lexical_path.dirname();
        for (auto const& file_link : file_item.value.file_links()) {
            ByteString pointee_file;
            if (file_link.file_path.is_empty()) {
                pointee_file = file_item.key;
            } else {
                pointee_file = LexicalPath::absolute_path(file_dir, file_link.file_path);
            }
            if (!FileSystem::exists(pointee_file) && !is_missing_file_acceptable(pointee_file)) {
                outln("File '{}' points to '{}' (label '{}'), but '{}' does not exist!",
                    file_item.key, file_link.file_path, file_link.label, pointee_file);
                any_problems = true;
                continue;
            }
            if (file_link.anchor.is_empty()) {
                // No anchor to test for.
                continue;
            }

            auto pointee_linkage = files.find(pointee_file);
            if (pointee_linkage == files.end()) {
                outln("File '{}' points to file '{}', which exists, but was not scanned. Add it to the command-line arguments and re-run.",
                    file_item.key, pointee_file);
                any_problems = true;
                continue;
            }

            if (!pointee_linkage->value.has_anchor(file_link.anchor)) {
                outln("File '{}' points to '{}#{}' (label '{}'), but file '{}' does not have any heading that results in the anchor '{}'.",
                    file_item.key, file_link.file_path, file_link.anchor, file_link.label, pointee_file, file_link.anchor);
                out("    The following anchors seem to be available:\n    ");
                bool any_anchors = false;
                for (auto const& anchor : pointee_linkage->value.anchors()) {
                    if (any_anchors)
                        out(", ");
                    out("'{}'", anchor);
                    any_anchors = true;
                }
                if (!any_anchors)
                    out("(none)");
                outln();
                any_problems = true;
            }
        }
    }

    if (output_link_graph) {
        // First, collect all pages, and collect links between pages in a second step after all pages must have been collected.
        HashMap<ByteString, NonnullRefPtr<Manual::PageNode const>> pages;
        for (auto const& file : files) {
            auto base_relative_path = TRY(String::formatted("/{}", LexicalPath::relative_path(file.key, base_path)));
            auto page = Manual::Node::try_create_from_query({ base_relative_path });
            if (page.is_error()) {
                dbgln("Not including {} in the link graph since it's not a man page.", file.key);
                continue;
            }
            TRY(pages.try_set(file.key, page.value()));
            for (auto const& link : file.value.file_links()) {
                auto base_relative_path = TRY(String::formatted("/{}", LexicalPath::relative_path(link.file_path, base_path)));
                auto maybe_target_page = Manual::Node::try_create_from_query({ base_relative_path });
                if (maybe_target_page.is_error()) {
                    dbgln("Not including {} in the link graph since it's not a man page.", link.file_path);
                    continue;
                }
                TRY(pages.try_set(link.file_path, maybe_target_page.value()));
            }
        }

        HashMap<NonnullRefPtr<Manual::PageNode const>, Vector<NonnullRefPtr<Manual::PageNode const>>> page_links;
        for (auto const& file : files) {
            auto page = pages.get(file.key);
            if (!page.has_value())
                continue;

            Vector<NonnullRefPtr<Manual::PageNode const>> linked_pages;
            for (auto const& link : file.value.file_links()) {
                auto linked_page = pages.get(link.file_path);
                if (!linked_page.has_value())
                    continue;

                TRY(linked_pages.try_append(*linked_page.value()));
            }
            TRY(page_links.try_set(*page.value(), move(linked_pages)));
        }

        auto const graph_text = TRY(generate_link_graph(page_links));
        auto const graph_file = TRY(Core::File::open("manpage-links.gv"sv, Core::File::OpenMode::Write | Core::File::OpenMode::Truncate));
        TRY(graph_file->write_until_depleted(graph_text.bytes()));
    }

    if (any_problems) {
        outln("Done. Some errors were encountered, please check above log.");
        return 1;
    } else if (verbose_output) {
        outln("Done. No problems detected.");
    }
    return 0;
}
