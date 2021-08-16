/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * A Simple AttachingConnector throwing RuntimeException during instantiating used by
 * nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006 test
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect006.connectors;

import nsk.share.jdi.*;
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.util.*;
import java.util.ArrayList;

public class PlugAttachConnector006_03 extends PlugConnectors implements AttachingConnector {

    static String plugAttachConnectorName = "PlugAttachConnector006_03_Name";
    static String plugAttachConnectorDescription = "PlugAttachConnector006_03_Description";
    static Transport plugAttachConnectorTransport = new PlugConnectorsTransport("PlugAttachConnector006_03_Transport");
    static Map<String, Connector.Argument> plugAttachConnectorDefaultArguments = new HashMap<String, Connector.Argument>();

    static Map<String, Connector.Argument> prepareConnectorDefaultArguments() {
        String plugAttachConnectorStringArgumentKey = "PlugAttachConnector006_03_StringArgument_Key";
        Connector.StringArgument testStringArgument = new TestStringArgument(
            "PlugAttachConnector006_03_StringArgument_Name",
            "PlugAttachConnector006_03_StringArgument_Label",
            "PlugAttachConnector006_03_StringArgument_Description",
            "PlugAttachConnector006_03_StringArgument_Value",
            true  // mustSpecify
            );
        plugAttachConnectorDefaultArguments.put(plugAttachConnectorStringArgumentKey, testStringArgument);

        String plugAttachConnectorIntegerArgumentKey = "PlugAttachConnector006_03_IntegerArgument_Key";
        Connector.IntegerArgument testIntegerArgument = new TestIntegerArgument(
            "PlugAttachConnector006_03_IntegerArgument_Name",
            "PlugAttachConnector006_03_IntegerArgument_Label",
            "PlugAttachConnector006_03_IntegerArgument_Description",
            555555, // IntegerArgument_Value",
            111111, // IntegerArgument_Min",
            999999, // IntegerArgument_Max",
            true    // mustSpecify
            );
        plugAttachConnectorDefaultArguments.put(plugAttachConnectorIntegerArgumentKey, testIntegerArgument);

        String plugAttachConnectorBooleanArgumentKey = "PlugAttachConnector006_03_BooleanArgument_Key";
        Connector.BooleanArgument testBooleanArgument = new TestBooleanArgument(
            "PlugAttachConnector006_03_BooleanArgument_Name",
            "PlugAttachConnector006_03_BooleanArgument_Label",
            "PlugAttachConnector006_03_BooleanArgument_Description",
            true, // BooleanArgument_Value",
            true    // mustSpecify
            );
        plugAttachConnectorDefaultArguments.put(plugAttachConnectorBooleanArgumentKey, testBooleanArgument);

        String plugAttachConnectorSelectedArgumentKey = "PlugAttachConnector006_03_SelectedArgument_Key";
        List<String> selectedArgumentChoices = new ArrayList<String>();
        selectedArgumentChoices.add("PlugAttachConnector006_03_SelectedArgument_Value_0");
        selectedArgumentChoices.add("PlugAttachConnector006_03_SelectedArgument_Value");
        selectedArgumentChoices.add("PlugAttachConnector006_03_SelectedArgument_Value_1");

        Connector.SelectedArgument testSelectedArgument = new TestSelectedArgument(
            "PlugAttachConnector006_03_SelectedArgument_Name",
            "PlugAttachConnector006_03_SelectedArgument_Label",
            "PlugAttachConnector006_03_SelectedArgument_Description",
            "PlugAttachConnector006_03_SelectedArgument_Value",
            selectedArgumentChoices, // List of choices,
            true    // mustSpecify
            );
        plugAttachConnectorDefaultArguments.put(plugAttachConnectorSelectedArgumentKey, testSelectedArgument);

        return plugAttachConnectorDefaultArguments;
    }  // end of prepareConnectorDefaultArguments() method


    public PlugAttachConnector006_03() {

        super(plugAttachConnectorName,
            plugAttachConnectorDescription,
            plugAttachConnectorTransport,
            prepareConnectorDefaultArguments());

        String exceptionMessage =
            "<## PlugAttachConnector006_03: This RuntimeException is thrown intentionally by AttachingConnector "
            + "constructor to check creating of pluggable connectors on base of such AttachingConnector. ##>";

        throw new RuntimeException(exceptionMessage);
    }

} // end of PlugAttachConnector006_03 class
