package pe.edu.unaj.amoblador3d.design;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FSHFHistory {
    private static final int MAX_STEPS = 100;

    private final Supplier<String> capture;
    private final Consumer<String> restore;
    private final Runnable onChange;
    private final List<String> stack = new ArrayList<>();
    private int index;
    private String saved;
    private boolean pending;

    public FSHFHistory(Supplier<String> capture, Consumer<String> restore, Runnable onChange) {
        this.capture = capture;
        this.restore = restore;
        this.onChange = onChange;
        start();
    }

    private void start() {
        stack.clear();
        stack.add(capture.get());
        index = 0;
        saved = stack.get(0);
        pending = false;
    }

    public void reset() {
        start();
        onChange.run();
    }

    public void commit() {
        pending = false;
        String snapshot = capture.get();
        if (snapshot.equals(stack.get(index))) return;
        stack.subList(index + 1, stack.size()).clear();
        stack.add(snapshot);
        if (stack.size() > MAX_STEPS) stack.remove(0);
        index = stack.size() - 1;
        onChange.run();
    }

    public void markPending() {
        pending = true;
    }

    public void flush() {
        if (pending) commit();
    }

    public boolean undo() {
        flush();
        if (index == 0) return false;
        index--;
        restore.accept(stack.get(index));
        onChange.run();
        return true;
    }

    public boolean redo() {
        flush();
        if (index >= stack.size() - 1) return false;
        index++;
        restore.accept(stack.get(index));
        onChange.run();
        return true;
    }

    public void markSaved() {
        flush();
        saved = stack.get(index);
        onChange.run();
    }

    public void markUnsaved() {
        saved = null;
        onChange.run();
    }

    public boolean isDirty() {
        return pending || saved == null || !stack.get(index).equals(saved);
    }

    public boolean canUndo() {
        return index > 0 || pending;
    }

    public boolean canRedo() {
        return index < stack.size() - 1;
    }
}
