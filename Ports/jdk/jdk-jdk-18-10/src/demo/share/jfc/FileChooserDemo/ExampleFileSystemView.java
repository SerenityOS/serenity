/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileSystemView;


/**
 * This is a simple example that uses the FileSystemView class.
 * You can provide a superclass of the FileSystemView class with your own functionality.
 *
 * @author Pavel Porvatov
 */
public class ExampleFileSystemView extends FileSystemView {

    /**
     * Creates a new folder with the default name "New folder". This method is invoked
     * when the user presses the "New folder" button.
     */
    public File createNewFolder(File containingDir) throws IOException {
        File result = new File(containingDir, "New folder");

        if (result.exists()) {
            throw new IOException("Directory 'New folder' exists");
        }

        if (!result.mkdir()) {
            throw new IOException("Cannot create directory");
        }

        return result;
    }

    /**
     * Returns a list which appears in a drop-down list of the FileChooser component.
     * In this implementation only the home directory is returned.
     */
    @Override
    public File[] getRoots() {
        return new File[] { getHomeDirectory() };
    }

    /**
     * Returns a string that represents a directory or a file in the FileChooser component.
     * A string with all upper case letters is returned for a directory.
     * A string with all lower case letters is returned for a file.
     */
    @Override
    public String getSystemDisplayName(File f) {
        String displayName = super.getSystemDisplayName(f);

        return f.isDirectory() ? displayName.toUpperCase() : displayName.
                toLowerCase();
    }
}
