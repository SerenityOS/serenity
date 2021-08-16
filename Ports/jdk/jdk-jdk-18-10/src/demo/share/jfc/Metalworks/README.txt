About Metalworks
================
Metalworks is a simple Swing-based simulated e-mail
application.  It shows off several features of Swing, including
JInternalFrame, JTabbedPane, JFileChooser, JEditorPane, and
JRadioButtonMenuItem.  Metalworks is optimized to work with the
Java look and feel (codenamed "Metal") and shows use of several
features, such as themes, that are specific to the Java look and
feel.


Running Metalworks
==================

To run the Metalworks demo:

  java -jar Metalworks.jar

These instructions assume that this installation's version of the java
command is in your path.  If it isn't, then you should either
specify the complete path to the java command or update your
PATH environment variable as described in the installation
instructions for the Java(TM) SE Development Kit.


Metalworks Features
===================
The functionality of the Metalworks demo is minimal, and many
controls are non-functional.  They are intended only to show how
to construct the UI for such interfaces.  Things that do work in
the Metalworks demo include:

1. Choosing New from the File menu displays an e-mail
   composition window.

2. Choosing Open from the File menu brings up the file chooser.

3. Choosing Preferences from the Edit menu will bring up a
   dialog.  Most of this dialog is only for show.

4. Choosing About Metalworks from the Help menu brings up a
   JOptionPane with a brief description of the application.

5. Choosing Open Help Window from the Help menu brings up an
   internal frame that displays a set of HTML files containing
   all sorts of useful info.  Look through these for tips about
   using Metal.

6. The Theme menu allows you to change the color theme of the
   application.  The default theme (Steel) and several other
   demo themes are included.  Note that the themes can control
   not only the colors, but also the sizes of many controls.
   Also included with this demo is the PropertiesMetalTheme
   class, which allows you to read a theme's colors from a text
   file.  The Charcoal theme is an example of using this.

