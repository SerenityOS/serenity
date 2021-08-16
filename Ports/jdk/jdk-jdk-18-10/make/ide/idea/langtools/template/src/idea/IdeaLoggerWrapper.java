package idea;

import org.apache.tools.ant.Task;

/**
 * This class implements a custom Ant task which replaces the standard Intellij IDEA Ant logger
 * with a custom one which generates tighter output.
 */
public class IdeaLoggerWrapper extends Task {
    public void execute() {
        new LangtoolsIdeaAntLogger(getProject());
    }
}
