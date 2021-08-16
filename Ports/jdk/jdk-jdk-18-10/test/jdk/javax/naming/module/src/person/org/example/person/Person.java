/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * This class has DirContext attributes and is used by the StorePerson test.
 * Implementation of the inetOrgPerson LDAP object class as defined in
 * <a href="http://www.ietf.org/internet-drafts/draft-smith-ldap-inetorgperson-03.txt">draft-smith-ldap-inetorgperson-03.txt</a>.
 */

package org.example.person;

import javax.naming.*;
import javax.naming.directory.*;

public class Person {

    private BasicAttributes attrs;

    public Person(String commonName, String surname) {

        attrs = new BasicAttributes(true);
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("inetOrgPerson");

        attrs.put(objectClass);
        attrs.put(new BasicAttribute("cn", commonName));
        attrs.put(new BasicAttribute("sn", surname));
    }

    public Attributes getAttributes() {
        return attrs;
    }

    public void setAudio(byte[] value) {
        attrs.put(new BasicAttribute("audio", value));
    }

    public void setBusinessCategory(String value) {
        attrs.put(new BasicAttribute("businessCategory", value));
    }

    public void setCarLicense(String value) {
        attrs.put(new BasicAttribute("carLicense", value));
    }

    public void setDepartmentNumber(String value) {
        attrs.put(new BasicAttribute("departmentNumber", value));
    }

    public void setDisplayName(String value) {
        attrs.put(new BasicAttribute("displayName", value));
    }

    public void setEmployeeNumber(String value) {
        attrs.put(new BasicAttribute("employeeNumber", value));
    }

    public void setEmployeeType(String value) {
        attrs.put(new BasicAttribute("employeeType", value));
    }

    public void setGivenName(String value) {
        attrs.put(new BasicAttribute("givenName", value));
    }

    public void setHomePhoneNumber(String value) {
        attrs.put(new BasicAttribute("homePhone", value));
    }

    public void setHomePostalAddress(String value) {
        attrs.put(new BasicAttribute("homePostalAddress", value));
    }

    public void setInitials(String value) {
        attrs.put(new BasicAttribute("initials", value));
    }

    public void setJPEGPhoto(String value) {
        attrs.put(new BasicAttribute("jpegPhoto", value));
    }

    public void setMailAddress(String value) {
        attrs.put(new BasicAttribute("mail", value));
    }

    public void setManagerName(String value) {
        attrs.put(new BasicAttribute("manager", value));
    }

    public void setMobileNumber(String value) {
        attrs.put(new BasicAttribute("mobile", value));
    }

    public void setOrganizationName(String value) {
        attrs.put(new BasicAttribute("o", value));
    }

    public void setPagerNumber(String value) {
        attrs.put(new BasicAttribute("pager", value));
    }

    public void setPhoto(String value) {
        attrs.put(new BasicAttribute("photo", value));
    }

    public void setRoomNumber(String value) {
        attrs.put(new BasicAttribute("roomNumber", value));
    }

    public void setSecretaryName(String value) {
        attrs.put(new BasicAttribute("secretary", value));
    }

    public void setUserID(String value) {
        attrs.put(new BasicAttribute("uid", value));
    }

    public void setUserCertificate(String value) {
        attrs.put(new BasicAttribute("userCertificate", value));
    }

    public void setX500UniqueID(String value) {
        attrs.put(new BasicAttribute("x500UniqueIdentifier", value));
    }

    public void setPreferredLanguage(String value) {
        attrs.put(new BasicAttribute("preferredLanguage", value));
    }

    public void setUserSMIMECertificate(String value) {
        attrs.put(new BasicAttribute("userSMIMECertificate", value));
    }

    public void setUserPKCS12(String value) {
        attrs.put(new BasicAttribute("userPKCS12", value));
    }

    /* ----- Attributes inherited from the Person LDAP object class ----- */

    public void setDescription(String value) {
        attrs.put(new BasicAttribute("description", value));
    }

    public void setDestinationIndicator(String value) {
        attrs.put(new BasicAttribute("destinationIndicator", value));
    }

    public void setFaxNumber(String value) {
        attrs.put(new BasicAttribute("facsimileTelephoneNumber", value));
    }

    public void setISDNNumber(String value) {
        attrs.put(new BasicAttribute("internationalISDNNumber", value));
    }

    public void setLocalityName(String value) {
        attrs.put(new BasicAttribute("localityName", value));
    }

    public void setOrganizationalUnitName(String value) {
        attrs.put(new BasicAttribute("ou", value));
    }

    public void setPhysicalDeliveryOfficeName(String value) {
        attrs.put(new BasicAttribute("physicalDeliveryOfficeName", value));
    }

    public void setPostalAddress(String value) {
        attrs.put(new BasicAttribute("postalAddress", value));
    }

    public void setPostalCode(String value) {
        attrs.put(new BasicAttribute("postalCode", value));
    }

    public void setPostOfficeBox(String value) {
        attrs.put(new BasicAttribute("postOfficeBox", value));
    }

    public void setPreferredDeliveryMethod(String value) {
        attrs.put(new BasicAttribute("preferredDeliveryMethod", value));
    }

    public void setRegisteredAddress(String value) {
        attrs.put(new BasicAttribute("registeredAddress", value));
    }

    public void setSeeAlso(String value) {
        attrs.put(new BasicAttribute("seeAlso", value));
    }

    public void setStateOrProvinceName(String value) {
        attrs.put(new BasicAttribute("st", value));
    }

    public void setStreetAddress(String value) {
        attrs.put(new BasicAttribute("street", value));
    }

    public void setTelephoneNumber(String value) {
        attrs.put(new BasicAttribute("telephoneNumber", value));
    }

    public void setTeletexTerminalID(String value) {
        attrs.put(new BasicAttribute("teletexTerminalIdentifier", value));
    }

    public void setTelexNumber(String value) {
        attrs.put(new BasicAttribute("telexNumber", value));
    }

    public void setTitle(String value) {
        attrs.put(new BasicAttribute("title", value));
    }

    public void setUserPassword(String value) {
        attrs.put(new BasicAttribute("userPassword", value));
    }

    public void setX121Address(String value) {
        attrs.put(new BasicAttribute("x121Address", value));
    }
}
