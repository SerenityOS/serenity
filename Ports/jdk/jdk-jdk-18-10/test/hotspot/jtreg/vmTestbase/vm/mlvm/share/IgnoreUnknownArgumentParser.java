package vm.mlvm.share;

import nsk.share.ArgumentParser;

public class IgnoreUnknownArgumentParser extends ArgumentParser {

    IgnoreUnknownArgumentParser(String[] args) {
        super(args);
    }

    @Override
    protected boolean checkOption(String option, String value) {
        return true;
    }

}
