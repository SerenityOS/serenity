/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www;
import java.net.URL;
import java.io.*;
import java.util.StringJoiner;
import java.util.StringTokenizer;

public class MimeEntry implements Cloneable {
    private String typeName;    // of the form: "type/subtype"
    private String tempFileNameTemplate;

    private int action;
    private String command;
    private String description;
    private String imageFileName;
    private String fileExtensions[];

    boolean starred;

    // Actions
    public static final int             UNKNOWN                 = 0;
    public static final int             LOAD_INTO_BROWSER       = 1;
    public static final int             SAVE_TO_FILE            = 2;
    public static final int             LAUNCH_APPLICATION      = 3;

    static final String[] actionKeywords = {
        "unknown",
        "browser",
        "save",
        "application",
    };

    /**
     * Construct an empty entry of the given type and subtype.
     */
    public MimeEntry(String type) {
        // Default action is UNKNOWN so clients can decide what the default
        // should be, typically save to file or ask user.
        this(type, UNKNOWN, null, null, null);
    }

    //
    // The next two constructors are used only by the deprecated
    // PlatformMimeTable classes or, in last case, is called by the public
    // constructor.  They are kept here anticipating putting support for
    // mailcap formatted config files back in (so BOTH the properties format
    // and the mailcap formats are supported).
    //
    MimeEntry(String type, String imageFileName, String extensionString) {
        typeName = type.toLowerCase();
        action = UNKNOWN;
        command = null;
        this.imageFileName = imageFileName;
        setExtensions(extensionString);
        starred = isStarred(typeName);
    }

    // For use with MimeTable::parseMailCap
    MimeEntry(String typeName, int action, String command,
              String tempFileNameTemplate) {
        this.typeName = typeName.toLowerCase();
        this.action = action;
        this.command = command;
        this.imageFileName = null;
        this.fileExtensions = null;

        this.tempFileNameTemplate = tempFileNameTemplate;
    }

    // This is the one called by the public constructor.
    MimeEntry(String typeName, int action, String command,
              String imageFileName, String fileExtensions[]) {

        this.typeName = typeName.toLowerCase();
        this.action = action;
        this.command = command;
        this.imageFileName = imageFileName;
        this.fileExtensions = fileExtensions;

        starred = isStarred(typeName);

    }

    public synchronized String getType() {
        return typeName;
    }

    public synchronized void setType(String type) {
        typeName = type.toLowerCase();
    }

    public synchronized int getAction() {
        return action;
    }

    public synchronized void setAction(int action, String command) {
        this.action = action;
        this.command = command;
    }

    public synchronized void setAction(int action) {
        this.action = action;
    }

    public synchronized String getLaunchString() {
        return command;
    }

    public synchronized void setCommand(String command) {
        this.command = command;
    }

    public synchronized String getDescription() {
        return (description != null ? description : typeName);
    }

    public synchronized void setDescription(String description) {
        this.description = description;
    }

    // ??? what to return for the image -- the file name or should this return
    // something more advanced like an image source or something?
    // returning the name has the least policy associated with it.
    // pro tempore, we'll use the name
    public String getImageFileName() {
        return imageFileName;
    }

    public synchronized void setImageFileName(String filename) {
        File file = new File(filename);
        if (file.getParent() == null) {
            imageFileName = System.getProperty(
                                     "java.net.ftp.imagepath."+filename);
        }
        else {
            imageFileName = filename;
        }

        if (filename.lastIndexOf('.') < 0) {
            imageFileName = imageFileName + ".gif";
        }
    }

    public String getTempFileTemplate() {
        return tempFileNameTemplate;
    }

    public synchronized String[] getExtensions() {
        return fileExtensions;
    }

    public synchronized String getExtensionsAsList() {
        String extensionsAsString = "";
        if (fileExtensions != null) {
            for (int i = 0; i < fileExtensions.length; i++) {
                extensionsAsString += fileExtensions[i];
                if (i < (fileExtensions.length - 1)) {
                    extensionsAsString += ",";
                }
            }
        }

        return extensionsAsString;
    }

    public synchronized void setExtensions(String extensionString) {
        StringTokenizer extTokens = new StringTokenizer(extensionString, ",");
        int numExts = extTokens.countTokens();
        String extensionStrings[] = new String[numExts];

        for (int i = 0; i < numExts; i++) {
            String ext = (String)extTokens.nextElement();
            extensionStrings[i] = ext.trim();
        }

        fileExtensions = extensionStrings;
    }

    private boolean isStarred(String typeName) {
        return typeName != null && typeName.endsWith("/*");
    }

    public boolean matches(String type) {
        if (starred) {
          // REMIND: is this the right thing or not?
          return type.startsWith(typeName);
        } else {
            return type.equals(typeName);
        }
    }

    public Object clone() {
        // return a shallow copy of this.
        MimeEntry theClone = new MimeEntry(typeName);
        theClone.action = action;
        theClone.command = command;
        theClone.description = description;
        theClone.imageFileName = imageFileName;
        theClone.tempFileNameTemplate = tempFileNameTemplate;
        theClone.fileExtensions = fileExtensions;

        return theClone;
    }

    public synchronized String toProperty() {
        StringJoiner sj = new StringJoiner("; ");

        int action = getAction();
        if (action != MimeEntry.UNKNOWN) {
            sj.add("action=" + actionKeywords[action]);
        }

        String command = getLaunchString();
        if (command != null && command.length() > 0) {
            sj.add("application=" + command);
        }

        String image = getImageFileName();
        if (image != null) {
            sj.add("icon=" + image);
        }

        String extensions = getExtensionsAsList();
        if (!extensions.isEmpty()) {
            sj.add("file_extensions=" + extensions);
        }

        String description = getDescription();
        if (description != null && !description.equals(getType())) {
            sj.add("description=" + description);
        }

        return sj.toString();
    }

    public String toString() {
        return "MimeEntry[contentType=" + typeName
            + ", image=" + imageFileName
            + ", action=" + action
            + ", command=" + command
            + ", extensions=" + getExtensionsAsList()
            + "]";
    }
}
