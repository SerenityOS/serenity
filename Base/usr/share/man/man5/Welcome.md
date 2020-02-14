# Name
Welcome - list of entries for the Welcome program

# Synopsis
`/res/welcome.txt`

# Description
Welcome's configuration file specifies the information that Welcome should show
to new users to onboard them with the SerenityOS system. It is based on a simple
line-by-line format.

**All lines must be less than 4096 bytes in length.**

# Contents
The file consists of lines, where each line has two parts: a one-character
specifier `$spec`, to indicate the type of line, and the remainder of the line
`$rem`. The remainder may have a space removed from the beginning, meaning that
you can use (for example) `* title` instead of `*title`.

## Line Types
Each line may be one of the following types:

* `*` (menu item) - Finishes the previous page (if applicable) and starts a new page, identified in the menu as `$rem`.
* `$` (icon) - Specifies the path to a PNG file to be used as the icon for the entry. The icon will always be displayed as 16x16.
* `>` (title) - Specifies the title that will be displayed in bold above the text. The icon will be displayed to the left, if there is one.
* `#` (comment) - Ignored.

## Content
All lines that are not one of the special types are considered content. These
lines are merged together unless separated by a blank line.

## Examples
```
# Create a new page 'Welcome'
* Welcome
# Set the icon to the Serenity ladybug
$ /res/icons/16x16/ladybug.png
# Set the page title to 'SerenityOS is great!'
> SerenityOS is great!

# Content goes here.
SerenityOS is a project that was started by @awesomekling.
Look, this will be put after that text!

And this is a new paragraph! :D
```
