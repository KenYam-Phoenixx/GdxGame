package com.gdx.game.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.gdx.game.GdxRunner;
import com.gdx.game.entities.Entity;
import com.gdx.game.entities.EntityConfig;
import com.gdx.game.entities.EntityFactory;
import com.gdx.game.entities.npc.NPCGraphicsComponent;
import com.gdx.game.entities.player.PlayerGraphicsComponent;
import com.gdx.game.inventory.InventoryObserver;
import com.gdx.game.profile.ProfileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(GdxRunner.class)
public class BattleStateTest {

    private MockedConstruction<PlayerGraphicsComponent> mockPlayerGraphics;

    private MockedConstruction<NPCGraphicsComponent> mockNPCGraphics;

    private final ProfileManager profileManager = ProfileManager.getInstance();

    @BeforeEach
    void init() {
        Gdx.gl = mock(GL20.class);
        Gdx.gl20 = mock(GL20.class);
        mockPlayerGraphics = mockConstruction(PlayerGraphicsComponent.class);
        mockNPCGraphics = mockConstruction(NPCGraphicsComponent.class);
        profileManager.setProperty("currentPlayerAP", 5);
        profileManager.setProperty("currentPlayerDP", 5);
        profileManager.setProperty("currentPlayerMP", 5);
        profileManager.setProperty("currentPlayerHP", 20);
    }

    @AfterEach
    void end() {
        mockPlayerGraphics.close();
        mockNPCGraphics.close();
    }

    @Test
    void playerAttack_doNotKill() {
        BattleState battleState = spy(new BattleState());
        Entity player = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.WARRIOR);
        Entity enemy = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.ENEMY);
        enemy.getEntityConfig().setPropertyValue(EntityConfig.EntityProperties.ENTITY_HEALTH_POINTS.toString(), "20");
        enemy.getEntityConfig().setPropertyValue(EntityConfig.EntityProperties.ENTITY_PHYSICAL_DEFENSE_POINTS.toString(), "4");
        battleState.setPlayer(player);
        battleState.setCurrentOpponent(enemy);

        battleState.getPlayerAttackCalculationTimer().run();

        assertThat(enemy.getEntityConfig().getPropertyValue(EntityConfig.EntityProperties.ENTITY_HEALTH_POINTS.toString())).isLessThanOrEqualTo("19");
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.OPPONENT_HIT_DAMAGE);
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.PLAYER_TURN_DONE);
        verify(battleState, never()).notify(enemy, BattleObserver.BattleEvent.OPPONENT_DEFEATED);
    }

    @Test
    void playerAttack_killOpponent() {
        String dropId = "1";
        EntityConfig.Drop drop = new EntityConfig.Drop();
        drop.setProbability(1);
        drop.setItemTypeID(dropId);
        BattleState battleState = spy(new BattleState());
        Entity player = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.WARRIOR);
        Entity enemy = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.ENEMY);
        enemy.getEntityConfig().setPropertyValue(EntityConfig.EntityProperties.ENTITY_HEALTH_POINTS.toString(), "1");
        enemy.getEntityConfig().setPropertyValue(EntityConfig.EntityProperties.ENTITY_PHYSICAL_DEFENSE_POINTS.toString(), "4");
        enemy.getEntityConfig().addDrop(drop);
        battleState.setPlayer(player);
        battleState.setCurrentOpponent(enemy);

        battleState.getPlayerAttackCalculationTimer().run();

        assertThat(enemy.getEntityConfig().getPropertyValue(EntityConfig.EntityProperties.ENTITY_HEALTH_POINTS.toString())).isEqualTo("0");
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.OPPONENT_HIT_DAMAGE);
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.PLAYER_TURN_DONE);
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.OPPONENT_DEFEATED);
        verify(battleState).notify(dropId, InventoryObserver.InventoryEvent.DROP_ITEM_ADDED);
    }

    @Test
    void opponentAttack() {
        BattleState battleState = spy(new BattleState());
        Entity player = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.WARRIOR);
        Entity enemy = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.ENEMY);
        enemy.getEntityConfig().setPropertyValue(EntityConfig.EntityProperties.ENTITY_PHYSICAL_ATTACK_POINTS.toString(), "6");
        battleState.setPlayer(player);
        battleState.setCurrentOpponent(enemy);

        battleState.getOpponentAttackCalculationTimer().run();

        assertThat(player.getEntityConfig().getPropertyValue(EntityConfig.EntityProperties.ENTITY_HEALTH_POINTS.toString())).isLessThanOrEqualTo("19");
        verify(battleState).notify(player, BattleObserver.BattleEvent.PLAYER_HIT_DAMAGE);
        verify(battleState).notify(enemy, BattleObserver.BattleEvent.OPPONENT_TURN_DONE);
    }

    @Test
    void playerRuns_shouldSucceed() {
        BattleState battleState = spy(new BattleState());
        Entity enemy = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.ENEMY);
        battleState.setCurrentOpponent(enemy);
        battleState.setSpeedRatio(100);

        battleState.playerRuns();

        verify(battleState).notify(enemy, BattleObserver.BattleEvent.PLAYER_RUNNING);
    }

    @Test
    void playerRuns_shouldFail() {
        BattleState battleState = spy(new BattleState());
        Entity enemy = EntityFactory.getInstance().getEntity(EntityFactory.EntityType.ENEMY);
        battleState.setCurrentOpponent(enemy);
        battleState.setSpeedRatio(0);

        battleState.playerRuns();

        verify(battleState).notify(enemy, BattleObserver.BattleEvent.PLAYER_TURN_DONE);
    }
}
