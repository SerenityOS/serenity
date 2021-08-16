The four examples in this directory show how to use some of the
features of the JTable component.

TableExample:
   This application includes a GUI for configuring the
   database connection and specifying the query.
TableExample2:
   The query and database connection are specified at the command
   line.  The results are displayed in a JTable.
TableExample3:
   Is a minimal example showing how to plug a generic sorter into the
   JTable.
TableExample4:
   Uses specialized renderers and editors.

TableExample3 and TableExample4 do not depend on database connectivity
and can be compiled and run in the normal way.

The most interesting example is probably TableExample, which has a
TextArea that can be used as an editor for an SQL expression.  Pressing
the Fetch button sends the expression to the database.  The results are
displayed in the JTable underneath the text area.

To run TableExample and TableExample2, you need to find a driver for
your database and set the environment variable JDBCHOME to a directory
where the driver is installed.  See the following URL for a list of
JDBC drivers provided by third party vendors:

  http://java.sun.com/products/jdbc/drivers.html

Once you find the driver, you can run one of the database examples by
specifying a class path that includes the JDBC classes and the example
classes.

For example:

  java -classpath $(JDBCHOME):TableExample.jar TableExample

These instructions assume that this installation's version of the java
command is in your path.  If it isn't, then you should either
specify the complete path to the java command or update your
PATH environment variable as described in the installation
instructions for the Java(TM) SE Development Kit.

