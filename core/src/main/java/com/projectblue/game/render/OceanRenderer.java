package com.projectblue.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.projectblue.game.logic.*;
import com.projectblue.game.ui.*;
import static com.projectblue.game.config.GameConfig.*;

/** Original procedural art. Visual geometry is in logical pixels; all gameplay tuning is in GameConfig. */
public final class OceanRenderer {
    private final UiPainter ui;
    private final ShapeRenderer s;
    private final Color top = new Color(), bottom = new Color(), reef = new Color();
    public OceanRenderer(UiPainter ui) { this.ui = ui; s = ui.shapes; }

    public void backdrop(float time, float restored) {
        ui.beginShapes();
        top.set(.045f, .23f + restored * .1f, .29f + restored * .07f, 1);
        bottom.set(.018f, .065f + restored * .06f, .12f + restored * .07f, 1);
        s.rect(0, 0, WIDTH, HEIGHT, bottom, bottom, top, top);
        s.setColor(.045f, .24f + restored * .08f, .29f + restored * .08f, 1);
        s.triangle(70, HEIGHT, 200, HEIGHT, 390, 0);
        s.triangle(290, HEIGHT, 345, HEIGHT, 520, 180);
        // Slow current contours and faster suspended motes create depth without texture downloads.
        for (int row = 0; row < 9; row++) {
            float y = ((row * 137f - time * 18) % 1200 + 1200) % 1200 - 120;
            s.setColor(.07f, .26f + restored * .06f, .31f + restored * .08f, 1);
            for (int x = -30; x < WIDTH; x += 30) {
                float a = y + (float) Math.sin(x * .014f + row) * 23;
                float b = y + (float) Math.sin((x + 30) * .014f + row) * 23;
                s.rectLine(x, a, x + 30, b, 1);
            }
        }
        for (int i = 0; i < 48; i++) {
            float x = (i * 173 % WIDTH) + (float) Math.sin(time * .2f + i) * 7;
            float y = ((i * 97 - time * (18 + i % 4 * 8)) % HEIGHT + HEIGHT) % HEIGHT;
            s.setColor(.18f, .40f + restored * .18f, .45f + restored * .1f, 1);
            s.circle(x, y, i % 3 == 0 ? 2 : 1, 8);
        }
        reef.set(.18f, .22f, .24f, 1).lerp(Palette.AQUA, restored * .72f);
        for (int i = 0; i < 10; i++) {
            float y = ((i * 113 - time * 26) % 1130 + 1130) % 1130 - 80;
            float x = i % 2 == 0 ? 8 : WIDTH - 8;
            coral(x, y, i % 2 == 0 ? 1 : -1, reef, i);
        }
        for (int i = 0; i < 2 + (int) (restored * 16); i++) {
            float x = (time * (14 + i % 3 * 5) + i * 117) % (WIDTH + 100) - 50;
            float y = 150 + i * 107 % 650 + (float) Math.sin(time + i) * 8;
            s.setColor(.24f, .51f + restored * .2f, .54f + restored * .16f, 1);
            s.ellipse(x, y, 17, 7, 12);
            s.triangle(x, y + 3, x - 6, y - 1, x - 6, y + 8);
        }
        ui.endShapes();
    }
    private void coral(float x, float y, int direction, Color color, int variation) {
        s.setColor(Palette.INK);
        s.ellipse(x - 39, y - 15, 78, 38, 18);
        s.setColor(color);
        for (int j = 0; j < 4; j++) {
            float endX = x + direction * (12 + j * 8), endY = y + 32 + (j * 13 + variation * 7) % 44;
            s.rectLine(x, y, endX, endY, 4);
            s.rectLine(endX - direction * 5, endY - 17, endX + direction * 11, endY - 9, 3);
            s.circle(endX, endY, 4, 10);
        }
    }
    public void world(GameWorld world) {
        backdrop(world.elapsed(), world.restoration());
        ui.beginShapes();
        for (int i = 0; i < world.corals.capacity(); i++) {
            Entity e = world.corals.at(i);
            if (!e.active) continue;
            Color color = e.health < e.maxHealth ? Palette.RED : reef;
            coral(e.x, e.y, e.x < WIDTH / 2f ? 1 : -1, color, i);
            ui.bar(e.x - 28, e.y + 48, 56, 3, (float)e.health / Math.max(1, e.maxHealth), color);
        }
        for (int i = 0; i < world.plastics.capacity(); i++) {
            Entity e = world.plastics.at(i);
            if (!e.active) continue;
            if (e.progress > 0) {
                s.setColor(Palette.AQUA); s.rectLine(world.player.x, world.player.y + 10, e.x, e.y, 2);
                float multiplier = e.waste == null ? 1 : e.waste.cleanMultiplier();
                ring(e.x, e.y, Math.max(22, e.radius + 5), e.progress / (world.spec().loadout().cleanupSeconds() * multiplier), Palette.AQUA);
            }
            waste(e);
        }
        for (int i = 0; i < world.turtles.capacity(); i++) {
            Entity e = world.turtles.at(i);
            if (!e.active) continue;
            turtle(e.x, e.y, 1, e.friendly);
            if (!e.friendly) ring(e.x, e.y, 43, e.progress / world.spec().loadout().rescueSeconds(), e.progress > 0 ? Palette.AQUA : Palette.MUTED);
        }
        for (int i = 0; i < world.salvage.capacity(); i++) {
            Entity e = world.salvage.at(i);
            if (e.active) {
                s.setColor(Palette.GOLD);
                s.triangle(e.x, e.y + 11, e.x - 8, e.y, e.x + 8, e.y);
                s.triangle(e.x, e.y - 11, e.x - 8, e.y, e.x + 8, e.y);
            }
        }
        for (int i = 0; i < world.drones.capacity(); i++) {
            Entity e = world.drones.at(i);
            if (e.active) drone(e);
        }
        for (int i = 0; i < world.bullets.capacity(); i++) {
            Entity b = world.bullets.at(i);
            if (!b.active) continue;
            s.setColor(b.friendly ? Palette.AQUA : b.slowSeconds > 0 ? Palette.MUTED : Palette.RED);
            if (b.friendly) { s.rect(b.x - 3, b.y - 8, 6, 19); s.setColor(Palette.TEXT); s.rect(b.x - 1, b.y, 2, 10); }
            else if (b.slowSeconds > 0) {
                s.circle(b.x, b.y, 9, 12);
                s.setColor(Palette.GOLD); s.rectLine(b.x - 7, b.y - 7, b.x + 7, b.y + 7, 1);
                s.rectLine(b.x - 7, b.y + 7, b.x + 7, b.y - 7, 1);
            } else { s.circle(b.x, b.y, 6, 12); s.setColor(Palette.GOLD); s.circle(b.x, b.y, 2, 8); }
        }
        if (world.laser.timer > 0) {
            s.setColor(Palette.AQUA); s.rectLine(world.laser.x, world.laser.y, world.laser.x, world.laser.vy, 5);
            s.setColor(Palette.TEXT); s.rectLine(world.laser.x, world.laser.y, world.laser.x, world.laser.vy, 2);
        }
        if (world.spec().loadout().droneDamage() > 0 || world.spec().loadout().weapon().behavior() == com.projectblue.game.config.ContentCatalog.Behavior.DRONE) {
            s.setColor(Palette.GOLD); s.circle(world.supportX(), world.supportY(), 10, 12);
            s.setColor(Palette.GLASS); s.circle(world.supportX(), world.supportY(), 5, 10);
        }
        if (world.shield() > 0) ring(world.player.x, world.player.y, 48, (float)world.shield() / world.spec().loadout().shieldCapacity(), Palette.GLASS);
        if (world.boss.active) {
            if (world.mission() == null) legacyBoss(world.boss);
            else compactor(world);
        }
        if (!world.invulnerable() || (int) (world.elapsed() * 14) % 2 == 0) submarine(world.player.x, world.player.y, 1, world.elapsed());
        for (int i = 0; i < world.particles.capacity(); i++) {
            Entity e = world.particles.at(i);
            if (e.active) {
                s.setColor(e.value == 0 ? Palette.GOLD : Palette.AQUA);
                s.circle(e.x, e.y, 4 * e.timer / PARTICLE_LIFE, 8);
            }
        }
        ui.endShapes();
    }
    public void submarine(float x, float y, float scale, float time) {
        float k = scale;
        s.setColor(Palette.GLASS);
        s.triangle(x - 10*k, y - 33*k, x + 10*k, y - 33*k, x, y - (52 + 5*(float)Math.sin(time*25))*k);
        s.setColor(Palette.INK);
        s.ellipse(x - 24*k, y - 42*k, 48*k, 89*k, 24);
        s.setColor(Palette.GOLD);
        s.rect(x - 32*k, y - 24*k, 15*k, 38*k);
        s.rect(x + 17*k, y - 24*k, 15*k, 38*k);
        s.setColor(Palette.SUB);
        s.ellipse(x - 20*k, y - 35*k, 40*k, 76*k, 24);
        s.setColor(Palette.TEXT);
        s.ellipse(x - 15*k, y - 6*k, 30*k, 43*k, 24);
        s.setColor(Palette.INK);
        s.ellipse(x - 14*k, y + 5*k, 28*k, 28*k, 20);
        s.setColor(Palette.GLASS);
        s.ellipse(x - 10*k, y + 9*k, 20*k, 20*k, 18);
        s.setColor(Palette.AQUA);
        s.ellipse(x - 7*k, y + 17*k, 9*k, 8*k, 12);
        s.setColor(Palette.INK);
        s.rect(x - 7*k, y - 24*k, 14*k, 4*k);
        s.rect(x - 7*k, y - 16*k, 14*k, 4*k);
        s.setColor(Palette.AQUA);
        s.circle(x - 26*k, y + 11*k, 3*k, 10);
        s.circle(x + 26*k, y + 11*k, 3*k, 10);
    }
    public void turtle(float x, float y, float k, boolean freed) {
        s.setColor(Palette.AQUA);
        s.ellipse(x + 17*k, y - 7*k, 17*k, 14*k, 16);
        s.triangle(x - 5*k, y + 9*k, x + 13*k, y + 25*k, x + 16*k, y + 4*k);
        s.triangle(x - 5*k, y - 9*k, x + 13*k, y - 25*k, x + 16*k, y - 4*k);
        s.triangle(x - 10*k, y + 6*k, x - 25*k, y + 16*k, x - 19*k, y);
        s.triangle(x - 10*k, y - 6*k, x - 25*k, y - 16*k, x - 19*k, y);
        s.setColor(Palette.INK); s.ellipse(x - 23*k, y - 16*k, 46*k, 32*k, 20);
        s.setColor(Palette.GLASS); s.ellipse(x - 20*k, y - 13*k, 40*k, 26*k, 20);
        s.setColor(Palette.AQUA);
        s.rectLine(x - 13*k, y, x + 13*k, y, 2*k);
        s.rectLine(x, y - 10*k, x, y + 10*k, 2*k);
        s.setColor(Palette.INK); s.circle(x + 28*k, y + 2*k, 2*k, 8);
        if (!freed) {
            s.setColor(Palette.GOLD);
            s.rectLine(x - 17*k, y - 17*k, x + 17*k, y + 17*k, 2*k);
            s.rectLine(x - 17*k, y + 17*k, x + 17*k, y - 17*k, 2*k);
        }
    }
    private void plastic(float x, float y) {
        s.setColor(Palette.INK); s.rect(x - 10, y - 16, 20, 32);
        s.setColor(Palette.MUTED); s.rect(x - 8, y - 14, 16, 26);
        s.setColor(Palette.GOLD); s.rect(x - 5, y + 12, 10, 5);
        s.setColor(Palette.TEXT); s.rect(x - 8, y - 5, 16, 8);
    }
    private void waste(Entity e) {
        if (e.waste == null || e.waste.kind() == com.projectblue.game.config.MissionConfig.WasteKind.BOTTLE) {
            plastic(e.x, e.y); return;
        }
        float x=e.x,y=e.y;
        switch (e.waste.kind()) {
            case BAG -> {
                s.setColor(Palette.MUTED); s.triangle(x-15,y-14,x+15,y-14,x,y+17);
                s.setColor(Palette.TEXT); s.rectLine(x-12,y-10,x+12,y+10,2);
            }
            case METAL -> {
                s.setColor(Palette.INK); s.circle(x,y,18,8); s.setColor(Palette.MUTED); s.circle(x,y,13,8);
                s.setColor(Palette.GOLD); s.rect(x-3,y-15,6,30);
            }
            case NET -> {
                s.setColor(Palette.GOLD);
                for (int i=-2;i<=2;i++) { s.rectLine(x-27,y+i*10,x+27,y+i*10,1); s.rectLine(x+i*10,y-27,x+i*10,y+27,1); }
            }
            case DIRTY_WATER -> {
                s.setColor(.20f,.23f,.18f,.72f); s.circle(x,y,e.radius,24);
                s.setColor(Palette.GOLD); s.circle(x-18,y+8,4,10); s.circle(x+21,y-14,3,10);
            }
            default -> plastic(x,y);
        }
    }
    private void drone(Entity e) {
        float x = e.x, y = e.y;
        String type = e.enemy == null ? "" : e.enemy.id();
        s.setColor(Palette.INK);
        float width = "CARRIER".equals(type) ? 94 : "REPAIR".equals(type) ? 58 : 80;
        s.rect(x - width/2, y - 10, width, 20);
        s.circle(x - 32, y, 15, 18); s.circle(x + 32, y, 15, 18);
        s.setColor("REPAIR".equals(type) ? Palette.AQUA : "NET_LAUNCHER".equals(type) ? Palette.GOLD : Palette.MUTED);
        s.circle(x - 32, y, 10, 16); s.circle(x + 32, y, 10, 16);
        s.setColor(Palette.INK);
        s.circle(x - 32, y, 6, 12); s.circle(x + 32, y, 6, 12);
        s.setColor(Palette.RED);
        s.triangle(x - 27, y + 17, x + 27, y + 17, x, y - 28);
        s.setColor(Palette.PANEL);
        s.triangle(x - 18, y + 12, x + 18, y + 12, x, y - 17);
        s.setColor("TURRET".equals(type) ? Palette.RED : Palette.GOLD); s.circle(x, y + 2, 6, 12);
        if ("CARRIER".equals(type)) { s.setColor(Palette.TEXT); s.rect(x-31,y-25,62,8); }
        if ("NET_LAUNCHER".equals(type)) { s.setColor(Palette.GOLD); s.rectLine(x-15,y-20,x+15,y+20,2); s.rectLine(x-15,y+20,x+15,y-20,2); }
        if ("TURRET".equals(type)) { s.setColor(Palette.RED); s.rectLine(x,y-3,e.aimX,e.aimY,1); }
        if (e.warned) { s.setColor(Palette.GOLD); s.rectLine(x,y-18,e.aimX,e.aimY,1); ring(e.aimX,e.aimY,13,0,Palette.RED); }
        if (e.effectTime > 0 && "REPAIR".equals(type)) { s.setColor(Palette.AQUA); s.rectLine(x,y,e.aimX,e.aimY,3); }
        ui.bar(x - 22, y + 30, 44, 3, (float)e.health / Math.max(1, e.maxHealth), Palette.RED);
    }
    private void legacyBoss(Entity b) {
        s.setColor(Palette.INK); s.ellipse(b.x - 68, b.y - 46, 136, 92, 24);
        s.setColor(Palette.RED); s.rect(b.x - 78, b.y - 10, 156, 22);
        s.setColor(Palette.PANEL); s.circle(b.x, b.y, 38, 24);
        s.setColor(Palette.GOLD); s.circle(b.x, b.y, 14, 18);
        ring(b.x, b.y, 55, 0, b.timer < .6f ? Palette.GOLD : Palette.RED);
        ui.bar(b.x - 70, b.y + 60, 140, 6, (float)b.health / b.maxHealth, Palette.RED);
    }
    private void compactor(GameWorld world) {
        Entity b=world.boss;
        float inset=world.mission().boss.pressInset();
        boolean presses=world.compactor().pressesActive();
        if (presses || world.compactor().state()==ShorelineCompactor.State.PRESS_WARNING) {
            s.setColor(world.compactor().telegraphing()?Palette.GOLD:Palette.RED);
            s.rect(0,90,inset,690); s.rect(WIDTH-inset,90,inset,690);
            s.setColor(Palette.INK); s.rect(inset-13,90,13,690); s.rect(WIDTH-inset,90,13,690);
        }
        s.setColor(Palette.INK); s.rect(b.x-105,b.y-48,210,96);
        s.setColor(Palette.MUTED); s.rect(b.x-92,b.y-35,184,70);
        s.setColor(Palette.RED); s.rect(b.x-112,b.y-11,224,22);
        s.setColor(Palette.PANEL); s.circle(b.x,b.y,42,24);
        s.setColor(world.compactor().coreVulnerable()?Palette.GOLD:Palette.EDGE); s.circle(b.x,b.y,17,18);
        if (world.compactor().telegraphing()) ring(b.x,b.y,74,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-105,b.y+61,210,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void drawPipe(Entity pipe) {
        if (!pipe.active) return;
        s.setColor(Palette.INK); s.rect(pipe.x-24,pipe.y-34,48,68);
        s.setColor(Palette.GOLD); s.rect(pipe.x-16,pipe.y-27,32,54);
        s.setColor(Palette.PANEL); s.circle(pipe.x,pipe.y-25,13,14);
        ui.bar(pipe.x-25,pipe.y+40,50,4,(float)pipe.health/Math.max(1,pipe.maxHealth),Palette.RED);
    }
    private void ring(float x, float y, float r, float progress, Color color) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + .65) * Math.PI * 2 / segments;
            s.setColor(i < progress * segments ? Palette.AQUA : color);
            s.rectLine(x + (float)Math.cos(a)*r, y + (float)Math.sin(a)*r,
                x + (float)Math.cos(b)*r, y + (float)Math.sin(b)*r, progress > 0 ? 2 : 1);
        }
    }
}
