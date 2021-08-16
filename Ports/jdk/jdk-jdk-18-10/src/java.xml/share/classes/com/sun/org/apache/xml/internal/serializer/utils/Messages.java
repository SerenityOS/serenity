/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer.utils;

import java.util.Locale;
import java.util.ResourceBundle;
import jdk.xml.internal.SecuritySupport;

/**
 * A utility class for issuing error messages.
 *
 * A user of this class normally would create a singleton
 * instance of this class, passing the name
 * of the message class on the constructor. For example:
 * <CODE>
 * static Messages x = new Messages("org.package.MyMessages");
 * </CODE>
 * Later the message is typically generated this way if there are no
 * substitution arguments:
 * <CODE>
 * String msg = x.createMessage(org.package.MyMessages.KEY_ONE, null);
 * </CODE>
 * If there are arguments substitutions then something like this:
 * <CODE>
 * String filename = ...;
 * String directory = ...;
 * String msg = x.createMessage(org.package.MyMessages.KEY_TWO,
 *   new Object[] {filename, directory) );
 * </CODE>
 *
 * The constructor of an instance of this class must be given
 * the class name of a class that extends java.util.ListResourceBundle
 * ("org.package.MyMessages" in the example above).
 * The name should not have any language suffix
 * which will be added automatically by this utility class.
 *
 * The message class ("org.package.MyMessages")
 * must define the abstract method getContents() that is
 * declared in its base class, for example:
 * <CODE>
 * public Object[][] getContents() {return contents;}
 * </CODE>
 *
 * It is suggested that the message class expose its
 * message keys like this:
 * <CODE>
 *   public static final String KEY_ONE = "KEY1";
 *   public static final String KEY_TWO = "KEY2";
 *   . . .
 * </CODE>
 * and used through their names (KEY_ONE ...) rather than
 * their values ("KEY1" ...).
 *
 * The field contents (returned by getContents()
 * should be initialized something like this:
 * <CODE>
 * public static final Object[][] contents = {
 * { KEY_ONE, "Something has gone wrong!" },
 * { KEY_TWO, "The file ''{0}'' does not exist in directory ''{1}''." },
 * . . .
 * { KEY_N, "Message N" }  }
 * </CODE>
 *
 * Where that section of code with the KEY to Message mappings
 * (where the message classes 'contents' field is initialized)
 * can have the Message strings translated in an alternate language
 * in a errorResourceClass with a language suffix.
 *
 *
 * This class is not a public API, it is only public because it is
 * used in com.sun.org.apache.xml.internal.serializer.
 *
 *  @xsl.usage internal
 * @LastModified: Sep 2017
 */
public final class Messages
{
    /** The local object to use.  */
    private final Locale m_locale = Locale.getDefault();

    /** The language specific resource object for messages.  */
    private ResourceBundle m_resourceBundle;

    /** The class name of the error message string table with no language suffix. */
    private String m_resourceBundleName;



    /**
     * Constructor.
     * @param resourceBundle the class name of the ListResourceBundle
     * that the instance of this class is associated with and will use when
     * creating messages.
     * The class name is without a language suffix. If the value passed
     * is null then loadResourceBundle(errorResourceClass) needs to be called
     * explicitly before any messages are created.
     *
     * @xsl.usage internal
     */
    Messages(String resourceBundle)
    {

        m_resourceBundleName = resourceBundle;
    }


    /**
     * Get the Locale object that is being used.
     *
     * @return non-null reference to Locale object.
     * @xsl.usage internal
     */
    private Locale getLocale()
    {
        return m_locale;
    }

    /**
     * Creates a message from the specified key and replacement
     * arguments, localized to the given locale.
     *
     * @param msgKey  The key for the message text.
     * @param args    The arguments to be used as replacement text
     * in the message created.
     *
     * @return The formatted message string.
     * @xsl.usage internal
     */
    public final String createMessage(String msgKey, Object args[])
    {
        if (m_resourceBundle == null)
            m_resourceBundle = SecuritySupport.getResourceBundle(m_resourceBundleName);

        if (m_resourceBundle != null)
        {
            return createMsg(m_resourceBundle, msgKey, args);
        }
        else
            return "Could not load the resource bundles: "+ m_resourceBundleName;
    }

    /**
     * Creates a message from the specified key and replacement
     * arguments, localized to the given locale.
     *
     * @param errorCode The key for the message text.
     *
     * @param fResourceBundle The resource bundle to use.
     * @param msgKey  The message key to use.
     * @param args      The arguments to be used as replacement text
     *                  in the message created.
     *
     * @return The formatted message string.
     * @xsl.usage internal
     */
    private final String createMsg(ResourceBundle fResourceBundle, String msgKey,
            Object args[]) //throws Exception
    {

        String fmsg = null;
        boolean throwex = false;
        String msg = null;

        if (msgKey != null)
            msg = fResourceBundle.getString(msgKey);
        else
            msgKey = "";

        if (msg == null)
        {
            throwex = true;
            /* The message is not in the bundle . . . this is bad,
             * so try to get the message that the message is not in the bundle
             */
            try
            {

                msg =
                    java.text.MessageFormat.format(
                        MsgKey.BAD_MSGKEY,
                        new Object[] { msgKey, m_resourceBundleName });
            }
            catch (Exception e)
            {
                /* even the message that the message is not in the bundle is
                 * not there ... this is really bad
                 */
                msg =
                    "The message key '"
                        + msgKey
                        + "' is not in the message class '"
                        + m_resourceBundleName+"'";
            }
        }
        else if (args != null)
        {
            try
            {
                // Do this to keep format from crying.
                // This is better than making a bunch of conditional
                // code all over the place.
                int n = args.length;

                for (int i = 0; i < n; i++)
                {
                    if (null == args[i])
                        args[i] = "";
                }

                fmsg = java.text.MessageFormat.format(msg, args);
                // if we get past the line above we have create the message ... hurray!
            }
            catch (Exception e)
            {
                throwex = true;
                try
                {
                    // Get the message that the format failed.
                    fmsg =
                        java.text.MessageFormat.format(
                            MsgKey.BAD_MSGFORMAT,
                            new Object[] { msgKey, m_resourceBundleName });
                    fmsg += " " + msg;
                }
                catch (Exception formatfailed)
                {
                    // We couldn't even get the message that the format of
                    // the message failed ... so fall back to English.
                    fmsg =
                        "The format of message '"
                            + msgKey
                            + "' in message class '"
                            + m_resourceBundleName
                            + "' failed.";
                }
            }
        }
        else
            fmsg = msg;

        if (throwex)
        {
            throw new RuntimeException(fmsg);
        }

        return fmsg;
    }

}
