## Name

man - SerenityOS manual system

## Description

The SerenityOS manual pages, or "man pages", document various parts of the operating system for users and developers. They are one of the two parts of the SerenityOS documentation.

The other part of the SerenityOS documentation is the developer documentation, which can be found in the `Documentation` folder in the repository ([online link](https://github.com/SerenityOS/serenity/tree/master/Documentation)). The developer documentation is focused on setting up a SerenityOS installation and workflow, as well as contributing to its development.

Note that documentation might cover standardized topics (such as standard POSIX C library functions) or SerenityOS-specific extensions (such as custom file formats). SerenityOS intends to be spec-complaint with industry standard specifications. Non-compliance with a particular specification should be documented in the relevant sections.

### Programs

There are three ways of accessing the manual pages:

-   [`Help`(1)](help://man/1/Applications/Help) provides a graphical user interface for the man pages.
-   [`man`(1)](help://man/1/man) implements the standard POSIX utility by accessing the man pages in the terminal.
-   You can also choose to open the underlying Markdown files (see [Organization](#organization)) directly.

### Organization

Each SerenityOS manual page is a Markdown (.md) file under [`/usr/share/man`](/usr/share/man). The [main sections](#sections) live in the subdirectories `man1` through `man8`. Subsections are found within these directories.

#### Sections

The SerenityOS manual is split into the following _sections_, or _chapters_:

1. User Programs - manuals for regular user applications and utilities
2. System Calls - SerenityOS system call interface documentation
3. Library Functions - SerenityOS C library function documentation
4. Special Files - documentation for pseudo-files of the SerenityOS virtual file system
5. File Formats - documentation for SerenityOS-specific file formats
6. Games - manuals for SerenityOS games
7. Miscellanea - various documentation that fits in no other category
8. Sysadmin Tools - manuals for services and utilities intended for system administration

Sections are subject to change in the future.

#### Subsections

Subsections exist to organize various large collections of topics within one main section. Subsections have their own pages (often a table of contents or general overview), so they are a category and a page at the same time. Subsections can be arbitrarily nested.

The currently existing subsections are not listed here, as they are subject to frequent change.

#### Naming

Manpages are named via standard POSIX convention, where the section number follows the page name in brackets. For example, this page is called `man(7)`, but there's also a page called `man(1)` (the _program_ named `man`) and a page named [`Mitigations(7)`](help://man/7/Mitigations). This naming convention also applies to subsections.

For pages in subsections, conventional directory notation with slashes is used. For example, the page `Widget/Button` in the subsection `GML(5)` has the full name `GML/Widget/Button(5)`.

When you open a page via command-line arguments, the section is specified separately before the page name, for example `7 man`, `1 man`, or `7 Mitigations`.

## See Also

-   [`man`(1)](help://man/1/man) To read manpages in the terminal
-   [`Help`(1)](help://man/1/Applications/Help) To read manpages in a GUI
