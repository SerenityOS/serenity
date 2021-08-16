/*
 * @test /nodynamiccopyright/
 * @bug 4993451
 * @summary compiler crash with malformed annotations
 * @author gafter
 *
 * @compile/fail/ref=Recovery.out -XDrawDiagnostics  Recovery.java
 */

import java.lang.annotation.*;

@RetentionPolicy(RetentionPolicy.RUNTIME)
public @interface Recovery {}
