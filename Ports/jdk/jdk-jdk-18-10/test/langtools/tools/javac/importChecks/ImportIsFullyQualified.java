/*
 * @test /nodynamiccopyright/
 * @bug 4335264
 * @summary Verify that import-on-demand must be fully qualified.
 * @author maddox
 *
 * @compile/fail/ref=ImportIsFullyQualified.out -XDrawDiagnostics  ImportIsFullyQualified.java
 */

import java.awt.*;
import JobAttributes.*;  // class JobAttributes is contained in package java.awt

public class ImportIsFullyQualified {
    JobAttributes.DefaultSelectionType x;
}
