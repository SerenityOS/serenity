# serenity-dsl-syntaxhighlight – Syntax highlighting for SerenityOS' DSLs

Syntax highlighting for SerenityOS' Domain Specific Languages:

* `.ipc`: Endpoint specification for the Inter Process Communication protocol.
* `.gml`: Graphical Markup Language for creating SerenityOS GUI application layouts.

## Features

Provides TextMate Grammar-based syntax highlighting for the IPC and GML languages. Syntax highlighting is mostly compliant with SerenityOS' own syntax highlighters (in the case of GML) and code generators (in the case of IPC).

### GML syntax highlighting
![](./img/gml-highlight.png)
### IPC syntax highlighting
![](./img/ipc-highlight.png)

### GML formatting

Allows formatting GML files with SerenityOS's own GML formatter. This is accomplished via the Lagom build of gml-format, so in order for the formatter to work, you need to build Lagom (see the options for the `Meta/serenity.sh` main script) and your workspace directory needs to be the Serenity root itself in order for the GML formatter to be found under Build/lagom/gml-format.

## Known Issues

GML uses the .gml extension, which is also used for the GameMaker language. You may have to set the language for each file manually.

The GML formatter needs to save the file in order to format it, so formatting and keeping a file unsaved is not currently possible.
