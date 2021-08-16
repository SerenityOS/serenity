/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package validation;

import static jaxp.library.JAXPTestUtilities.USER_DIR;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;

import javax.xml.validation.SchemaFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 4987574
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4987574
 * @run testng/othervm validation.Bug4987574
 * @summary Test schemaFactory.newSchema doesn't throw NullPointerExceptio for empty schema.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4987574 {

    @Test
    public void test1() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File tmpFile = File.createTempFile("jaxpri", "bug", Paths.get(USER_DIR).toFile());
        tmpFile.deleteOnExit();
        {
            PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));
            pw.println("<schema xmlns='http://www.w3.org/2001/XMLSchema'/>");
            pw.close();
        }

        schemaFactory.newSchema(tmpFile);
    }
}
