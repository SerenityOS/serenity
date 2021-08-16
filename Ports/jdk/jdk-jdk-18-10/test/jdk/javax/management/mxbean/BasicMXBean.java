/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Date;
import java.util.Map;

import javax.management.ObjectName;

/**
 * Interface BasicMBean
 * Basic Description
 */
@SqeDescriptorKey("INTERFACE BasicMXBean")
public interface BasicMXBean
{
   /**
    * Get int attribute
    */
    @SqeDescriptorKey("ATTRIBUTE intAtt")
    public int getIntAtt();

   /**
    * Set int attribute
    */
    @SqeDescriptorKey("ATTRIBUTE intAtt")
    public void setIntAtt(int value);

   /**
    * Get Integer attribute
    */
    @SqeDescriptorKey("ATTRIBUTE integerAtt")
    public Integer getIntegerAtt();

   /**
    * Set Integer attribute
    */
    @SqeDescriptorKey("ATTRIBUTE integerAtt")
    public void setIntegerAtt(Integer value);

   /**
    * Get boolean attribute
    */
    @SqeDescriptorKey("ATTRIBUTE boolAtt")
    public boolean getBoolAtt();

   /**
    * Set boolean attribute
    */
    @SqeDescriptorKey("ATTRIBUTE boolAtt")
    public void setBoolAtt(boolean value);

   /**
    * Get Boolean attribute
    */
    @SqeDescriptorKey("ATTRIBUTE booleanAtt")
    public Boolean getBooleanAtt();

   /**
    * Set Boolean attribute
    */
    @SqeDescriptorKey("ATTRIBUTE booleanAtt")
    public void setBooleanAtt(Boolean value);

   /**
    * Get String attribute
    */
    @SqeDescriptorKey("ATTRIBUTE stringAtt")
    public String getStringAtt();

   /**
    * Set String attribute
    */
    @SqeDescriptorKey("ATTRIBUTE stringAtt")
    public void setStringAtt(String value);

   /**
    * Get Date attribute
    */
    @SqeDescriptorKey("ATTRIBUTE dateAtt")
    public Date getDateAtt();

   /**
    * Set Date attribute
    */
    @SqeDescriptorKey("ATTRIBUTE dateAtt")
    public void setDateAtt(Date value);

   /**
    * Get ObjectName attribute
    */
    @SqeDescriptorKey("ATTRIBUTE objectNameAtt")
    public ObjectName getObjectNameAtt();

   /**
    * Set ObjectName attribute
    */
    @SqeDescriptorKey("ATTRIBUTE objectNameAtt")
    public void setObjectNameAtt(ObjectName value);

   /**
    * Get SqeParameter attribute
    */
    @SqeDescriptorKey("ATTRIBUTE sqeParameterAtt")
    public SqeParameter getSqeParameterAtt() throws Exception;

   /**
    * Set SqeParameter attribute
    */
    @SqeDescriptorKey("ATTRIBUTE sqeParameterAtt")
    public void setSqeParameterAtt(SqeParameter value);

   /**
    * Set NumOfNotificationSenders attribute
    */
    @SqeDescriptorKey("ATTRIBUTE NumOfNotificationSenders")
    public void setNumOfNotificationSenders(int value);

   /**
    * Set NumOfNotificationSenderLoops attribute
    */
    @SqeDescriptorKey("ATTRIBUTE NumOfNotificationSenderLoops")
    public void setNumOfNotificationSenderLoops(int value);

   /**
    * do nothing
    *
    */
    @SqeDescriptorKey("OPERATION doNothing")
    public void doNothing();

   /**
    * Do take SqeParameter as a parameter
    */
    @SqeDescriptorKey("OPERATION doWeird")
    public void doWeird(@SqeDescriptorKey("METHOD PARAMETER")SqeParameter param);

    /**
    * throw an Exception
    *
    */
    @SqeDescriptorKey("OPERATION throwException")
    public void throwException() throws Exception;

   /**
    * throw an Error
    *
    */
    @SqeDescriptorKey("OPERATION throwError")
    public void throwError();

   /**
    * reset all attributes
    *
    */
    @SqeDescriptorKey("OPERATION reset")
    public void reset();

   /**
    * returns the weather for the coming days
    *
    * @param verbose <code>boolean</code> verbosity
    * @return <code>ObjectName</code>
    */
    @SqeDescriptorKey("OPERATION getWeather")
    public Weather getWeather(@SqeDescriptorKey("METHOD PARAMETER")boolean verbose)
        throws java.lang.Exception;

    public enum Weather {
        CLOUDY, SUNNY
    }

    @SqeDescriptorKey("ATTRIBUTE notifDescriptorAsMapAtt")
    public Map<String, String> getNotifDescriptorAsMapAtt();

    @SqeDescriptorKey("ATTRIBUTE notifDescriptorAsMapAtt")
    public void setNotifDescriptorAsMapAtt(Map<String, String> value);

    @SqeDescriptorKey("OPERATION sendNotification")
    public void sendNotification(@SqeDescriptorKey("METHOD PARAMETER")String notifType);

    @SqeDescriptorKey("OPERATION sendNotificationWave")
    public void sendNotificationWave(boolean customNotification) throws Exception;
}
