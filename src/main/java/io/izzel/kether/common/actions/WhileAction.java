package io.izzel.kether.common.actions;

import io.izzel.kether.common.api.persistent.KetherCompleters;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.QuestActionParser;
import io.izzel.kether.common.api.QuestContext;
import io.izzel.kether.common.api.QuestService;
import io.izzel.kether.common.loader.InferType;
import io.izzel.kether.common.util.Coerce;

import java.util.concurrent.CompletableFuture;

final class WhileAction extends QuestAction<Void> {

    private final InferType condition;
    private final QuestAction<?> action;

    public WhileAction(InferType condition, QuestAction<?> action) {
        this.condition = condition;
        this.action = action;
    }

    @Override
    public CompletableFuture<Void> process(QuestContext.Frame frame) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        process(frame, future);
        return future;
    }

    private void process(QuestContext.Frame frame, CompletableFuture<Void> future) {
        condition.process(frame).thenAcceptAsync(t -> {
            if (Coerce.toBoolean(t)) {
                action.process(frame).thenRunAsync(
                    () -> process(frame, future),
                    frame.context().getExecutor()
                );
            } else {
                future.complete(null);
            }
        }, frame.context().getExecutor());
    }

    @Override
    public String toString() {
        return "WhileAction{" +
            "condition=" + condition +
            ", action=" + action +
            '}';
    }

    public static QuestActionParser parser(QuestService<?> service) {
        return QuestActionParser.of(
            resolver -> {
                InferType condition = resolver.nextInferType();
                QuestAction<?> action = resolver.expect("then").nextAction();
                return new WhileAction(condition, action);
            },
            KetherCompleters.seq(
                KetherCompleters.action(service),
                KetherCompleters.constant("then"),
                KetherCompleters.action(service)
            )
        );
    }
}
