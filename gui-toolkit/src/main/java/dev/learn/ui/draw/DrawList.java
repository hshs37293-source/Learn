package dev.learn.ui.draw;

import java.util.ArrayList;
import java.util.List;

/** Zapis jednej klatki. Backend dostaje {@link #commands()} i je wykonuje po kolei. */
public final class DrawList {
    private final List<DrawCommand> cmds = new ArrayList<>();
    private int clipDepth;

    public DrawList add(DrawCommand c) {
        if (c instanceof DrawCommand.PushClip) clipDepth++;
        if (c instanceof DrawCommand.PopClip) {
            if (clipDepth == 0) throw new IllegalStateException("PopClip bez PushClip");
            clipDepth--;
        }
        cmds.add(c);
        return this;
    }

    public List<DrawCommand> commands() {
        if (clipDepth != 0) throw new IllegalStateException("Niezamknięty clip: " + clipDepth);
        return List.copyOf(cmds);
    }

    public void clear() { cmds.clear(); clipDepth = 0; }
}
