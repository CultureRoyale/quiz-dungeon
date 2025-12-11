package com.cultureroyale.quizdungeon.model.dto;

import com.cultureroyale.quizdungeon.model.enums.CombatResult;

public class TurnResult {
    public CombatResult status;
    public String message;
    public int bossHp;
    public int userHp;

    public TurnResult(CombatResult status, String message, int bossHp, int userHp) {
        this.status = status;
        this.message = message;
        this.bossHp = bossHp;
        this.userHp = userHp;
    }
}
