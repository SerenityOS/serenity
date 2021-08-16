The classes for the Java2D(TM) demo are contained in the J2Ddemo.jar file.  
To run the J2D demo:

% java -jar J2Ddemo.jar

-----------------------------------------------------------------------
Introduction
-----------------------------------------------------------------------

This Java2D demo consists of a set of demos housed in one GUI 
framework that uses a JTabbedPane.  You can access different groups of 
demos by clicking the tabs at the top of the pane. There are demo 
groups for Arcs_Curves, Clipping, Colors, Composite, Fonts, Images, 
Lines, Mix, Paint, Paths and Transforms.  On the right-hand side of the 
pane, the GUI framework features individual and global controls for 
changing graphics attributes. There's also a memory-usage monitor, and 
a monitor for tracking the performance, in frames per second, of 
animation demos.


-----------------------------------------------------------------------
Tips on usage 
----------------------------------------------------------------------- 

Click on one of the tabs at the top of the pane to select a demo group.  
When you select a group, a set of surfaces is displayed, each of which 
contains one of the group's demos. At the bottom of each surface is 
a set of tools for controlling the demo.  The tools can be displayed
by selecting the Tools checkbox in the Global Controls panel or
by clicking on the slim strip of gray bumps at the bottom of the demo
panel.

If you click on a demo surface, that demo is laid out by itself. A
new icon button will appear in the demo's tools toolbar one that enables 
you to create new instances of that demo's surface. 

To run the demo continuously without user interaction, select the 
Run Window item in the Options menu and press the run button in the 
new window that's displayed.  To do this from the command line:

    java -jar J2Ddemo.jar -runs=10

To view all the command line options for customizing demo runs:

    java -jar J2Ddemo.jar -help

You can run the demos in stand-alone mode by issuing a command like this

    java -cp J2Ddemo.jar java2d.demos.Clipping.ClipAnim

You can run the demos in groups by issuing a command like this

    java -cp J2Ddemo.jar java2d.DemoGroup Clipping    

To increase or decrease the Memory Monitor sampling rate click on the
Memory Monitor's title border, a panel with a TextField will appear.

The J2Ddemo Intro (the 'J2D demo' tab) contains a scene table, click in 
the gray border and a table will appear.

Animated demos have a slider to control the animation rate.  Bring up
the animated demo toolbar, then click in the gray area of the toolbar
panel, the toolbar goes away and the slider appears.

Demos that have Custom Controls can have their Custom Control Thread
activated and stopped by clicking in the gray area of the demos Custom 
Control panel.

-----------------------------------------------------------------------
NOTE about demo surfaces 
----------------------------------------------------------------------- 

The demo groups are in separate packages with their class files stored 
in directories named according to the demo group name.  All drawing 
demos extend either the Surface, AnimatingSurface, ControlsSurface or
AnimatingControlsSurface classes.  Surface is the base class, demos
must implement the Surface's render method.  All animated demos extend 
either the AnimatingSurface or the AnimatingControlsSurface classes.  
Animated demos must implement the reset and step methods.  The demos
with gui controls extend either the ControlsSurface or the 
AnimatingControlsSurface classes.  Demos with controls must implement
the methods in the CustomControlsContext interface.


======================================================================

Here are some resources for learning about and using the Java2D(TM)

OpenJDK group page: http://openjdk.java.net/groups/2d/

Learning Java 2D: http://www.oracle.com/technetwork/articles/java/java2dpart1-137217.html

Tutorial : http://download.oracle.com/javase/tutorial/2d/
Specification: http://download.oracle.com/javase/8/docs/technotes/guides/2d/spec/j2d-bookTOC.html
Java 2D (TM) API White Paper : http://www.oracle.com/technetwork/java/javase/tech/2d-142228.html
2D FAQ: http://www.oracle.com/technetwork/java/index-137037.html
