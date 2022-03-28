/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/*
 * You may want to invoke the checker like this:
 * $ cd Build/lagom
 * $ ninja
 * $ find ../../AK ../../Base ../../Documentation/ ../../Kernel/ ../../Meta/ ../../Ports/ ../../Tests/ ../../Userland/ -type f -name '*.md' | xargs ./markdown-check ../../README.md
 */

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/RecursionDecision.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Visitor.h>
#include <stdlib.h>

static bool is_missing_file_acceptable(String const& filename)
{
    const StringView acceptable_missing_files[] = {
        // FIXME: Please write these manpages!
        "/usr/share/man/man2/accept.md",
        "/usr/share/man/man2/exec.md",
        "/usr/share/man/man2/fcntl.md",
        "/usr/share/man/man2/fork.md",
        "/usr/share/man/man2/ioctl.md",
        "/usr/share/man/man2/listen.md",
        "/usr/share/man/man2/mmap.md",
        "/usr/share/man/man2/mprotect.md",
        "/usr/share/man/man2/open.md",
        "/usr/share/man/man2/ptrace.md",
        "/usr/share/man/man5/perfcore.md",
        // These ones are okay:
        "/home/anon/Tests/js-tests/test-common.js",
        "/man1/index.html",
        "/man2/index.html",
        "/man3/index.html",
        "/man4/index.html",
        "/man5/index.html",
        "/man6/index.html",
        "/man7/index.html",
        "/man8/index.html",
        "index.html",
    };
    for (auto const& suffix : acceptable_missing_files) {
        if (filename.ends_with(suffix))
            return true;
    }
    return false;
}

struct FileLink {
    String file_path; // May be empty, but not null
    String anchor;    // May be null ("foo.md", "bar.png"), may be empty ("baz.md#")
    String label;     // May be empty, but not null
};

class MarkdownLinkage final : Markdown::Visitor {
public:
    ~MarkdownLinkage() = default;

    static MarkdownLinkage analyze(Markdown::Document const&);

    bool has_anchor(String const& anchor) const { return m_anchors.contains(anchor); }
    HashTable<String> const& anchors() const { return m_anchors; }
    bool has_invalid_link() const { return m_has_invalid_link; }
    Vector<FileLink> const& file_links() const { return m_file_links; }

private:
    MarkdownLinkage()
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

    HashTable<String> m_anchors;
    Vector<FileLink> m_file_links;
    bool m_has_invalid_link { false };

    String m_serenity_source_directory;
};

MarkdownLinkage MarkdownLinkage::analyze(Markdown::Document const& document)
{
    MarkdownLinkage linkage;

    document.walk(linkage);

    return linkage;
}

class StringCollector final : Markdown::Visitor {
public:
    StringCollector() = default;
    virtual ~StringCollector() = default;

    String build() { return m_builder.build(); }

    static String from(Markdown::Heading const& heading)
    {
        StringCollector collector;
        heading.walk(collector);
        return collector.build();
    }

    static String from(Markdown::Text::Node const& node)
    {
        StringCollector collector;
        node.walk(collector);
        return collector.build();
    }

private:
    virtual RecursionDecision visit(String const& text) override
    {
        m_builder.append(text);
        return RecursionDecision::Recurse;
    }

    StringBuilder m_builder;
};

static String slugify(String const& text)
{
    // TODO: This feels like it belongs into LibWeb.
    String slug = text.to_lowercase();
    // Reverse-engineered through github, using:
    // find AK/ Base/ Documentation/ Kernel/ Meta/ Ports/ Tests/ Userland/ -name '*.md' | xargs grep --color=always -Pin '^##+ .*[^a-z0-9 ?()`_:/!&|.$'"'"',<>"+-]' README.md
    slug = slug.replace(" ", "-", true)
               .replace("!", "", true)
               .replace("?", "", true)
               .replace("(", "", true)
               .replace(")", "", true)
               .replace(":", "", true)
               .replace("/", "-", true)
               .replace("&", "", true)
               .replace("|", "", true)
               .replace(".", "", true)
               .replace("$", "", true)
               .replace("'", "", true)
               .replace(",", "", true)
               .replace("\"", "", true)
               .replace("+", "", true)
               .replace("\\", "", true)
               .replace("<", "", true)
               .replace(">", "", true);
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
    String const& href = link_node.href;
    if (href.is_null()) {
        // Nothing to do here.
        return RecursionDecision::Recurse;
    }
    auto url = URL::create_with_url_or_path(href);
    if (url.is_valid()) {
        if (url.scheme() == "https" || url.scheme() == "http") {
            outln("Not checking external link {}", href);
            return RecursionDecision::Recurse;
        }
        if (url.scheme() == "help") {
            if (url.host() != "man") {
                warnln("help:// URL without 'man': {}", href);
                m_has_invalid_link = true;
                return RecursionDecision::Recurse;
            }
            if (url.paths().size() < 2) {
                warnln("help://man URL is missing section or page: {}", href);
                m_has_invalid_link = true;
                return RecursionDecision::Recurse;
            }
            auto file = String::formatted("../man{}/{}.md", url.paths()[0], url.paths()[1]);

            m_file_links.append({ file, String(), StringCollector::from(*link_node.text) });
            return RecursionDecision::Recurse;
        }
        if (url.scheme() == "file") {
            // TODO: Check more possible links other than icons.
            if (url.path().starts_with("/res/icons/")) {
                auto file = String::formatted("{}/Base{}", m_serenity_source_directory, url.path());
                m_file_links.append({ file, String(), StringCollector::from(*link_node.text) });
                return RecursionDecision::Recurse;
            }
            outln("Not checking local link {}", href);
            return RecursionDecision::Recurse;
        }
    }

    String label = StringCollector::from(*link_node.text);
    Optional<size_t> last_hash = href.find_last('#');
    if (last_hash.has_value()) {
        m_file_links.append({ href.substring(0, last_hash.value()), href.substring(last_hash.value() + 1), label });
    } else {
        m_file_links.append({ href, String(), label });
    }

    return RecursionDecision::Recurse;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    Vector<StringView> file_paths;
    args_parser.add_positional_argument(file_paths, "Path to markdown files to read and parse", "paths", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    outln("Reading and parsing Markdown files ...");
    HashMap<String, MarkdownLinkage> files;
    for (auto path : file_paths) {
        auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to read {}: {}", path, file_or_error.error());
            // Since this should never happen anyway, fail early.
            return file_or_error.release_error();
        }
        auto file = file_or_error.release_value();
        auto content_buffer = file->read_all();
        auto content = StringView(content_buffer);
        auto document = Markdown::Document::parse(content);
        if (!document) {
            warnln("Failed to parse {} due to an unspecified error.", path);
            // Since this should never happen anyway, fail early.
            return 1;
        }
        files.set(Core::File::real_path_for(path), MarkdownLinkage::analyze(*document));
    }

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
            String pointee_file;
            if (file_link.file_path.is_empty()) {
                pointee_file = file_item.key;
            } else {
                pointee_file = LexicalPath::absolute_path(file_dir, file_link.file_path);
            }
            if (!Core::File::exists(pointee_file) && !is_missing_file_acceptable(pointee_file)) {
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

    if (any_problems) {
        outln("Done. Some errors were encountered, please check above log.");
        return 1;
    } else {
        outln("Done. No problems detected.");
        return 0;
    }
}
