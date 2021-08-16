/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/**
 * This framework allows one to automate parsing of the test command line
 * arguments (it automatically sets the corresponding fields of the
 * test class).
 <p>
<h3>A simplified example</h3>
Suppose we want to to define a test Test with an option "iterations",
which can be run via
</p>
<pre> > java Test -iterations 10 </pre>
or via
<pre> java Test </pre>
in the last case iterations defaults to 100.
<p>
We want to achieve this by annotating fields of the Test class by
a special @Option annotation.
<p>
For simplicity suppose @Option is defined as follows:
<pre>
 &#064;interface Option
 { //here all the annotation fields are mandatory
        String name();
        String default();
        String description();
 }
 </pre>
 The test class uses an API like:
 <pre>
     public class OptionSupport {
     public static void setup(Object test, String[] args);
 }
</pre>
Now a simple example:
<pre>
public class Test {

    &#064;Option( name="iterations",
                 default="100",
                 description="Number of iterations")
    int iterations;
    public void run() {
        // ..do actual testing here..
    }

    public static void main(String args) {
        Test test = new Test();
        OptionsSupport.setup(test, args); // instead of manually
                      // parsing arguments
        // now test.iterations is set to 10 or 100.
        test.run();
    }
}
</pre>
This test can be also run via
<pre>
- java Test -help
</pre>
Then OptionSupport.setup() shows
help and exits (by throwing exception?):
<pre>
   Supported options:
    -iterations <number>
              Number of iterations (mandatory)
</pre>
We also want to be able to apply this to fields of non-simple types (via
factories) and  to other classes recursively (see @Options annotation
below).
<p>
Please, see {@link vm.share.options.test.SimpleExample}
for a working version of this.
 * <h3> General description </h3>
 Options are defined using annotations like this:
<pre>
public class StressOptions {
    // [2]
    &#064;Option(name="stressTime",
                default_value="30",
                description="Stress time")
    private long stressTime;

    ...
}
</pre>
<p> we want to use command line like
<pre> java Test -stressTime 50 </pre>
here 50 is passed to the StressOptions.stressTime field.
see {@link vm.share.options.Options} below. </p>
<pre>
public class Test {
    // [1]
    &#064;Options
    StressOptions stressOptions = new StressOptions();

    // [2]
    &#064;Option(name="iterations",
                default_value="100",
                description="Number of iterations")
    int iterations;

    // [3]
    &#064;Option(
        name="garbageProducer",
        default="byteArr",
        description="Garbage  producer",
        factory="nsk.share.gc.gp.GarbageProducerFactory")
    GarbageProducer garbageProducer;
...

    // [4]
    &#064;Option(name="logger",
         description="Logger",
     factory="nsk.share.log.LogFactory")
    Log log;

    public void run() {
        log.info("Start test");
        log.info("Finish test");
    }

    public static void main(String[] args) {
        Test test = new Test();
        OptionsSupport.setup(test, args);
        test.run();
    }
}
</pre>
<p> The API is invoked via a call to {@link vm.share.options.OptionSupport#setup(Object, String[])}).
 Also there is {@link vm.share.options.OptionSupport#setup(Object, String[], OptionHandler)}) method.
 It allows the caller to pass a handler which takes
 care of the options not defined via @Option annotation.
</p>

<h3>Requirements</h3>
<p>
 - The following field types are supported out-of-box: [2]
   <ul>
   <li/>All basic (primitive) types (int, long, ...) and corresponding wrapper types.
   <li/> In the case of a boolean type option user is allowed to skip second argument,
        i.e. write '-option_name'
        instead of '-option_name true'.
   <li/> Strings.
   <li/> One dimentional arrays of the above (comma-separated).
   <li/> Classes if a factory is specified, see below.
        <emph>NOTE: currently there is no way to pass some options to the instantiated objects.</emph>
   </ul>
</p>
<p> All non-static fields (including private and protected) of class and
    it's superclasses are scanned for annotations.
</p>
<p>
 (Possibly) Same annotations for setter methods ? (NOT IMPLEMENTED)
</p>
<p>
  It is possible to inherit options of the field type
  through @Options annotations, see {@link vm.share.options.Options}, and [1] above.
</p>
<p>
 Option.name defaults to the name of the field.
</p>
<p> Object options are supported using {@link vm.share.options.OptionObjectFactory}, see [3] above.
Please see {@link vm.share.options.OptionObjectFactory} interface, it should be
implemented by the user and specified in the Option.factory attribute.
</p>
<p>
As a shortcut we provide BasicOptionObjectFactory class, which allows user to
create a factory via @{@link vm.share.options.Factory} annotation:
<pre>
&#064;Factory (
    placeholder_text="garbage producer", //used for generating <..> in the help message
    default_value="byteArr",
    classlist={
        &#064;FClass(key="byteArr",
                     type="nsk.share.gc.gp.array.ByteArrayProducer",
                     description="byte array producer")
        &#064;FClass(key="charArr",
                     type="nsk.share.gc.gp.array.CharArrayProducer",
                     description="char array producer")
        ...
    }
)
public class GarbageProducerFactory extends BasicOptionObjectFactory {
}
</pre>
<p> <emph> note: for subclasses of BasicOptionObjectFactory
factories can extend each other!!
so the check for @OptionObjectFactory is done recursively.
NOT SURE IF THIS IS IMPLEMENTED.
</emph></p>
<p> If there is no unknownOptionHandler then in case of unsupported
option, a Runtime exception is thrown.
</p>
<p>
 If there is no 'default' annotation attribute and there is no default
for OptionObjectFactory, then option is mandatory and a Runtime exception is thrown if
it's missing.
</p>
 <p> Both '-option value' and '-option=value' formats are supported.
 </p>
<p> If main class is given '-help', OptionSupport.setup() shows
help and exits (by throwing a Runtime exception):
<pre>
Supported options:
    -iterations <number>
              Number of iterations (mandatory)
        -stressTime <number>
              Stress time (default 30)
    -garbageProducer <garbage producer>
              Garbage producer (default byteArr). Supported keys:
                  byteArr    byte array producer
                  charArr    char array producer
                  ...
        ...
</pre>

<p> <emph> NOT IMPLEMENTED: </emph>
Integer type boundaries are validated with RuntimeException with detailed
meaningful error message. Other validations that are possible are also done.
</p>
 <p> <emph> NOT IMPLEMENTED: </emph>
 Empty default ("") value for Object annotations [3] means null value.
</p>
<p> The object created via factory is also scanned for @Option annotations
(like in the @Options case), i.e. it inherits options from the Test class.
<emph> This is not implemented as it causes several problems, in particular, the
object must be instanciated in order to be scanned for options, hence no meaningful
help message could be generated for corresponding options.
One of the possible solutions
is to allow -object_opt_name.suboption syntax (or even with a colon), and to pass the
corresponding suboptions Map to the OptionObjectFactory, it could use OptionFramework
to take care of it. It is unclear, what other problems can arise.</emph>
</p>
 *
 */
package vm.share.options;
