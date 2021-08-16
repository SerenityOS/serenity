package version;

public class Nested {
    public int getVersion() {
        return 9;
    }

    protected void doNothing() {
    }

    class nested {
        int save = getVersion();
    }
}
