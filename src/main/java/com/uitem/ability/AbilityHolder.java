package com.uitem.ability;

import java.util.List;

/** Item hoac potion UItem co the gan rule ky nang. */
public interface AbilityHolder {

    String getAbilityId();

    boolean isAbilitiesEnabled();

    void setAbilitiesEnabled(boolean enabled);

    List<ItemAbilityRule> getAbilities();

    void setAbilities(List<ItemAbilityRule> rules);
}