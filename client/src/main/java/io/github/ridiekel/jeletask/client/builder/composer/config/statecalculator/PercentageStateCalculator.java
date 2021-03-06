package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;

public class PercentageStateCalculator extends SimpleStateCalculator {
    public PercentageStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public boolean isValidState(String state) {
        Long value = Long.valueOf(state);
        return value >= 0 && value <= 100;
    }
}