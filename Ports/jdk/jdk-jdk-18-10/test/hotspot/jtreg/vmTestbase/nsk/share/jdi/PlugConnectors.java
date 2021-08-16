/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A Super class for pluggable connectors used by
 * nsk/jdi/PlugConnectors tests
 */

package nsk.share.jdi;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

public class PlugConnectors implements Connector {

    String plugConnectorName = "Undefined_PlugConnector_Name";
    String plugConnectorDescription = "Undefined_PlugConnector_Description";
    Transport plugConnectorTransport = new PlugConnectorsTransport();
    Map<java.lang.String,com.sun.jdi.connect.Connector.Argument> plugConnectorDefaultArguments = new HashMap<java.lang.String,com.sun.jdi.connect.Connector.Argument>();

    /*
     * Simple implementation of Connector.Argument
     */
    public static class TestArgument implements Connector.Argument {
        String argName;
        String argLabel;
        String argDescription;
        String argStringValue;
        boolean argMustSpecify;

        public TestArgument(
                String argName,
                String argLabel,
                String argDescription,
                String argValue,
                boolean argMustSpecify) {

            this.argName = argName;
            this.argLabel = argLabel;
            this.argDescription = argDescription;
            this.argStringValue = argValue;
            this.argMustSpecify = argMustSpecify;
        }

        public String name() {
            return argName;
        }

        public String label() {
            return argLabel;
        }

        public String description() {
            return argDescription;
        }

        public String value() {
            return argStringValue;
        }

        public void setValue(String argValue) {
            this.argStringValue = argValue;
        }

        public boolean isValid(String argValue) {
            if ( argValue != null ) {
                if (argValue.length() > 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean mustSpecify() {
            return argMustSpecify;
        }
    } // end of TestArgument static class

    /*
     * Simple implementation of Connector.StringArgument
     */
    public static class TestStringArgument extends TestArgument implements Connector.StringArgument {

        public TestStringArgument( String argName,
                        String argLabel,
                        String argDescription,
                        String argValue,
                        boolean argMustSpecify) {

            super(argName, argLabel, argDescription, argValue, argMustSpecify);
        }

    } // end of TestStringArgument static class

    /*
     * Simple implementation of Connector.IntegerArgument
     */
    public static class TestIntegerArgument extends TestArgument implements Connector.IntegerArgument {

        int argIntValue;
        int minArgIntValue;
        int maxArgIntValue;

        public TestIntegerArgument(String argName,
                        String argLabel,
                        String argDescription,
                        int argValue,
                        int minArgIntValue,
                        int maxArgIntValue,
                        boolean argMustSpecify) {

            super(argName, argLabel, argDescription, "" + argValue, argMustSpecify);
            this.argIntValue = argValue;
            this.minArgIntValue = minArgIntValue;
            this.maxArgIntValue = maxArgIntValue;

        }

        public int intValue() {
            return argIntValue;
        }

        public boolean isValid(int value) {
            if ( value >= minArgIntValue && value <= maxArgIntValue ) {
                return true;
            }
            return false;
        }

        public boolean isValid(String stringValue) {
            int intValue;
            try {
                intValue = Integer.parseInt(stringValue);
            } catch (NumberFormatException exception) {
                return false;
            }
            return isValid(intValue);
        }

        public int max() {
            return maxArgIntValue;
        }

        public int min() {
            return minArgIntValue;
        }

        public void setValue(int value) {
            argIntValue = value;
        }

        public String stringValueOf(int value) {
            return "" + value;
        }

    } // end of TestIntegerArgument static class

    /*
     * Simple implementation of Connector.BooleanArgument
     */
    public static class TestBooleanArgument extends TestArgument implements Connector.BooleanArgument {

        static final String argStringValueTrue = "true";
        static final String argStringValueFalse = "false";
        boolean argBooleanValue;

        public TestBooleanArgument(String argName,
                        String argLabel,
                        String argDescription,
                        boolean argValue,
                        boolean argMustSpecify) {

            super(argName, argLabel, argDescription,
                argValue ? argStringValueTrue : argStringValueFalse,
                argMustSpecify);
            this.argBooleanValue = argValue;

        }

        public boolean booleanValue() {
            return argBooleanValue;
        }

        public boolean isValid(String stringValue) {
            if ( argStringValueTrue.equals(stringValue) || argStringValueFalse.equals(stringValue) ) {
                return true;
            }
            return false;
        }

        public void setValue(boolean value) {
            argBooleanValue = value;
        }

        public String stringValueOf(boolean value) {
            if ( value ) {
                return argStringValueTrue;
            }
            return argStringValueFalse;
        }

    } // end of TestBooleanArgument static class

    /*
     * Simple implementation of Connector.SelectedArgument
     */
    public static class TestSelectedArgument extends TestArgument implements Connector.SelectedArgument {

        List<String> acceptableArgsList;

        public TestSelectedArgument( String argName,
                        String argLabel,
                        String argDescription,
                        String argValue,
                        List<String> acceptableArgsList,
                        boolean argMustSpecify) {

            super(argName, argLabel, argDescription, argValue, argMustSpecify);
            this.acceptableArgsList = acceptableArgsList;
        }

        public List<String> choices() {
            return acceptableArgsList;
        }

        public boolean isValid(String stringValue) {

            return acceptableArgsList.contains(stringValue);
        }

    } // end of TestSelectedArgument static class

    public PlugConnectors(
        String plugConnectorName,
        String plugConnectorDescription,
        Transport plugConnectorTransport,
        Map<java.lang.String,com.sun.jdi.connect.Connector.Argument> plugConnectorDefaultArguments
        ) {

        this.plugConnectorName = plugConnectorName;
        this.plugConnectorDescription = plugConnectorDescription;
        this.plugConnectorTransport = plugConnectorTransport;
        this.plugConnectorDefaultArguments = plugConnectorDefaultArguments;
    }

    public String name() {
        return plugConnectorName;
    }

    public String description() {
        return plugConnectorDescription;
    }

    public Transport transport() {
        return plugConnectorTransport;
    }

    public Map<java.lang.String,com.sun.jdi.connect.Connector.Argument> defaultArguments() {
        return plugConnectorDefaultArguments;
    }

    public VirtualMachine launch(Map<String,? extends Connector.Argument> arguments) throws
                                IOException,
                                IllegalConnectorArgumentsException,
                                VMStartException {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of launch(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public VirtualMachine attach(Map<String,? extends Connector.Argument> arguments) throws
                                java.io.IOException,
                                IllegalConnectorArgumentsException {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of attach(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public String startListening(Map<String,? extends Connector.Argument> arguments) throws
                                java.io.IOException,
                                IllegalConnectorArgumentsException {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of startListening(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public void stopListening(Map<String,? extends Connector.Argument> arguments) throws
                                java.io.IOException,
                                IllegalConnectorArgumentsException {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of stopListening(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }
    }

    public VirtualMachine accept(Map<String,? extends Connector.Argument> arguments) throws
                                java.io.IOException,
                                IllegalConnectorArgumentsException {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of accept(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public boolean supportsMultipleConnections() {

        String exceptionMessage = "## PlugConnectors: Connector name = '" +
            plugConnectorName + "';\nNon-authorized call of supportsMultipleConnections() method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return false;
    }

    public static class PlugConnectorsTransport implements Transport {

        String transportName = "Undefined_Transport_Name";

        public PlugConnectorsTransport() {
        }

        public PlugConnectorsTransport(String transportName) {
            this.transportName = transportName;
        }

        public String name() {
            return transportName;
        }

    } // end of PlugConnectorsTransport class

    // Auxiliary general purpose methods for pluggable connectors' tests.

    public static String compareConnectors(
        String errorLogPrefixHead,
        String errorLogPrefix,
        Connector referenceConnector,
        Connector checkedConnector) {

        String emptyString = "";
        String errorMessage = emptyString;

        // check that connectors have the same name
        String referenceConnectorName = referenceConnector.name();
        String checkedConnectorName = checkedConnector.name();
        if ( ! referenceConnectorName.equals(checkedConnectorName) ) {
            errorMessage =  errorMessage +
                errorLogPrefixHead + "Checked pluggable Connector has unexpected name:\n"
                + errorLogPrefix + "Expected Connector's name = '" + referenceConnectorName + "'\n"
                + errorLogPrefix + "Actual Connector's name = '" + checkedConnectorName + "'\n";
        }

        // check that connectors have the same description
        String referenceConnectorDescription = referenceConnector.description();
        String checkedConnectorDescription = checkedConnector.description();
        if ( ! referenceConnectorDescription.equals(checkedConnectorDescription) ) {
            errorMessage =  errorMessage +
                errorLogPrefixHead + "Checked pluggable Connector has unexpected description:\n"
                + errorLogPrefix + "Expected Connector's description = '" + referenceConnectorDescription + "'\n"
                + errorLogPrefix + "Actual Connector's description = '" + checkedConnectorDescription + "'\n";
        }

        // check that connectors have the same transport name
        String referenceConnectorTransportName = referenceConnector.transport().name();
        String checkedConnectorTransportName = checkedConnector.transport().name();
        if ( ! referenceConnectorTransportName.equals(checkedConnectorTransportName) ) {
            errorMessage =  errorMessage +
                errorLogPrefixHead + "Checked pluggable Connector has unexpected transport name:\n"
                + errorLogPrefix + "Expected Connector's transport name = '" + referenceConnectorTransportName + "'\n"
                + errorLogPrefix + "Actual Connector's transport name = '" + checkedConnectorTransportName + "'\n";
        }

        // check that connectors have the same number of default arguments
        int referenceConnectorArgumentsNumber = referenceConnector.defaultArguments().size();
        int checkedConnectorArgumentsNumber = checkedConnector.defaultArguments().size();
        if ( referenceConnectorArgumentsNumber != checkedConnectorArgumentsNumber ) {
            errorMessage =  errorMessage +
                errorLogPrefixHead + "Checked pluggable Connector has unexpected number of default arguments:\n"
                + errorLogPrefix + "Expected number of default arguments = '" + referenceConnectorArgumentsNumber + "'\n"
                + errorLogPrefix + "Actual number of default arguments = '" + checkedConnectorArgumentsNumber + "'\n";
        }


        return errorMessage;
    }  // end of compareConnectors(...) method

    public static String compareConnectorArguments(
        String errorLogPrefixHead,
        String errorLogPrefix,
        Connector.Argument referenceArgument,
        Connector.Argument checkedArgument) {

        String emptyString = "";
        String errorMessage = emptyString;

        if ( referenceArgument == null ) {
            errorMessage =
                errorLogPrefixHead + "Reference connector's argument is null!\n";
        }

        if ( checkedArgument == null ) {
            errorMessage = errorMessage +
                errorLogPrefixHead + "Checked connector's argument is null!\n";
        }

        if ( ! errorMessage.equals(emptyString) ) {
            return errorMessage;
        }

        String referenceArgumentName = referenceArgument.name();
        String checkedArgumentName = checkedArgument.name();
        if ( ! referenceArgumentName.equals(checkedArgumentName) ) {
            errorMessage =
                errorLogPrefixHead + "Checked connector's argument has unexpected name:\n"
                + errorLogPrefix + "Expected connector's argument name = '" + referenceArgumentName + "'\n"
                + errorLogPrefix + "Actual connector's argument name = '" + checkedArgumentName + "'";
            return errorMessage;
        }

        String referenceArgumentLabel = referenceArgument.label();
        String checkedArgumentLabel = checkedArgument.label();
        if ( ! referenceArgumentLabel.equals(checkedArgumentLabel) ) {
            errorMessage =
                errorLogPrefixHead + "Checked connector's argument has unexpected label:\n"
                + errorLogPrefix + "Expected connector's argument label = '" + referenceArgumentLabel + "'\n"
                + errorLogPrefix + "Actual connector's argument label = '" + checkedArgumentLabel + "'";
            return errorMessage;
        }

        String referenceArgumentDescription = referenceArgument.description();
        String checkedArgumentDescription = checkedArgument.description();
        if ( ! referenceArgumentDescription.equals(checkedArgumentDescription) ) {
            errorMessage =
                errorLogPrefixHead + "Checked connector's argument has unexpected description:\n"
                + errorLogPrefix + "Expected connector's argument description = '" + referenceArgumentDescription + "'\n"
                + errorLogPrefix + "Actual connector's argument description = '" + checkedArgumentDescription + "'";
            return errorMessage;
        }

        String referenceArgumentValue = referenceArgument.value();
        String checkedArgumentValue = checkedArgument.value();
        if ( ! referenceArgumentValue.equals(checkedArgumentValue) ) {
            errorMessage =
                errorLogPrefixHead + "Checked connector's argument has unexpected value:\n"
                + errorLogPrefix + "Expected connector's argument value = '" + referenceArgumentValue + "'\n"
                + errorLogPrefix + "Actual connector's argument value = '" + checkedArgumentValue + "'";
            return errorMessage;
        }

        boolean referenceArgumentMustSpecify = referenceArgument.mustSpecify();
        boolean checkedArgumentMustSpecify = checkedArgument.mustSpecify();
        if ( referenceArgumentMustSpecify != checkedArgumentMustSpecify ) {
            errorMessage =
                errorLogPrefixHead + "Checked connector's argument has unexpected 'mustSpecify' property:\n"
                + errorLogPrefix + "Expected connector's argument 'mustSpecify' property = '"
                    + referenceArgumentMustSpecify + "'\n"
                + errorLogPrefix + "Actual connector's argument 'mustSpecify' property = '"
                    + checkedArgumentMustSpecify + "'";
            return errorMessage;
        }

        if ( referenceArgument instanceof Connector.IntegerArgument ) {

            int referenceArgumentMin = ((Connector.IntegerArgument)referenceArgument).min();
            int checkedArgumentMin = ((Connector.IntegerArgument)checkedArgument).min();
            if ( referenceArgumentMin != checkedArgumentMin ) {
                errorMessage =
                    errorLogPrefixHead + "Checked connector's integer argument has unexpected min value:\n"
                    + errorLogPrefix + "Expected connector's argument min value = "
                        + referenceArgumentMin + "\n"
                    + errorLogPrefix + "Actual connector's argument min value = "
                        + checkedArgumentMin + "\n";
            }

            int referenceArgumentMax = ((Connector.IntegerArgument)referenceArgument).max();
            int checkedArgumentMax = ((Connector.IntegerArgument)checkedArgument).max();
            if ( referenceArgumentMax != checkedArgumentMax ) {
                errorMessage = errorMessage +
                    errorLogPrefixHead + "Checked connector's integer argument has unexpected max value:\n"
                    + errorLogPrefix + "Expected connector's argument max value = "
                        + referenceArgumentMax + "\n"
                    + errorLogPrefix + "Actual connector's argument max value = "
                        + checkedArgumentMax + "\n";
            }

        }

        if ( referenceArgument instanceof Connector.SelectedArgument ) {

            List referenceArgumentChoices = ((Connector.SelectedArgument)referenceArgument).choices();
            List checkedArgumentChoices = ((Connector.SelectedArgument)checkedArgument).choices();

            int referenceArgumentChoicesSize = referenceArgumentChoices.size();
            int checkedArgumentChoicesSize = checkedArgumentChoices.size();

            if ( referenceArgumentChoicesSize != checkedArgumentChoicesSize) {
                errorMessage = errorMessage +
                    errorLogPrefixHead + "Checked connector's Selected argument has unexpected choices' size:\n"
                    + errorLogPrefix + "Expected size = '"
                        + referenceArgumentChoicesSize + "'\n"
                    + errorLogPrefix + "Actual size = "
                        + checkedArgumentChoicesSize;
                return errorMessage;
            }

            for (int i=0; i < referenceArgumentChoicesSize; i++) {
                String referenceArgumentChoice = (String)(referenceArgumentChoices.get(i));
                if ( ! checkedArgumentChoices.contains(referenceArgumentChoice) ) {
                    errorMessage = errorMessage +
                        errorLogPrefixHead + "Checked connector's Selected argument has NOT expected choices' element:\n"
                        + errorLogPrefix + "Expected choices' element = '"
                            + referenceArgumentChoice + "'\n";
                }

                String checkedArgumentChoice = (String)(checkedArgumentChoices.get(i));
                if ( ! referenceArgumentChoices.contains(checkedArgumentChoice) ) {
                    errorMessage = errorMessage +
                        errorLogPrefixHead + "Checked connector's Selected argument has unexpected choices' element:\n"
                        + errorLogPrefix + "Unexpected choices' element = '"
                            + checkedArgumentChoice + "'\n";
                }
            }
        }
        return errorMessage;
    }  // end of compareConnectorArguments(...) method

    public static Connector.Argument getConnectorDefaultArgument(
            Connector connector,
            String argumentName) {

        Connector.Argument foundArgument = null;

        Map connectorDefaultArguments = connector.defaultArguments();
        Object[] defaultArgumentsArray = connectorDefaultArguments.values().toArray();

        for (int i=0; i < defaultArgumentsArray.length; i++) {
            Connector.Argument connectorArgument = (Connector.Argument)defaultArgumentsArray[i];
            if ( argumentName.equals(connectorArgument.name()) ) {
                foundArgument = connectorArgument;
                break;
            }
        }
        return foundArgument;
    }

} // end of PlugConnectors class
