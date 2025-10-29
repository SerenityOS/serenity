# Guidelines for writing SerenityOS manual pages (manpages)

Additional useful information on authoring manpages can be found with the [Linux man-pages project](https://www.man7.org/linux/man-pages/man7/man-pages.7.html) and the [FreeBSD Manual Pages documentation](https://docs.freebsd.org/en/books/fdp-primer/manual-pages/).

## What does and does not belong in manual pages

The SerenityOS manpages are the primary documentation for SerenityOS itself. Different from the documentation pages, it is not primarily intended for SerenityOS developers. If the question of "Would this information be useful to view within the OS?" can be answered with "Yes", that probably means the information belongs in a manual page.

## Format

Primary information on the manpage system and structure lives in [man(7)](../Base/usr/share/man/man7/man.md).

Manpages are written in CommonMark Markdown. It is allowed, but not ideal, to use features not supported by Serenity's markdown tooling.

### Sections

Consult the primary structure information to choose the correct main section for a new page. If a section contains many closely related pages, or contains many pages in general, it may be worth moving them to a new subsection. Each subsection must have its own page which at minimum explains the purpose of the subsection and lists all pages contained within it.

### Links

It is allowed to link online resources in manpages.

For resources specific to the manpage (for example, application screenshots), it is recommended to include these resources in the manpage folder next to the manpage. Other resources should be part of the SerenityOS [base files](../Base) and referenced via an absolute path (for example `/res/graphics/map.png`). Both kinds of paths will be converted to work correctly on [man.serenityos.org](https://man.serenityos.org).

Links to other man pages must be provided via the `help` pseudo-scheme:

```
help://man/section/page
```

`section` is a main section number and `page` is a page name, possibly including sub-sections. For example: `help://man/1/Applications/FontEditor` or `help://man/7/boot_parameters`. Using this format is enforced by the manpage linter.

### Headings

The highest heading level of manpages is 2 (`##`).

## Style and language

The general project guidelines on human language and text style apply to manpages as well. Titles must be written in sentence case instead of book title case.

Examples for described concepts, especially when complex, are always appreciated to help users understand a new concept.

### Name

A manpage always begins with a `## Name` heading that contains a maximum of one sentence naming the page. For most pages, the format of this should be

```md
Short title - full name or single-sentence descriptor
```

For example: `INI - generic config file format (.ini)` (full name for an abbreviation) or `w - Show information about currently logged-in users` (describes an application as briefly as possible). A period should only be included if a proper sentence is used instead of the dash format.

If the page describes an application with an icon, the 16x16 icon must be linked with alt text "Icon" inline before the textual name. For example: `![Icon](/res/icons/16x16/certificate.png) Certificate Settings`.

### Structure

All manpages should contain at least the following three sections:

```md
## Name

The manpage name, in the style described above.

## Description

Main text of the manpage.

## See also

List of related pages.
```

The `See also` section should contain related manpages, and may also link to relevant external pages. Even if pages are linked within the main prose, they should still be listed here if they are relevant to the entire page topic. Manpage links in `See also` should go both ways (page A links to page B, and B also links to A) to ease navigation within the manpage system. The `See also` section may be omitted if there's no related pages.

#### Applications

Applications in sections 1 and 8, especially command-line ones, must adhere to the following section structure.

```md
## Name

program name - single-sentence descriptor of the program

## Synopsis

Usage synopsis for invoking the program, within a `sh` code block.

## Description

Prose description of the program.

## Options

A list of optional arguments (flags) the program takes, with a brief description of each. More complex description, especially when options interact, should be part of the Description section. May be omitted if the program has no options.

## Arguments

A list of (required) arguments the program takes, in the same format as the Options section. May be omitted if the program has no arguments.

## Examples

Program invocation examples with resulting output.

## See also

List of related pages.
```

Other descriptive sections may be included as a subsection of `Description`, or directly after this section.
