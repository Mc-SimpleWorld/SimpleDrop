package org.nott.simpledrop.global;

public interface KeyWord {

    interface CONFIG {
        String DROP_ENABLE = "drop.enable";
        String OFFER_ENABLE = "offer.enable";
        String DROP_INVENTORY = "death_drop.inventory";
        String DROP_HEAD = "death_drop.head";
        String DROP_HEAD_PROB = "drop.head.probability";
        String DROP_STEAL_PROB = "drop.steal.probability";
        String DROP_STEAL_MAX = "drop.steal.max";
        String DROP_FILTER = "drop.filter";
        String SUPPORT_TOWNY = "support.towny";
        String REG_DEATH = "registry.death_success";
        String REG_OFFER = "registry.offer_success";
        String DIS_REG_DEATH = "disregistry.death_success";
        String DIS_REG_OFFER = "disregistry.offer_success";
    }

    interface TABLE {
        String OFFER = "offer_info";
        String OFFER_CREATE_SQL = "CREATE TABLE \"offer_info\" (\"id\" text NOT NULL,\"amount\" integer,PRIMARY KEY (\"id\"));";
    }

    interface MSG {
        String OFFER_GET_REWARD = "offer.get_reward";
    }

    interface COMMAND{
        String SD = "simpledrop";
        String OFFER = "offer";
    }
}
