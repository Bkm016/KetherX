package io.izzel.kether.common.loader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.izzel.kether.common.api.*;
import io.izzel.kether.common.api.data.ContextString;
import io.izzel.kether.common.api.data.SimpleQuest;
import io.izzel.kether.common.loader.types.ArgTypes;
import io.izzel.kether.common.util.LocalizedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class SimpleReader implements QuestReader {

    private final QuestService<?> service;
    private final char[] arr;
    private int index = 0;
    private int mark = 0;
    private int block = 0;

    public SimpleReader(QuestService<?> service, String text) {
        this.service = service;
        this.arr = text.toCharArray();
    }

    @Override
    public char peek() {
        return arr[index];
    }

    @Override
    public char peek(int n) {
        return arr[index + n];
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getMark() {
        return mark;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean hasNext() {
        skipBlank();
        return index < arr.length;
    }

    @Override
    public String nextToken() {
        skipBlank();
        if (arr.length - index >= 2 && peek() == '"') {
            index++;
            boolean close = false;
            int i;
            for (i = index; i < arr.length; ++i) {
                if (arr[i] == '"') close = true;
                else if (close) break;
            }
            if (!close) throw LocalizedException.of("string-not-close");
            String ret = new String(arr, index, i - 1 - index);
            index = i;
            return ret;
        } else {
            int begin = index;
            while (index < arr.length && !Character.isWhitespace(arr[index])) {
                index++;
            }
            return new String(arr, begin, index - begin);
        }
    }

    @Override
    public ContextString nextString() {
        String str = nextToken();
        if (!Character.isWhitespace(peek())) {
            String[] ids = nextToken().split(",");
            Map<String, BiFunction<QuestContext.Frame, String, String>> map = new HashMap<>();
            for (String id : ids) {
                Optional<BiFunction<QuestContext.Frame, String, String>> optional = service.getRegistry().getContextStringProcessor(id);
                optional.ifPresent(f -> map.put(id, f));
            }
            return new ContextString(str, map);
        }
        return new ContextString(str, ImmutableMap.of());
    }

    @Override
    public void mark() {
        this.mark = index;
    }

    @Override
    public void reset() {
        this.index = mark;
    }

    @Override
    public <T> QuestAction<T> nextAction() {
        skipBlank();
        String element = nextToken();
        Optional<QuestActionParser> optional = service.getRegistry().getParser(element);
        if (optional.isPresent()) {
            return optional.get().resolve(this);
        } else {
            throw LocalizedException.of("unknown-action", element);
        }
    }

    @Override
    public boolean flag(String name) {
        skipBlank();
        if (peek() == '-') {
            mark();
            String s = nextToken();
            if (s.substring(1).equals(name)) {
                return true;
            } else {
                reset();
                return false;
            }
        }
        return false;
    }

    @Override
    public <T> Optional<T> optionalFlag(String name, ArgType<T> flagType) {
        skipBlank();
        if (peek() == '-') {
            mark();
            String s = nextToken();
            if (s.substring(1).equals(name)) {
                return Optional.of(next(flagType));
            } else {
                reset();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<QuestAction<T>> nextList() {
        return nextList("[", "]");
    }

    @Override
    public Quest.Block nextBlock() {
        ImmutableList.Builder<QuestAction<?>> builder = ImmutableList.builder();
        builder.addAll(nextList("{", "}"));
        return new SimpleQuest.SimpleBlock("inner_" + (block++), builder.build());
    }

    @Override
    public InferType nextInferType() {
        mark();
        try {
            return new InferType(InferType.Type.LIST, nextInferList());
        } catch (LocalizedException ignored) {
            reset();
        }
        try {
            return new InferType(InferType.Type.BLOCK, nextBlock());
        } catch (LocalizedException ignored) {
            reset();
        }
        try {
            return new InferType(InferType.Type.ACTION, nextAction());
        } catch (LocalizedException ignored) {
            reset();
        }
        if (peek() == '*') {
            return new InferType(InferType.Type.VARIABLE, nextToken().substring(1));
        }
        mark();
        for (ArgTypes argTypes : ArgTypes.values()) {
            try {
                return new InferType(InferType.Type.fromArgTypes(argTypes), next(argTypes));
            } catch (LocalizedException ignored) {
                reset();
            }
        }
        return new InferType(InferType.Type.TOKEN, nextToken());
    }

    @Override
    public List<InferType> nextInferList() {
        String element = nextToken();
        if (!element.equals("[")) {
            throw LocalizedException.of("not-match", "[ / begin", element);
        } else {
            ImmutableList.Builder<InferType> builder = ImmutableList.builder();
            while (this.hasNext()) {
                this.mark();
                String end = this.nextToken();
                if (end.equals("]")) {
                    break;
                }
                this.reset();
                builder.add(this.nextInferType());
            }
            return builder.build();
        }
    }

    private <T> List<QuestAction<T>> nextList(String separatorBegin, String separatorEnd) {
        String element = nextToken();
        if (!element.equals(separatorBegin)) {
            throw LocalizedException.of("not-match", separatorBegin, element);
        } else {
            ImmutableList.Builder<QuestAction<T>> builder = ImmutableList.builder();
            while (this.hasNext()) {
                this.mark();
                String end = this.nextToken();
                if (end.equals(separatorEnd)) {
                    break;
                }
                this.reset();
                builder.add(this.nextAction());
            }
            return builder.build();
        }
    }

    private void skipBlank() {
        while (index < arr.length) {
            if (Character.isWhitespace(arr[index])) {
                index++;
            } else if (index + 1 < arr.length && arr[index] == '/' && arr[index + 1] == '/') {
                while (index < arr.length && arr[index] != '\n' && arr[index] != '\r') {
                    index++;
                }
            } else {
                break;
            }
        }
    }
}
