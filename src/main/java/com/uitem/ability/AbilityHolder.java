package com.uitem.ability;

import java.util.List;

/** Item hoặc potion UItem có thể gán rule kỹ năng. */
public interface AbilityHolder {

    String getAbilityId();

    boolean isAbilitiesEnabled();

    void setAbilitiesEnabled(boolean enabled);

    List<ItemAbilityRule> getAbilities();

    void setAbilities(List<ItemAbilityRule> rules);
}