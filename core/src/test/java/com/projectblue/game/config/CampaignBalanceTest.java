package com.projectblue.game.config;

import com.projectblue.game.config.Loadout.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class CampaignBalanceTest {
    private record Income(int enemy, int waste, int boss, int cap) {
        int at(float enemyRate,float wasteRate) { return Math.min(cap,Math.round(enemy*enemyRate+waste*wasteRate+boss)); }
        int maximum() { return cap; }
    }

    private static Income income(MissionConfig mission) {
        int enemy=0,waste=0;
        for (MissionConfig.Wave wave:mission.waves())
            enemy+=wave.count()*mission.enemy(wave.enemy()).reward().salvage();
        for (MissionConfig.Prop prop:mission.props())
            if (prop.kind()==MissionConfig.SpawnKind.WASTE) waste+=mission.waste(prop.waste()).salvage();
        return new Income(enemy,waste,mission.boss.salvage(),mission.salvageCap);
    }

    private static int campaignIncome(float enemyRate,float wasteRate) {
        int total=0;
        for (int id=1;id<=CampaignConfig.LEVEL_COUNT;id++) total+=income(MissionConfig.forLevel(id)).at(enemyRate,wasteRate);
        return total;
    }

    private static int totalUpgradeCost() {
        int total=0;
        for (Upgrade upgrade:Upgrade.values()) for (int level=0;level<upgrade.definition().maxLevel();level++)
            total+=upgrade.cost(level);
        return total;
    }

    private static int affordableLevels(int budget) {
        int[] levels=new int[Upgrade.values().length];
        int purchased=0;
        while (true) {
            int choice=-1,cost=Integer.MAX_VALUE;
            for (Upgrade upgrade:Upgrade.values()) {
                int current=levels[upgrade.ordinal()];
                if (current<upgrade.definition().maxLevel() && upgrade.cost(current)<cost) {
                    choice=upgrade.ordinal(); cost=upgrade.cost(current);
                }
            }
            if (choice<0 || cost>budget) return purchased;
            budget-=cost; levels[choice]++; purchased++;
        }
    }

    @Test void firstNormalCampaignFundsManyChoicesButCannotMaxEveryUpgrade() {
        int maximum=campaignIncome(1,1);
        int cautious=campaignIncome(.45f,.65f);
        int expected=campaignIncome(.65f,.85f);
        assertEquals(3781,maximum);
        assertTrue(totalUpgradeCost()>maximum,"Even perfect first clears must leave replay goals");
        assertTrue(affordableLevels(cautious)>=18,"No-ad progression must remain practical");
        assertTrue(affordableLevels(expected)>=21);
        assertTrue(affordableLevels(maximum)<Upgrade.values().length*Upgrade.MAX_LEVEL);
    }

    @Test void firstSectorAlreadyFundsSeveralDifferentUpgradeChoices() {
        int cautious=income(MissionConfig.BLUE_COAST).at(.45f,.65f);
        assertTrue(affordableLevels(cautious)>=4);
        assertTrue(cautious<Arrays.stream(Upgrade.values()).mapToInt(u->u.cost(0)).sum());
    }

    @Test void vesselsAndWeaponsUseTradeoffsInsteadOfOneDominantDamageChoice() {
        var tide=Submarine.TIDE.definition();
        var manta=Submarine.MANTA.definition();
        var leviathan=Submarine.LEVIATHAN.definition();
        assertTrue(manta.movementSpeed()>tide.movementSpeed() && manta.cleanupPower()>tide.cleanupPower());
        assertTrue(leviathan.baseHealth()>tide.baseHealth() && leviathan.shieldCapacity()>tide.shieldCapacity());
        assertTrue(tide.primaryDamage()*tide.fireRate()>manta.primaryDamage()*manta.fireRate());
        assertTrue(tide.primaryDamage()*tide.fireRate()>leviathan.primaryDamage()*leviathan.fireRate());
        for (Weapon weapon:Weapon.values()) {
            var def=weapon.definition();
            float idealOutput=def.damageMultiplier()*def.rateMultiplier()*def.projectiles();
            assertTrue(idealOutput>=.95f && idealOutput<=1.1f,weapon+" ideal output="+idealOutput);
        }
    }

    @Test void pilotsOwnDistinctModerateSpecialties() {
        EnumSet<ContentCatalog.Stat> specialties=EnumSet.noneOf(ContentCatalog.Stat.class);
        for (Pilot pilot:Pilot.values()) {
            var passives=pilot.definition().passives();
            assertEquals(1,passives.size());
            assertTrue(specialties.add(passives.get(0).stat()));
            assertTrue(passives.get(0).amount()>=.1f && passives.get(0).amount()<=.2f);
        }
        assertEquals(Pilot.values().length,specialties.size());
    }
}
