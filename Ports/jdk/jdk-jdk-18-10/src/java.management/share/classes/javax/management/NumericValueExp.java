/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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


import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

import java.security.AccessController;

/**
 * This class represents numbers that are arguments to relational constraints.
 * A NumericValueExp may be used anywhere a ValueExp is required.
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>-4679739485102359104L</code>.
 *
 * @serial include
 *
 * @since 1.5
 */
@SuppressWarnings("serial")  // serialVersionUID not constant
class NumericValueExp extends QueryEval implements ValueExp {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -6227876276058904000L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = -4679739485102359104L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
        new ObjectStreamField("longVal", Long.TYPE),
        new ObjectStreamField("doubleVal", Double.TYPE),
        new ObjectStreamField("valIsLong", Boolean.TYPE)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
        new ObjectStreamField("val", Number.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;

    /**
     * @serialField val Number The numeric value
     *
     * <p>The <b>serialVersionUID</b> of this class is <code>-4679739485102359104L</code>.
     */
    private static final ObjectStreamField[] serialPersistentFields;
    private Number val = 0.0;

    private static boolean compat = false;
    static {
        try {
            GetPropertyAction act = new GetPropertyAction("jmx.serial.form");
            @SuppressWarnings("removal")
            String form = AccessController.doPrivileged(act);
            compat = (form != null && form.equals("1.0"));
        } catch (Exception e) {
            // OK: exception means no compat with 1.0, too bad
        }
        if (compat) {
            serialPersistentFields = oldSerialPersistentFields;
            serialVersionUID = oldSerialVersionUID;
        } else {
            serialPersistentFields = newSerialPersistentFields;
            serialVersionUID = newSerialVersionUID;
        }
    }
    //
    // END Serialization compatibility stuff


    /**
     * Basic constructor.
     */
    public NumericValueExp() {
    }

    /** Creates a new NumericValue representing the numeric literal @{code val}.*/
    NumericValueExp(Number val)
    {
      this.val = val;
    }

    /**
     * Returns a double numeric value
     */
    public double doubleValue()  {
      if (val instanceof Long || val instanceof Integer)
      {
        return (double)(val.longValue());
      }
      return val.doubleValue();
    }

    /**
     * Returns a long numeric value
     */
    public long longValue()  {
      if (val instanceof Long || val instanceof Integer)
      {
        return val.longValue();
      }
      return (long)(val.doubleValue());
    }

    /**
     * Returns true is if the numeric value is a long, false otherwise.
     */
    public boolean isLong()  {
        return (val instanceof Long || val instanceof Integer);
    }

    /**
     * Returns the string representing the object
     */
    public String toString()  {
      if (val == null)
        return "null";
      if (val instanceof Long || val instanceof Integer)
      {
        return Long.toString(val.longValue());
      }
      double d = val.doubleValue();
      if (Double.isInfinite(d))
          return (d > 0) ? "(1.0 / 0.0)" : "(-1.0 / 0.0)";
      if (Double.isNaN(d))
          return "(0.0 / 0.0)";
      return Double.toString(d);
    }

    /**
     * Applies the ValueExp on a MBean.
     *
     * @param name The name of the MBean on which the ValueExp will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException
     * @exception InvalidApplicationException
     */
    public ValueExp apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException {
        return this;
    }

    /**
     * Deserializes a {@link NumericValueExp} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      if (compat)
      {
        // Read an object serialized in the old serial form
        //
        double doubleVal;
        long longVal;
        boolean isLong;
        ObjectInputStream.GetField fields = in.readFields();
        doubleVal = fields.get("doubleVal", (double)0);
        if (fields.defaulted("doubleVal"))
        {
          throw new NullPointerException("doubleVal");
        }
        longVal = fields.get("longVal", (long)0);
        if (fields.defaulted("longVal"))
        {
          throw new NullPointerException("longVal");
        }
        isLong = fields.get("valIsLong", false);
        if (fields.defaulted("valIsLong"))
        {
          throw new NullPointerException("valIsLong");
        }
        if (isLong)
        {
          this.val = longVal;
        }
        else
        {
          this.val = doubleVal;
        }
      }
      else
      {
        // Read an object serialized in the new serial form
        //
        in.defaultReadObject();
      }
    }


    /**
     * Serializes a {@link NumericValueExp} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("doubleVal", doubleValue());
        fields.put("longVal", longValue());
        fields.put("valIsLong", isLong());
        out.writeFields();
      }
      else
      {
        // Serializes this instance in the new serial form
        //
        out.defaultWriteObject();
      }
    }

    @Deprecated
    public void setMBeanServer(MBeanServer s) {
        super.setMBeanServer(s);
    }

 }
