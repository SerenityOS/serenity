/*
 * @test /nodynamiccopyright/
 * @bug 4974524
 * @summary compiler crash with ill-formed annotation
 * @author gafter
 *
 * @compile/fail/ref=Syntax1.out -XDrawDiagnostics  Syntax1.java
 */

package syntax1;

import java.lang.annotation.*;
import java.util.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD, ElementType.TYPE)
public @interface Syntax1
{
    String elementName();
}
