Font2DTest
-----------

To run Font2DTest:

% java -jar Font2DTest.jar
    or 
% appletviewer Font2DTest.html

These instructions assume that the 1.7 versions of the java
and appletviewer commands are in your path.  If they aren't,
then you should either specify the complete path to the commands
or update your PATH environment variable as described in the
installation instructions for the Java(TM) SE Development Kit.

To view Font2DTest within a web browser with Java Plugin,
load Font2DTest.html.

If you wish to modify any of the source code, you may want to extract
the contents of the Font2DTest.jar file by executing this command:

% jar -xvf Font2DTest.jar

NOTE:

When Font2DTest is ran as an applet, the browser plugin/viewer needs
following permissions given in order to run properly:

AWTPermission     "showWindowWithoutWarningBanner"
RuntimePermission "queuePrintJob"

The program will run without these properties set,
but some of its features will be limited.
To enable all features, please add these permissions.

-----------------------------------------------------------------------
Introduction
-----------------------------------------------------------------------

Font2DTest is an encompassing application for testing various fonts
found on the user's system.  A number of controls are available to 
change many attributes of the current font including style, size, and
rendering hints.  The user can select from multiple display modes,
such as one Unicode range at a time, all glyphs of a particular font, 
user-edited text, or text loaded from a file. 
In addition, the user can control which method will
be used to render the text to the screen (or to be printed out).

-----------------------------------------------------------------------
Tips on usage 
----------------------------------------------------------------------- 

- The "Font" combobox will show a tick mark if some of the characters in 
selected unicode range can be displayed by this font. No tick is shown, 
if none of the characters can be displayed. A tooltip is shown with this 
information. This indication is available only if "Unicode Range" is 
selected in "Text to use" combobox.

This feature is enabled by default. For disabling this feature, use 
command line flag -disablecandisplaycheck or -dcdc.

java -jar Font2DTest.jar -dcdc

- For the "Font Size" field to have an effect, it is necessary to press
ENTER when finished inputting data in those fields.

- When "Unicode Range" or "All Glyphs" is selected for Text to Use,
the status bar will show the range of the characters that is
currently being displayed. If mouse cursor is pointed to one of
the character drawn, the message will be changed to indicate
what character the cursor is pointing to.
By clicking on a character displayed, one can also "Zoom" a character.
Options can be set to show grids around each character,
or force the number of characters displayed across the screen to be 16.
These features are not available in "User Text" or "File Text" mode.

- The default number of columns in a Unicode Range or All Glyphs drawing
is "fit as many as possible". If this is too hard to read, then you
can force number of columns to be 16. However, this will not resize the
window to fit all 16 columns, so if the font size is too big, this will
overflow the canvas. (Unfortunately, I could not add horizontal space
bar due to design restrictions)

- If font size is too large to fit a character, then a message will
inform that smaller font size or larger canvas size is needed.

- Custom Unicode Range can be displayed by selecting "Custom..."
at the bottom of the Unicode Range menu. This will bring up
a dialog box to specify the starting and ending index
of the unicode characters to be drawn.

- To enter a customized text, select "User Text" from Text to Use menu.
A dialog box with a text area will come up. Enter any text here,
and then press update; the text on screen will then be redrawn to
draw the text just entered. To hide the user text dialog box,
switch to different selection in Text to Use menu.
(Closing the dialog box will not work...)
If a escape sequence of form \uXXXX is entered, it is will be
converted into the character that it maps to.

- drawBytes will only work for characters in Unicode range 0x00-0xFF
by its method definition. This program will warn when such text is
being drawn in "Range Text" mode. But since there is no way to detect
this from User Text, the warning will not be given even though
wrong text seems to be drawn on screen when it contains any character
beyond 0xFF.

- In the "All Glyphs" mode which displays all available  glyphs for the
current font, only drawGlyphVector is available as the draw method.
Similary, when "Text File" mode is used, the file will always be wrapped
to canvas width using LineBreakMeasurer, so TextLayout.draw is used.

- With "User Text" mode, no text wrapping operation is done.
When displaying or printing text that does not fit in a given canvas,
the text will overflow to the right side of the page.

- It is also possible to display a text loaded from a file.
Font2DTest will handle is UTF-16 and the platform default encoding.
The text will then be reformatted to fit in the screen with
LineBreakMeasurer, and drawn with TextLayout.draw.
Most major word processor softwares support this format.

- When printing, the program will ask to select 1 of 3 options.
First "Print one full page..." will print as much
characters/lines of text as it can fit in one page, starting from
the character/line that is currently drawn at the top of the page.
Second option, "Print all characters..." will print all characters
that are within the selected range. Third option, "Print all text..."
is similar, and it will print all lines of text that user has put in.

====================================================================

Known Problems:

- When a PostScript font is used, the characters may extend beyond the
enclosing grid or zoom rectangle. This is due to the problem with
FontMetrics.getMaxAscent() and getMaxDescent() functions; the functions
do not always return the right values for PostScript fonts.

- There are still some bugs around the error handling.
Most of these problems will usually get fixed when some parameters
are changed, or the screen is refreshed.

- Many fonts on Solaris fails to retrieve outlines properly,
and as the result, they do not align within the grid properly.
These are mainly F3 and fonts that was returned by X server.

- When showWindowWithoutWarningBanner AWTPermission is not given,
the "zoom" window will look really bad because of the
Applet warning label tacked at the bottom of the zoom window.
To remove this, follow the "NOTE:" instruction at the top.
