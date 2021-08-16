/**@test /nodynamiccopyright/
 * @compile/fail/ref=Test.out -Xplugin:coding_rules -XDrawDiagnostics Test.java
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;

public class Test implements SourcePositions, TaskListener {
    @Override @DefinedBy(Api.COMPILER_TREE)
    public long getStartPosition(CompilationUnitTree file, Tree tree) {
        return 0;
    }
    @Override
    public long getEndPosition(CompilationUnitTree file, Tree tree) {
        return 0;
    }
    @DefinedBy(Api.COMPILER_TREE)
    public long getEndPosition(Tree tree) {
        return 0;
    }
    @Override @DefinedBy(Api.LANGUAGE_MODEL)
    public void started(TaskEvent e) {
    }
    @Override @DefinedBy(Api.COMPILER_TREE)
    public void finished(TaskEvent e) {
    }
}
