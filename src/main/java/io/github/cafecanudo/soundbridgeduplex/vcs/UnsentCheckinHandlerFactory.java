package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;

public final class UnsentCheckinHandlerFactory extends CheckinHandlerFactory {

    @Override
    public CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
        return new UnsentCheckinHandler(panel);
    }
}
