package io.izzel.kether.common.actions;

import io.izzel.kether.common.api.persistent.KetherCompleters;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.QuestActionParser;
import io.izzel.kether.common.api.QuestContext;
import io.izzel.kether.common.api.QuestService;
import io.izzel.kether.common.loader.InferType;
import io.izzel.kether.common.util.Coerce;

import java.util.concurrent.CompletableFuture;

final class IfAction extends QuestAction<Object> {

    private final InferType condition;
    private final InferType trueAction;
    private final InferType falseAction;

    public IfAction(InferType condition, InferType trueAction, InferType falseAction) {
        this.condition = condition;
        this.trueAction = trueAction;
        this.falseAction = falseAction;
    }

    @Override
    public CompletableFuture<Object> process(QuestContext.Frame frame) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        condition.process(frame).thenAccept(t -> {
            if (Coerce.toBoolean(t)) {
                trueAction.process(frame).thenAccept(future::complete);
            } else {
                falseAction.process(frame).thenAccept(future::complete);
            }
        });
        return future;
    }

    @Override
    public String toString() {
        return "IfAction{" +
            "condition=" + condition +
            ", trueAction=" + trueAction +
            ", falseAction=" + falseAction +
            '}';
    }

    public static <U, CTX extends QuestContext> QuestActionParser parser(QuestService<CTX> service) {
        return QuestActionParser.of(
            resolver -> {
                InferType condition = resolver.nextInferType();
                InferType trueAction = resolver.expect("then").nextInferType();
                if (resolver.hasNext()) {
                    resolver.mark();
                    String element = resolver.nextToken();
                    if (element.equals("else")) {
                        InferType falseAction = resolver.nextInferType();
                        return new IfAction(condition, trueAction, falseAction);
                    } else {
                        resolver.reset();
                    }
                }
                return new IfAction(condition, trueAction, new InferType(InferType.Type.ACTION, QuestAction.noop()));
            },
            KetherCompleters.seq(
                KetherCompleters.action(service),
                KetherCompleters.constant("then"),
                KetherCompleters.action(service),
                KetherCompleters.optional(
                    KetherCompleters.seq(
                        KetherCompleters.constant("else"),
                        KetherCompleters.action(service)
                    )
                )
            )
        );
    }
}
