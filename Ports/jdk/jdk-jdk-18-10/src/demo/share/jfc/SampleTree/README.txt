SampleTree demonstrates JTree features.  Each node of SampleTree has 7
children, with each one drawn in a random font and color.  Each node is
named after its font.  While the data isn't interesting, the example
illustrates a number of features:

- Dynamically loading children (see DynamicTreeNode.java)
- Adding/removing/inserting/reloading (see the following inner
  classes in SampleTree.java: AddAction, RemoveAction, InsertAction,
  and ReloadAction)
- Creating a custom cell renderer (see SampleTreeCellRenderer.java)
- Subclassing JTreeModel for editing (see SampleTreeModel.java)


To run the SampleTree demo:

  java -jar SampleTree.jar

These instructions assume that this installation's version of the java
command is in your path.  If it isn't, then you should either
specify the complete path to the java command or update your
PATH environment variable as described in the installation
instructions for the Java(TM) SE Development Kit.

