/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package javax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;

// remaining imports are for Javadoc
import java.io.InvalidObjectException;
import java.lang.management.MemoryUsage;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataInvocationHandler;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanInfo;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
    <p>Annotation to mark an interface explicitly as being an MXBean
    interface, or as not being an MXBean interface.  By default, an
    interface is an MXBean interface if it is public and its name ends
    with {@code MXBean}, as in {@code SomethingMXBean}.  The following
    interfaces are MXBean interfaces:</p>

    <pre>
    public interface WhatsitMXBean {}

    &#64;MXBean
    public interface Whatsit1Interface {}

    &#64;MXBean(true)
    public interface Whatsit2Interface {}
    </pre>

    <p>The following interfaces are not MXBean interfaces:</p>

    <pre>
    interface NonPublicInterfaceNotMXBean{}

    public interface Whatsit3Interface{}

    &#64;MXBean(false)
    public interface MisleadingMXBean {}
    </pre>

    <h2 id="MXBean-spec">MXBean specification</h2>

    <p>The MXBean concept provides a simple way to code an MBean
      that only references a predefined set of types, the ones defined
      by {@link javax.management.openmbean}.  In this way, you can be
      sure that your MBean will be usable by any client, including
      remote clients, without any requirement that the client have
      access to <em>model-specific classes</em> representing the types
      of your MBeans.</p>

    <p>The concepts are easier to understand by comparison with the
      Standard MBean concept.  Here is how a managed object might be
      represented as a Standard MBean, and as an MXBean:</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
public interface MemoryPool<b>MBean</b> {
    String getName();
    MemoryUsage getUsage();
    // ...
}
          </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
public interface MemoryPool<b>MXBean</b> {
    String getName();
    MemoryUsage getUsage();
    // ...
}
          </pre>
    </div>

    <p>As you can see, the definitions are very similar.  The only
      difference is that the convention for naming the interface is to use
      <code><em>Something</em>MXBean</code> for MXBeans, rather than
      <code><em>Something</em>MBean</code> for Standard MBeans.</p>

    <p>In this managed object, there is an attribute called
      <code>Usage</code> of type {@link MemoryUsage}.  The point of an
      attribute like this is that it gives a coherent snapshot of a set
      of data items.  For example, it might include the current amount
      of used memory in the memory pool, and the current maximum of the
      memory pool.  If these were separate items, obtained with separate
      {@link MBeanServer#getAttribute getAttribute} calls, then we could
      get values seen at different times that were not consistent.  We
      might get a <code>used</code> value that was greater than the
      <code>max</code> value.</p>

    <p>So, we might define <code>MemoryUsage</code> like this:</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
public class MemoryUsage <b>implements Serializable</b> {
    // standard JavaBean conventions with getters

    public MemoryUsage(long init, long used,
                       long committed, long max) {...}
    long getInit() {...}
    long getUsed() {...}
    long getCommitted() {...}
    long getMax() {...}
}
        </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
public class MemoryUsage {
    // standard JavaBean conventions with getters
    <b>&#64;ConstructorParameters({"init", "used", "committed", "max"})</b>
    public MemoryUsage(long init, long used,
                       long committed, long max) {...}
    long getInit() {...}
    long getUsed() {...}
    long getCommitted() {...}
    long getMax() {...}
}
        </pre>
    </div>

    <p>The definitions are the same in the two cases, except
      that with the MXBean, <code>MemoryUsage</code> no longer needs to
      be marked <code>Serializable</code> (though it can be).  On
      the other hand, we have added a {@link ConstructorParameters &#64;ConstructorParameters}
      annotation to link the constructor parameters to the corresponding getters.
      We will see more about this below.</p>

    <p><code>MemoryUsage</code> is a <em>model-specific class</em>.
      With Standard MBeans, a client of the MBean Server cannot access the
      <code>Usage</code> attribute if it does not know the class
      <code>MemoryUsage</code>.  Suppose the client is a generic console
      based on JMX technology.  Then the console would have to be
      configured with the model-specific classes of every application it
      might connect to.  The problem is even worse for clients that are
      not written in the Java language.  Then there may not be any way
      to tell the client what a <code>MemoryUsage</code> looks like.</p>

    <p>This is where MXBeans differ from Standard MBeans.  Although we
      define the management interface in almost exactly the same way,
      the MXBean framework <em>converts</em> model-specific classes into
      standard classes from the Java platform.  Using arrays and the
      {@link javax.management.openmbean.CompositeData CompositeData} and
      {@link javax.management.openmbean.TabularData TabularData} classes
      from the standard {@link javax.management.openmbean} package, it
      is possible to build data structures of arbitrary complexity
      using only standard classes.</p>

    <p>This becomes clearer if we compare what the clients of the two
      models might look like:</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
String name = (String)
    mbeanServer.{@link MBeanServer#getAttribute
    getAttribute}(objectName, "Name");
<b>MemoryUsage</b> usage = (<b>MemoryUsage</b>)
    mbeanServer.getAttribute(objectName, "Usage");
<b>long used = usage.getUsed();</b>
        </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
String name = (String)
    mbeanServer.{@link MBeanServer#getAttribute
    getAttribute}(objectName, "Name");
<b>{@link CompositeData}</b> usage = (<b>CompositeData</b>)
    mbeanServer.getAttribute(objectName, "Usage");
<b>long used = (Long) usage.{@link CompositeData#get get}("used");</b>
        </pre>
    </div>

    <p>For attributes with simple types like <code>String</code>, the
      code is the same.  But for attributes with complex types, the
      Standard MBean code requires the client to know the model-specific
      class <code>MemoryUsage</code>, while the MXBean code requires no
      non-standard classes.</p>

    <p>The client code shown here is slightly more complicated for the
      MXBean client.  But, if the client does in fact know the model,
      here the interface <code>MemoryPoolMXBean</code> and the
      class <code>MemoryUsage</code>, then it can construct a
      <em>proxy</em>.  This is the recommended way to interact with
      managed objects when you know the model beforehand, regardless
      of whether you are using Standard MBeans or MXBeans:</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
MemoryPool<b>MBean</b> proxy =
    JMX.<b>{@link JMX#newMBeanProxy(MBeanServerConnection, ObjectName,
              Class) newMBeanProxy}</b>(
        mbeanServer,
        objectName,
        MemoryPool<b>MBean</b>.class);
String name = proxy.getName();
MemoryUsage usage = proxy.getUsage();
long used = usage.getUsed();
          </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
MemoryPool<b>MXBean</b> proxy =
    JMX.<b>{@link JMX#newMXBeanProxy(MBeanServerConnection, ObjectName,
              Class) newMXBeanProxy}</b>(
        mbeanServer,
        objectName,
        MemoryPool<b>MXBean</b>.class);
String name = proxy.getName();
MemoryUsage usage = proxy.getUsage();
long used = usage.getUsed();
          </pre>
    </div>

    <p>Implementing the MemoryPool object works similarly for both
      Standard MBeans and MXBeans.</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
public class MemoryPool
        implements MemoryPool<b>MBean</b> {
    public String getName() {...}
    public MemoryUsage getUsage() {...}
    // ...
}
        </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
public class MemoryPool
        implements MemoryPool<b>MXBean</b> {
    public String getName() {...}
    public MemoryUsage getUsage() {...}
    // ...
}
        </pre>
    </div>

    <p>Registering the MBean in the MBean Server works in the same way
      in both cases:</p>

    <div style="display:inline-block; margin: 0 3em">
        <h3>Standard MBean</h3>
        <pre>
{
    MemoryPool<b>MBean</b> pool = new MemoryPool();
    mbeanServer.{@link MBeanServer#registerMBean
    registerMBean}(pool, objectName);
}
        </pre>
    </div>
    <div style="display:inline-block; margin: 0 3em">
        <h3>MXBean</h3>
        <pre>
{
    MemoryPool<b>MXBean</b> pool = new MemoryPool();
    mbeanServer.{@link MBeanServer#registerMBean
    registerMBean}(pool, objectName);
}
        </pre>
    </div>


    <h2 id="mxbean-def">Definition of an MXBean</h2>

    <p>An MXBean is a kind of MBean.  An MXBean object can be
      registered directly in the MBean Server, or it can be used as an
      argument to {@link StandardMBean} and the resultant MBean
      registered in the MBean Server.</p>

    <p>When an object is registered in the MBean Server using the
      {@code registerMBean} or {@code createMBean} methods of the
      {@link MBeanServer} interface, the object's class is examined
      to determine what type of MBean it is:</p>

    <ul>
      <li>If the class implements the interface {@link DynamicMBean}
        then the MBean is a Dynamic MBean.  Note that the class
        {@code StandardMBean} implements this interface, so this
        case applies to a Standard MBean or MXBean created using
        the class {@code StandardMBean}.</li>

      <li>Otherwise, if the class matches the Standard MBean naming
        conventions, then the MBean is a Standard MBean.</li>

      <li>Otherwise, it may be an MXBean.  The set of interfaces
        implemented by the object is examined for interfaces that:

        <ul>
          <li>have a class name <code><em>S</em>MXBean</code> where
            <code><em>S</em></code> is any non-empty string, and
            do not have an annotation {@code @MXBean(false)}; and/or</li>
          <li>have an annotation {@code @MXBean(true)}
            or just {@code @MXBean}.</li>
        </ul>

        If there is exactly one such interface, or if there is one
        such interface that is a subinterface of all the others, then
        the object is an MXBean.  The interface in question is the
        <em>MXBean interface</em>.  In the example above, the MXBean
        interface is {@code MemoryPoolMXBean}.

      <li>If none of these conditions is met, the MBean is invalid and
        the attempt to register it will generate {@link
        NotCompliantMBeanException}.
    </ul>

    <p>Every Java type that appears as the parameter or return type of a
      method in an MXBean interface must be <em>convertible</em> using
      the rules below.  Additionally, parameters must be
      <em>reconstructible</em> as defined below.</p>

    <p>An attempt to construct an MXBean that does not conform to the
      above rules will produce an exception.</p>


    <h2 id="naming-conv">Naming conventions</h2>

    <p>The same naming conventions are applied to the methods in an
      MXBean as in a Standard MBean:</p>

    <ol>
      <li>A method <code><em>T</em> get<em>N</em>()</code>, where
        <code><em>T</em></code> is a Java type (not <code>void</code>)
        and <code><em>N</em></code> is a non-empty string, specifies
        that there is a readable attribute called
        <code><em>N</em></code>.  The Java type and Open type of the
        attribute are determined by the mapping rules below.
        The method {@code final Class getClass()} inherited from {@code
        Object} is ignored when looking for getters.</li>

      <li>A method <code>boolean is<em>N</em>()</code> specifies that
        there is a readable attribute called <code><em>N</em></code>
        with Java type <code>boolean</code> and Open type
        <code>SimpleType.Boolean</code>.</li>

      <li>A method <code>void set<em>N</em>(<em>T</em> x)</code>
        specifies that there is a writeable attribute called
        <code><em>N</em></code>.  The Java type and Open type of the
        attribute are determined by the mapping rules below.  (Of
        course, the name <code>x</code> of the parameter is
        irrelevant.)</li>

      <li>Every other method specifies that there is an operation with
        the same name as the method.  The Java type and Open type of the
        return value and of each parameter are determined by the mapping
        rules below.</li>
    </ol>

    <p>The rules for <code>get<em>N</em></code> and
      <code>is<em>N</em></code> collectively define the notion of a
      <em>getter</em>.  The rule for <code>set<em>N</em></code> defines
      the notion of a <em>setter</em>.</p>

    <p>It is an error for there to be two getters with the same name, or
      two setters with the same name.  If there is a getter and a setter
      for the same name, then the type <code><em>T</em></code> in both
      must be the same.  In this case the attribute is read/write.  If
      there is only a getter or only a setter, the attribute is
      read-only or write-only respectively.</p>


    <h2 id="mapping-rules">Type mapping rules</h2>

    <p>An MXBean is a kind of Open MBean, as defined by the {@link
      javax.management.openmbean} package.  This means that the types of
      attributes, operation parameters, and operation return values must
      all be describable using <em>Open Types</em>, that is the four
      standard subclasses of {@link javax.management.openmbean.OpenType}.
      MXBeans achieve this by mapping Java types into Open Types.</p>

    <p>For every Java type <em>J</em>, the MXBean mapping is described
      by the following information:</p>

    <ul>
      <li>The corresponding Open Type, <em>opentype(J)</em>.  This is
        an instance of a subclass of {@link
        javax.management.openmbean.OpenType}.</li>
      <li>The <em>mapped</em> Java type, <em>opendata(J)</em>, which is
        always the same for any given <em>opentype(J)</em>.  This is a Java
        class.</li>
      <li>How a value is converted from type <em>J</em> to type
        <em>opendata(J)</em>.</li>
      <li>How a value is converted from type <em>opendata(J)</em> to
        type <em>J</em>, if it can be.</li>
    </ul>

    <p>For example, for the Java type {@code List<String>}:</p>

    <ul>
      <li>The Open Type, <em>opentype(</em>{@code
        List<String>}<em>)</em>, is {@link ArrayType}<code>(1, </code>{@link
          SimpleType#STRING}<code>)</code>, representing a 1-dimensional
          array of <code>String</code>s.</li>
      <li>The mapped Java type, <em>opendata(</em>{@code
        List<String>}<em>)</em>, is {@code String[]}.</li>
      <li>A {@code List<String>} can be converted to a {@code String[]}
          using {@link List#toArray(Object[]) List.toArray(new
          String[0])}.</li>
      <li>A {@code String[]} can be converted to a {@code List<String>}
          using {@link Arrays#asList Arrays.asList}.</li>
    </ul>

    <p>If no mapping rules exist to derive <em>opentype(J)</em> from
      <em>J</em>, then <em>J</em> cannot be the type of a method
      parameter or return value in an MXBean interface.</p>

    <p id="reconstructible-def">If there is a way to convert
      <em>opendata(J)</em> back to <em>J</em> then we say that <em>J</em> is
      <em>reconstructible</em>.  All method parameters in an MXBean
      interface must be reconstructible, because when the MXBean
      framework is invoking a method it will need to convert those
      parameters from <em>opendata(J)</em> to <em>J</em>.  In a proxy
      generated by {@link JMX#newMXBeanProxy(MBeanServerConnection,
      ObjectName, Class) JMX.newMXBeanProxy}, it is the return values
      of the methods in the MXBean interface that must be
      reconstructible.</p>

    <p>Null values are allowed for all Java types and Open Types,
      except primitive Java types where they are not possible.  When
      converting from type <em>J</em> to type <em>opendata(J)</em> or
      from type <em>opendata(J)</em> to type <em>J</em>, a null value is
      mapped to a null value.</p>

    <p>The following table summarizes the type mapping rules.</p>

    <table class="striped">
    <caption style="display:none">Type Mapping Rules</caption>
      <thead>
      <tr>
        <th scope="col">Java type <em>J</em></th>
        <th scope="col"><em>opentype(J)</em></th>
        <th scope="col"><em>opendata(J)</em></th>
      </tr>
      </thead>
      <tbody style="text-align:left; vertical-align:top">
        <tr>
          <th scope="row">{@code int}, {@code boolean}, etc<br>
            (the 8 primitive Java types)</th>
          <td>{@code SimpleType.INTEGER},<br>
            {@code SimpleType.BOOLEAN}, etc</td>
          <td>{@code Integer}, {@code Boolean}, etc<br>
            (the corresponding boxed types)</td>
        </tr>
        <tr>
          <th scope="row">{@code Integer}, {@code ObjectName}, etc<br>
            (the types covered by {@link SimpleType})</th>
          <td>the corresponding {@code SimpleType}</td>
          <td><em>J</em>, the same type</td>
        </tr>
        <tr>
          <th scope="row">{@code int[]} etc<br>
            (a one-dimensional array with primitive element type)</th>
          <td>{@code ArrayType.getPrimitiveArrayType(int[].class)} etc</td>
          <td><em>J</em>, the same type</td>
        <tr>
          <th scope="row"><em>E</em>{@code []}<br>
            (an array with non-primitive element type <em>E</em>;
              this includes {@code int[][]}, where <em>E</em> is {@code int[]})</th>
          <td>{@code ArrayType.getArrayType(}<em>opentype(E)</em>{@code )}</td>
          <td><em>opendata(E)</em>{@code []}</td>
        </tr>
        <tr>
          <th scope="row">{@code List<}<em>E</em>{@code >}<br>
            {@code Set<}<em>E</em>{@code >}<br>
            {@code SortedSet<}<em>E</em>{@code >} (see below)</th>
          <td>same as for <em>E</em>{@code []}</td>
          <td>same as for <em>E</em>{@code []}</td>
        </tr>
        <tr>
          <th scope="row">An enumeration <em>E</em><br>
            (declared in Java as {@code enum }<em>E</em>
            {@code {...}})</th>
          <td>{@code SimpleType.STRING}</td>
          <td>{@code String}</td>
        </tr>
        <tr>
          <th scope="row">{@code Map<}<em>K</em>,<em>V</em>{@code >}<br>
            {@code SortedMap<}<em>K</em>,<em>V</em>{@code >}</th>
          <td>{@link TabularType}<br>
            (see below)</td>
          <td>{@link TabularData}<br>
            (see below)</td>
        </tr>
        <tr>
          <th scope="row">{@linkplain Record Record classes}</th>
          <td>{@link CompositeType}, if possible<br>
            (see below)</td>
          <td>{@link CompositeData}<br>
            (see below)</td>
        </tr>
        <tr>
          <th scope="row">An MXBean interface</th>
          <td>{@code SimpleType.OBJECTNAME}<br>
            (see below)</td>
          <td>{@link ObjectName}<br>
            (see below)</td>
        </tr>
        <tr>
          <th scope="row">Any other type</th>
          <td>{@link CompositeType},
            if possible<br>
            (see below)</td>
          <td>{@link CompositeData}<br>
            (see below)</td>
      </tbody>
    </table>

    <p>The following sections give further details of these rules.</p>


    <h3>Mappings for primitive types</h3>

    <p>The 8 primitive Java types
      ({@code boolean}, {@code byte}, {@code short}, {@code int}, {@code
      long}, {@code float}, {@code double}, {@code char}) are mapped to the
      corresponding boxed types from {@code java.lang}, namely {@code
      Boolean}, {@code Byte}, etc.  The Open Type is the corresponding
      {@code SimpleType}.  Thus, <em>opentype(</em>{@code
      long}<em>)</em> is {@code SimpleType.LONG}, and
      <em>opendata(</em>{@code long}<em>)</em> is {@code
      java.lang.Long}.</p>

    <p>An array of primitive type such as {@code long[]} can be represented
      directly as an Open Type.  Thus, <em>openType(</em>{@code
      long[]}<em>)</em> is {@code
      ArrayType.getPrimitiveArrayType(long[].class)}, and
      <em>opendata(</em>{@code long[]}<em>)</em> is {@code
      long[]}.</p>

    <p>In practice, the difference between a plain {@code int} and {@code
      Integer}, etc, does not show up because operations in the JMX API
      are always on Java objects, not primitives.  However, the
      difference <em>does</em> show up with arrays.</p>


    <h3>Mappings for collections ({@code List<}<em>E</em>{@code >} etc)</h3>

    <p>A {@code List<}<em>E</em>{@code >} or {@code
      Set<}<em>E</em>{@code >}, such as {@code List<String>} or {@code
        Set<ObjectName>}, is mapped in the same way as an array of the
          same element type, such as {@code String[]} or {@code
          ObjectName[]}.</p>

    <p>A {@code SortedSet<}<em>E</em>{@code >} is also mapped in the
      same way as an <em>E</em>{@code []}, but it is only convertible if
      <em>E</em> is a class or interface that implements {@link
      java.lang.Comparable}.  Thus, a {@code SortedSet<String>} or
        {@code SortedSet<Integer>} is convertible, but a {@code
          SortedSet<int[]>} or {@code SortedSet<List<String>>} is not.  The
                conversion of a {@code SortedSet} instance will fail with an
                {@code IllegalArgumentException} if it has a
                non-null {@link java.util.SortedSet#comparator()
                comparator()}.</p>

    <p>A {@code List<}<em>E</em>{@code >} is reconstructed as a
      {@code java.util.ArrayList<}<em>E</em>{@code >};
      a {@code Set<}<em>E</em>{@code >} as a
      {@code java.util.HashSet<}<em>E</em>{@code >};
      a {@code SortedSet<}<em>E</em>{@code >} as a
      {@code java.util.TreeSet<}<em>E</em>{@code >}.</p>


    <h3>Mappings for maps ({@code Map<}<em>K</em>,<em>V</em>{@code >} etc)</h3>

    <p>A {@code Map<}<em>K</em>,<em>V</em>{@code >} or {@code
      SortedMap<}<em>K</em>,<em>V</em>{@code >}, for example {@code
      Map<String,ObjectName>}, has Open Type {@link TabularType} and is mapped
        to a {@link TabularData}.
        The {@code TabularType} has two items called {@code key} and
        {@code value}.  The Open Type of {@code key} is
        <em>opentype(K)</em>, and the Open Type of {@code value} is
        <em>opentype(V)</em>.  The index of the {@code TabularType} is the
        single item {@code key}.</p>

    <p>For example, the {@code TabularType} for a {@code
      Map<String,ObjectName>} might be constructed with code like
        this:</p>

    <pre>
String typeName =
    "java.util.Map&lt;java.lang.String, javax.management.ObjectName&gt;";
String[] keyValue =
    new String[] {"key", "value"};
OpenType[] openTypes =
    new OpenType[] {SimpleType.STRING, SimpleType.OBJECTNAME};
CompositeType rowType =
    new CompositeType(typeName, typeName, keyValue, keyValue, openTypes);
TabularType tabularType =
    new TabularType(typeName, typeName, rowType, new String[] {"key"});
    </pre>

    <p>The {@code typeName} here is determined by the <a href="#type-names">
      type name rules</a> detailed below.

    <p>A {@code SortedMap<}<em>K</em>,<em>V</em>{@code >} is mapped in the
      same way, but it is only convertible if
      <em>K</em> is a class or interface that implements {@link
      java.lang.Comparable}.  Thus, a {@code SortedMap<String,int[]>}
        is convertible, but a
        {@code SortedMap<int[],String>} is not.  The conversion of a
          {@code SortedMap} instance will fail with an {@code
          IllegalArgumentException} if it has a non-null {@link
          java.util.SortedMap#comparator() comparator()}.</p>

    <p>A {@code Map<}<em>K</em>,<em>V</em>{@code >} is reconstructed as
      a {@code java.util.HashMap<}<em>K</em>,<em>V</em>{@code >};
      a {@code SortedMap<}<em>K</em>,<em>V</em>{@code >} as
      a {@code java.util.TreeMap<}<em>K</em>,<em>V</em>{@code >}.</p>

    <p>{@code TabularData} is an interface.  The concrete class that is
      used to represent a {@code Map<}<em>K</em>,<em>V</em>{@code >} as
      Open Data is {@link TabularDataSupport},
      or another class implementing {@code
      TabularData} that serializes as {@code TabularDataSupport}.</p>


    <h3 id="records">Mappings for Records</h3>

    <p>A {@linkplain Record record} class <em>J</em> can be converted
      to a {@link CompositeType} if and only if all its
      {@linkplain Class#getRecordComponents() components} are
      convertible to open types. Otherwise, it is not convertible.
      A record that has no components is not convertible.</p>

    <h4 id="record-type-map">Mapping a record class to
      {@code CompositeType}</h4>

    <p>A record whose components are all convertible to open
      types, is itself convertible to a {@link CompositeType}.
      The record class is converted to a {@code CompositeType}
      as follows.</p>

    <ul>
      <li>The type name of the {@code CompositeType} is the name
        of the record class.</li>

      <li>The record getters are the accessors for the
        {@linkplain RecordComponent record components}.</li>

      <li>For each record component of type <em>T</em>, the item in
        the {@code CompositeType} has the same name as the record
        component and its type is <em>opentype(T)</em>, as
        defined by the <a href="#mapping-rules">type mapping rules</a>
        above.</li>
    </ul>

    <h4 id="record-data-map">Mapping an instance of a record class to
      {@code CompositeData}</h4>

    <p>The mapping from an instance of a record class to a
      {@link CompositeData} corresponding to the {@code CompositeType}
      is the same as specified for
      <a href="#composite-data-map">other types</a>.</p>

    <h4 id="reconstructing-record">Reconstructing an instance of a record class
      from a {@code CompositeData}</h4>

    <p>A record is reconstructed using its canonical constructor.
      The canonical constructor doesn't require the presence of
      {@link ConstructorParameters &#64;javax.management.ConstructorParameters}
      or {@code @java.beans.ConstructorProperties} annotations. If these
      annotations are present on the canonical constructor they
      will be ignored.</p>

    <p>How an instance of a record class <em>J</em> is reconstructed
      from a {@link CompositeData} is detailed in
      <a href="#reconstructing">Reconstructing an instance
      of Java type or record class <em>J</em> from a {@code CompositeData}</a>
      below.</p>

    <h3 id="mxbean-map">Mappings for MXBean interfaces</h3>

    <p>An MXBean interface, or a type referenced within an MXBean
      interface, can reference another MXBean interface, <em>J</em>.
      Then <em>opentype(J)</em> is {@code SimpleType.OBJECTNAME} and
      <em>opendata(J)</em> is {@code ObjectName}.</p>

    <p>For example, suppose you have two MXBean interfaces like this:</p>

    <pre>
public interface ProductMXBean {
    public ModuleMXBean[] getModules();
}

public interface ModuleMXBean {
    public ProductMXBean getProduct();
}
    </pre>

    <p>The object implementing the {@code ModuleMXBean} interface
      returns from its {@code getProduct} method an object
      implementing the {@code ProductMXBean} interface.  The
      {@code ModuleMXBean} object and the returned {@code
      ProductMXBean} objects must both be registered as MXBeans in the
      same MBean Server.</p>

    <p>The method {@code ModuleMXBean.getProduct()} defines an
      attribute called {@code Product}.  The Open Type for this
      attribute is {@code SimpleType.OBJECTNAME}, and the corresponding
      {@code ObjectName} value will be the name under which the
      referenced {@code ProductMXBean} is registered in the MBean
      Server.</p>

    <p>If you make an MXBean proxy for a {@code ModuleMXBean} and
      call its {@code getProduct()} method, the proxy will map the
      {@code ObjectName} back into a {@code ProductMXBean} by making
      another MXBean proxy.  More formally, when a proxy made with
      {@link JMX#newMXBeanProxy(MBeanServerConnection, ObjectName,
       Class)
      JMX.newMXBeanProxy(mbeanServerConnection, objectNameX,
      interfaceX)} needs to map {@code objectNameY} back into {@code
      interfaceY}, another MXBean interface, it does so with {@code
      JMX.newMXBeanProxy(mbeanServerConnection, objectNameY,
      interfaceY)}.  The implementation may return a proxy that was
      previously created by a call to {@code JMX.newMXBeanProxy}
      with the same parameters, or it may create a new proxy.</p>

    <p>The reverse mapping is illustrated by the following change to the
      {@code ModuleMXBean} interface:</p>

    <pre>
public interface ModuleMXBean {
    public ProductMXBean getProduct();
    public void setProduct(ProductMXBean c);
}
    </pre>

    <p>The presence of the {@code setProduct} method now means that the
      {@code Product} attribute is read/write.  As before, the value
      of this attribute is an {@code ObjectName}.  When the attribute is
      set, the {@code ObjectName} must be converted into the
      {@code ProductMXBean} object that the {@code setProduct} method
      expects.  This object will be an MXBean proxy for the given
      {@code ObjectName} in the same MBean Server.</p>

    <p>If you make an MXBean proxy for a {@code ModuleMXBean} and
      call its {@code setProduct} method, the proxy will map its
      {@code ProductMXBean} argument back into an {@code ObjectName}.
      This will only work if the argument is in fact another proxy,
      for a {@code ProductMXBean} in the same {@code
      MBeanServerConnection}.  The proxy can have been returned from
      another proxy (like {@code ModuleMXBean.getProduct()} which
      returns a proxy for a {@code ProductMXBean}); or it can have
      been created by {@link
      JMX#newMXBeanProxy(MBeanServerConnection, ObjectName, Class)
      JMX.newMXBeanProxy}; or it can have been created using {@link
      java.lang.reflect.Proxy Proxy} with an invocation handler that
      is {@link MBeanServerInvocationHandler} or a subclass.</p>

    <p>If the same MXBean were registered under two different
      {@code ObjectName}s, a reference to that MXBean from another
      MXBean would be ambiguous.  Therefore, if an MXBean object is
      already registered in an MBean Server and an attempt is made to
      register it in the same MBean Server under another name, the
      result is an {@link InstanceAlreadyExistsException}.  Registering
      the same MBean object under more than one name is discouraged in
      general, notably because it does not work well for MBeans that are
      {@link NotificationBroadcaster}s.</p>

    <h3 id="composite-map">Mappings for other types</h3>

    <p>Given a Java class or interface <em>J</em> that does not match the other
      rules in the table above, the MXBean framework will attempt to map
      it to a {@link CompositeType} as follows.  The type name of this
      {@code CompositeType} is determined by the <a href="#type-names">
      type name rules</a> below.</p>

    <h4 id="composite-type-map">Mapping a Java type <em>J</em>
      to {@link CompositeType}</h4>

    <p>The class is examined for getters using the conventions
      <a href="#naming-conv">above</a>.  (Getters must be public
      instance methods.)  If there are no getters, or if
      any getter has a type that is not convertible, then <em>J</em> is
      not convertible.</p>

    <p>If there is at least one getter and every getter has a
      convertible type, then <em>opentype(J)</em> is a {@code
      CompositeType} with one item for every getter.  If the getter is

    <blockquote>
      <code><em>T</em> get<em>Name</em>()</code>
    </blockquote>

    then the item in the {@code CompositeType} is called {@code name}
    and has type <em>opentype(T)</em>.  For example, if the item is

    <blockquote>
      <code>String getOwner()</code>
    </blockquote>

    then the item is called {@code owner} and has Open Type {@code
    SimpleType.STRING}.  If the getter is

    <blockquote>
      <code>boolean is<em>Name</em>()</code>
    </blockquote>

    then the item in the {@code CompositeType} is called {@code name}
    and has type {@code SimpleType.BOOLEAN}.

    <p>Notice that the first character (or code point) is converted to
      lower case.  This follows the Java Beans convention, which for
      historical reasons is different from the Standard MBean
      convention.  In a Standard MBean or MXBean interface, a method
      {@code getOwner} defines an attribute called {@code Owner}, while
      in a Java Bean or mapped {@code CompositeType}, a method {@code
      getOwner} defines a property or item called {@code owner}.</p>

    <p>If two methods produce the same item name (for example, {@code
      getOwner} and {@code isOwner}, or {@code getOwner} and {@code
      getowner}) then the type is not convertible.</p>

    <h4 id="composite-data-map" >Mapping from an instance of Java
      type or record class <em>J</em> to {@code CompositeData}</h4>

    <p>When the Open Type is {@code CompositeType}, the corresponding
      mapped Java type (<em>opendata(J)</em>) is {@link
      CompositeData}.  The mapping from an instance of <em>J</em> to a
      {@code CompositeData} corresponding to the {@code CompositeType}
      just described is done as follows.  First, if <em>J</em>
      implements the interface {@link CompositeDataView}, then that
      interface's {@link CompositeDataView#toCompositeData
      toCompositeData} method is called to do the conversion.
      Otherwise, the {@code CompositeData} is constructed by calling
      the getter for each item and converting it to the corresponding
      Open Data type.  Thus, a getter such as</p>

    <blockquote>
      {@code List<String> getNames()} (or {@code List<String> names()} for a record)
    </blockquote>

    <p>will have been mapped to an item with name "{@code names}" and
      Open Type {@code ArrayType(1, SimpleType.STRING)}.  The conversion
      to {@code CompositeData} will call {@code getNames()} and convert
      the resultant {@code List<String>} into a {@code String[]} for the
        item "{@code names}".</p>

    <p>{@code CompositeData} is an interface.  The concrete class that is
      used to represent a type as Open Data is {@link
      CompositeDataSupport}, or another class implementing {@code
      CompositeData} that serializes as {@code
      CompositeDataSupport}.</p>


    <h4 id="reconstructing">Reconstructing an instance of Java type
      or record class <em>J</em> from a {@code CompositeData}</h4>

    <p>If <em>opendata(J)</em> is {@code CompositeData} for a Java type
      <em>J</em>, then either an instance of <em>J</em> can be
      reconstructed from a {@code CompositeData}, or <em>J</em> is not
      reconstructible.  If any item in the {@code CompositeData} is not
      reconstructible, then <em>J</em> is not reconstructible either.</p>

    <p>For any given <em>J</em>, the following rules are consulted to
      determine how to reconstruct instances of <em>J</em> from
      {@code CompositeData}.  The first applicable rule in the list is
      the one that will be used.</p>

    <ol>

      <li><p>If <em>J</em> has a method<br>
        {@code public static }<em>J </em>{@code from(CompositeData cd)}<br>
        then that method is called to reconstruct an instance of
        <em>J</em>.</p></li>

      <li><p>Otherwise, if <em>J</em> is a {@link Record} class,
        and the record canonical constructor is applicable,
        an instance of <em>J</em> is reconstructed by calling
        the record canonical constructor.
        The canonical constructor, if applicable, is called
        with the appropriate reconstructed items from the
        {@code CompositeData}. The canonical constructor
        is <em>applicable</em> if all the properties named
        by the record components are present in the
        {@code CompositeData}.</p></li>

      <li><p>Otherwise, if <em>J</em> has at least one public
        constructor with either {@link javax.management.ConstructorParameters
        &#64;javax.management.ConstructorParameters} or
        {@code @java.beans.ConstructoProperties} annotation, then one of those
        constructors (not necessarily always the same one) will be called to
        reconstruct an instance of <em>J</em>.
        If a constructor is annotated with both
        {@code @javax.management.ConstructorParameters} and
        {@code @java.beans.ConstructorProperties},
        {@code @javax.management.ConstructorParameters} will be used and
        {@code @java.beans.ConstructorProperties} will be ignored.
        Every such annotation must list as many strings as the
        constructor has parameters; each string must name a property
        corresponding to a getter of <em>J</em>; and the type of this
        getter must be the same as the corresponding constructor
        parameter.  It is not an error for there to be getters that
        are not mentioned in the {@code @ConstructorParameters} or
        {@code @ConstructorProperties} annotations (these may correspond to
        information that is not needed to reconstruct the object).</p>

        <p>An instance of <em>J</em> is reconstructed by calling a
        constructor with the appropriate reconstructed items from the
        {@code CompositeData}.  The constructor to be called will be
        determined at runtime based on the items actually present in
        the {@code CompositeData}, given that this {@code
        CompositeData} might come from an earlier version of
        <em>J</em> where not all the items were present.  A
        constructor is <em>applicable</em> if all the properties named
        in its {@code @ConstructorParameters} or {@code @ConstructorProperties}
        annotation are present as items in the {@code CompositeData}.
        If no constructor is applicable, then the attempt to reconstruct
        <em>J</em> fails.</p>

        <p>For any possible combination of properties, it must be the
        case that either (a) there are no applicable constructors, or
        (b) there is exactly one applicable constructor, or (c) one of
        the applicable constructors names a proper superset of the
        properties named by each other applicable constructor.  (In
        other words, there should never be ambiguity over which
        constructor to choose.)  If this condition is not true, then
        <em>J</em> is not reconstructible.</p></li>

      <li><p>Otherwise, if <em>J</em> has a public no-arg constructor, and
        for every getter in <em>J</em> with type
        <em>T</em> and name <em>N</em> there is a corresponding setter
        with the same name and type, then an instance of <em>J</em> is
        constructed with the no-arg constructor and the setters are
        called with the reconstructed items from the {@code CompositeData}
        to restore the values.  For example, if there is a method<br>
        {@code public List<String> getNames()}<br>
          then there must also be a method<br>
          {@code public void setNames(List<String> names)}<br>
            for this rule to apply.</p>

        <p>If the {@code CompositeData} came from an earlier version of
        <em>J</em>, some items might not be present.  In this case,
        the corresponding setters will not be called.</p></li>

      <li><p>Otherwise, if <em>J</em> is an interface that has no methods
        other than getters, an instance of <em>J</em> is constructed
        using a {@link java.lang.reflect.Proxy} with a {@link
        CompositeDataInvocationHandler} backed by the {@code
        CompositeData} being converted.</p></li>

      <li><p>Otherwise, <em>J</em> is not reconstructible.</p></li>
    </ol>

    <p>Rule 2 is not applicable when {@code java.beans.ConstructorProperties}
    is not visible (e.g. when the java.desktop module is not readable or when
    the runtime image does not contain the java.desktop module). When
    targeting a runtime that does not include the {@code java.beans} package,
    and where there is a mismatch between the compile-time and runtime
    environment whereby <em>J</em> is compiled with a public constructor
    and the {@code ConstructorProperties} annotation, then <em>J</em> is
    not reconstructible unless another rule applies.</p>

    <p>Here are examples showing different ways to code a type {@code
      NamedNumber} that consists of an {@code int} and a {@code
      String}.  In each case, the {@code CompositeType} looks like this:</p>

    <blockquote>
      <pre>
{@link CompositeType}(
    "NamedNumber",                      // typeName
    "NamedNumber",                      // description
    new String[] {"number", "name"},    // itemNames
    new String[] {"number", "name"},    // itemDescriptions
    new OpenType[] {SimpleType.INTEGER,
                    SimpleType.STRING}  // itemTypes
);
      </pre>
    </blockquote>

    <ol>
      <li>Static {@code from} method:

        <blockquote>
          <pre>
public class NamedNumber {
    public int getNumber() {return number;}
    public String getName() {return name;}
    private NamedNumber(int number, String name) {
        this.number = number;
        this.name = name;
    }
    <b>public static NamedNumber from(CompositeData cd)</b> {
        return new NamedNumber((Integer) cd.get("number"),
                               (String) cd.get("name"));
    }
    private final int number;
    private final String name;
}
          </pre>
        </blockquote>
      </li>

      <li>Record:

        <blockquote>
          <pre>
 public record NamedNumber(int number, String name) {}
          </pre>
        </blockquote>
      </li>

      <li>Public constructor with <code>&#64;ConstructorParameters</code> annotation:

        <blockquote>
          <pre>
public class NamedNumber {
    public int getNumber() {return number;}
    public String getName() {return name;}
    <b>&#64;ConstructorParameters({"number", "name"})
    public NamedNumber(int number, String name)</b> {
        this.number = number;
        this.name = name;
    }
    private final int number;
    private final String name;
}
          </pre>
        </blockquote>
      </li>

      <li>Setter for every getter:

        <blockquote>
          <pre>
public class NamedNumber {
    public int getNumber() {return number;}
    public void <b>setNumber</b>(int number) {this.number = number;}
    public String getName() {return name;}
    public void <b>setName</b>(String name) {this.name = name;}
    <b>public NamedNumber()</b> {}
    private int number;
    private String name;
}
          </pre>
        </blockquote>
      </li>

      <li>Interface with only getters:

        <blockquote>
          <pre>
public interface NamedNumber {
    public int getNumber();
    public String getName();
}
          </pre>
        </blockquote>
      </li>
    </ol>

    <p>It is usually better for classes that simply represent a
      collection of data to be <em>immutable</em>.  An instance of an
      immutable class cannot be changed after it has been constructed.
      Notice that {@code CompositeData} itself is immutable.
      Immutability has many advantages, notably with regard to
      thread-safety and security.  So the approach using setters should
      generally be avoided if possible.</p>


    <h3>Recursive types</h3>

    <p>Recursive (self-referential) types cannot be used in MXBean
      interfaces.  This is a consequence of the immutability of {@link
      CompositeType}.  For example, the following type could not be the
      type of an attribute, because it refers to itself:</p>

    <pre>
public interface <b>Node</b> {
    public String getName();
    public int getPriority();
    public <b>Node</b> getNext();
}
</pre>

    <p>It is always possible to rewrite recursive types like this so
      they are no longer recursive.  Doing so may require introducing
      new types.  For example:</p>

    <pre>
public interface <b>NodeList</b> {
    public List&lt;Node&gt; getNodes();
}

public interface Node {
    public String getName();
    public int getPriority();
}
</pre>

    <h3>MBeanInfo contents for an MXBean</h3>

    <p>An MXBean is a type of Open MBean.  However, for compatibility
      reasons, its {@link MBeanInfo} is not an {@link OpenMBeanInfo}.
      In particular, when the type of an attribute, parameter, or
      operation return value is a primitive type such as {@code int},
      or is {@code void} (for a return type), then the attribute,
      parameter, or operation will be represented respectively by an
      {@link MBeanAttributeInfo}, {@link MBeanParameterInfo}, or
      {@link MBeanOperationInfo} whose {@code getType()} or {@code
      getReturnType()} returns the primitive name ("{@code int}" etc).
      This is so even though the mapping rules above specify that the
      <em>opendata</em> mapping is the wrapped type ({@code Integer}
      etc).</p>

    <p>The array of public constructors returned by {@link
      MBeanInfo#getConstructors()} for an MXBean that is directly
      registered in the MBean Server will contain all of the public
      constructors of that MXBean.  If the class of the MXBean is not
      public then its constructors are not considered public either.
      The list returned for an MXBean that is constructed using the
      {@link StandardMBean} class is derived in the same way as for
      Standard MBeans.  Regardless of how the MXBean was constructed,
      its constructor parameters are not subject to MXBean mapping
      rules and do not have a corresponding {@code OpenType}.</p>

    <p>The array of notification types returned by {@link
      MBeanInfo#getNotifications()} for an MXBean that is directly
      registered in the MBean Server will be empty if the MXBean does
      not implement the {@link NotificationBroadcaster} interface.
      Otherwise, it will be the result of calling {@link
      NotificationBroadcaster#getNotificationInfo()} at the time the MXBean
      was registered.  Even if the result of this method changes
      subsequently, the result of {@code MBeanInfo.getNotifications()}
      will not.  The list returned for an MXBean that is constructed
      using the {@link StandardMBean} or {@link StandardEmitterMBean}
      class is derived in the same way as for Standard MBeans.</p>

    <p>The {@link Descriptor} for all of the
      {@code MBeanAttributeInfo}, {@code MBeanParameterInfo}, and
      {@code MBeanOperationInfo} objects contained in the {@code MBeanInfo}
      will have a field {@code openType} whose value is the {@link OpenType}
      specified by the mapping rules above.  So even when {@code getType()}
      is "{@code int}", {@code getDescriptor().getField("openType")} will
      be {@link SimpleType#INTEGER}.</p>

    <p>The {@code Descriptor} for each of these objects will also have a
      field {@code originalType} that is a string representing the Java type
      that appeared in the MXBean interface.  The format of this string
      is described in the section <a href="#type-names">Type Names</a>
      below.</p>

    <p>The {@code Descriptor} for the {@code MBeanInfo} will have a field
      {@code mxbean} whose value is the string "{@code true}".</p>


    <h3 id="type-names">Type Names</h3>

    <p>Sometimes the unmapped type <em>T</em> of a method parameter or
    return value in an MXBean must be represented as a string.  If
    <em>T</em> is a non-generic type, this string is the value
    returned by {@link Class#getName()}.  Otherwise it is the value of
    <em>genericstring(T)</em>, defined as follows:

    <ul>

      <li>If <em>T</em> is a non-generic non-array type,
      <em>genericstring(T)</em> is the value returned by {@link
      Class#getName()}, for example {@code "int"} or {@code
      "java.lang.String"}.

      <li>If <em>T</em> is an array <em>E[]</em>,
      <em>genericstring(T)</em> is <em>genericstring(E)</em> followed
      by {@code "[]"}.  For example, <em>genericstring({@code int[]})</em>
      is {@code "int[]"}, and <em>genericstring({@code
      List<String>[][]})</em> is {@code
      "java.util.List<java.lang.String>[][]"}.

    <li>Otherwise, <em>T</em> is a parameterized type such as {@code
    List<String>} and <em>genericstring(T)</em> consists of the
    following: the fully-qualified name of the parameterized type as
    returned by {@code Class.getName()}; a left angle bracket ({@code
    "<"}); <em>genericstring(A)</em> where <em>A</em> is the first
    type parameter; if there is a second type parameter <em>B</em>
    then {@code ", "} (a comma and a single space) followed by
    <em>genericstring(B)</em>; a right angle bracket ({@code ">"}).

    </ul>

    <p>Note that if a method returns {@code int[]}, this will be
      represented by the string {@code "[I"} returned by {@code
      Class.getName()}, but if a method returns {@code List<int[]>},
      this will be represented by the string {@code
      "java.util.List<int[]>"}.

    <h3>Exceptions</h3>

    <p>A problem with mapping <em>from</em> Java types <em>to</em>
      Open types is signaled with an {@link OpenDataException}.  This
      can happen when an MXBean interface is being analyzed, for
      example if it references a type like {@link java.util.Random
      java.util.Random} that has no getters.  Or it can happen when an
      instance is being converted (a return value from a method in an
      MXBean or a parameter to a method in an MXBean proxy), for
      example when converting from {@code SortedSet<String>} to {@code
      String[]} if the {@code SortedSet} has a non-null {@code
      Comparator}.</p>

    <p>A problem with mapping <em>to</em> Java types <em>from</em>
      Open types is signaled with an {@link InvalidObjectException}.
      This can happen when an MXBean interface is being analyzed, for
      example if it references a type that is not
      <em>reconstructible</em> according to the rules above, in a
      context where a reconstructible type is required.  Or it can
      happen when an instance is being converted (a parameter to a
      method in an MXBean or a return value from a method in an MXBean
      proxy), for example from a String to an Enum if there is no Enum
      constant with that name.</p>

    <p>Depending on the context, the {@code OpenDataException} or
      {@code InvalidObjectException} may be wrapped in another
      exception such as {@link RuntimeMBeanException} or {@link
      UndeclaredThrowableException}.  For every thrown exception,
      the condition <em>C</em> will be true: "<em>e</em> is {@code
      OpenDataException} or {@code InvalidObjectException} (as
      appropriate), or <em>C</em> is true of <em>e</em>.{@link
      Throwable#getCause() getCause()}".</p>

   @since 1.6
*/

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MXBean {
    /**
       True if the annotated interface is an MXBean interface.
       @return true if the annotated interface is an MXBean interface.
    */
    boolean value() default true;
}
