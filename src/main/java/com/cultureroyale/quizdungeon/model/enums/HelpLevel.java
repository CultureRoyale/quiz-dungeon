package com.cultureroyale.quizdungeon.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HelpLevel {
    NO_HELP(3.0), // Max dégâts (x3)
    FOUR_CHOICES(1.0), // Normal (x1)
    TWO_CHOICES(0.5); // Min dégâts (x0.5)

    private final double damageMultiplier;
}
