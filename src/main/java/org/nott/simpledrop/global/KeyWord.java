package org.nott.simpledrop.global;

public interface KeyWord {

    public interface CONFIG {
        String BANK_ENABLE = "bank.enable";
        String DROP_ENABLE = "drop.enable";
        String DROP_INVENTORY = "death_drop.inventory";
        String DROP_HEAD = "death_drop.head";
        String DROP_HEAD_PROB = "drop.head.probability";
        String DROP_STEAL_PROB = "drop.steal.probability";
        String DROP_STEAL_MAX = "drop.steal.max";
        String DROP_FILTER = "drop.filter";
        String REG_DEATH = "registry.death_success";
        String DIS_REG_DEATH = "disregistry.death_success";
    }
}
