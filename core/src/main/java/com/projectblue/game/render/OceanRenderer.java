package com.projectblue.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.projectblue.game.logic.*;
import com.projectblue.game.ui.*;
import com.projectblue.game.config.MissionConfig;
import static com.projectblue.game.config.GameConfig.*;

/** Original procedural art. Visual geometry is in logical pixels; all gameplay tuning is in GameConfig. */
public final class OceanRenderer {
    private final UiPainter ui;
    private final ShapeRenderer s;
    private final Color top = new Color(), bottom = new Color(), reef = new Color(), entityTint = new Color();
    public OceanRenderer(UiPainter ui) { this.ui = ui; s = ui.shapes; }

    public void backdrop(float time, float restored) {
        backdrop(time,restored,MissionConfig.MissionType.BLUE_COAST);
    }
    public void backdrop(float time,float restored,MissionConfig.MissionType type) {
        ui.beginShapes();
        if (type==MissionConfig.MissionType.CORAL_GARDENS) {
            top.set(.035f,.27f+restored*.12f,.30f+restored*.14f,1);
            bottom.set(.035f,.09f+restored*.07f,.13f+restored*.08f,1);
        } else if (type==MissionConfig.MissionType.GHOST_NETS) {
            top.set(.025f,.09f+restored*.05f,.18f+restored*.06f,1);
            bottom.set(.008f,.025f+restored*.03f,.075f+restored*.04f,1);
        } else if (type==MissionConfig.MissionType.SUNKEN_CITY) {
            top.set(.04f,.12f+restored*.06f,.17f+restored*.06f,1);
            bottom.set(.06f+.08f*restored,.045f,.035f,1);
        } else if (type==MissionConfig.MissionType.BLACK_TIDE) {
            top.set(.018f,.055f+restored*.035f,.075f+restored*.055f,1);
            bottom.set(.008f,.012f,.016f,1);
        } else if (type==MissionConfig.MissionType.SILENT_REEF) {
            top.set(.105f,.055f+restored*.06f,.24f+restored*.08f,1);
            bottom.set(.018f,.018f,.09f+restored*.05f,1);
        } else if (type==MissionConfig.MissionType.FROZEN_DEPTHS) {
            top.set(.32f+restored*.12f,.55f+restored*.12f,.68f+restored*.12f,1);
            bottom.set(.025f,.11f+restored*.05f,.18f+restored*.08f,1);
        } else if (type==MissionConfig.MissionType.ABYSS_MINE) {
            top.set(.065f,.035f+restored*.05f,.12f+restored*.1f,1);
            bottom.set(.006f,.008f,.014f+restored*.035f,1);
        } else if (type==MissionConfig.MissionType.PLASTIC_VORTEX) {
            top.set(.19f+restored*.06f,.21f+restored*.12f,.22f+restored*.16f,1);
            bottom.set(.025f,.055f+restored*.06f,.09f+restored*.12f,1);
        } else if (type==MissionConfig.MissionType.NEREID_CORE) {
            top.set(.025f,.035f+restored*.24f,.045f+restored*.31f,1);
            bottom.set(.004f,.008f+restored*.12f,.012f+restored*.19f,1);
        } else {
            top.set(.045f, .23f + restored * .1f, .29f + restored * .07f, 1);
            bottom.set(.018f, .065f + restored * .06f, .12f + restored * .07f, 1);
        }
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
        if (type==MissionConfig.MissionType.CORAL_GARDENS) reef.set(.34f,.34f,.35f,1).lerp(Palette.RED,restored*.8f);
        else if (type==MissionConfig.MissionType.GHOST_NETS) reef.set(.18f,.24f,.21f,1).lerp(Palette.TEXT,restored*.4f);
        else if (type==MissionConfig.MissionType.SUNKEN_CITY) reef.set(.36f,.17f,.09f,1).lerp(new Color(.38f,.65f,.28f,1),restored*.55f);
        else if (type==MissionConfig.MissionType.BLACK_TIDE) reef.set(.035f,.045f,.052f,1).lerp(new Color(.72f,.28f,.06f,1),restored*.45f);
        else if (type==MissionConfig.MissionType.SILENT_REEF) reef.set(.16f,.08f,.27f,1).lerp(new Color(.08f,.92f,.86f,1),restored*.65f);
        else if (type==MissionConfig.MissionType.FROZEN_DEPTHS) reef.set(.62f,.78f,.88f,1).lerp(new Color(.95f,.42f,.08f,1),restored*.42f);
        else if (type==MissionConfig.MissionType.ABYSS_MINE) reef.set(.12f,.08f,.16f,1).lerp(entityTint.set(.08f,.72f,.95f,1),restored*.7f);
        else if (type==MissionConfig.MissionType.PLASTIC_VORTEX) reef.set(.29f,.28f,.27f,1).lerp(entityTint.set(.05f,.52f,.72f,1),restored*.72f);
        else if (type==MissionConfig.MissionType.NEREID_CORE) reef.set(.045f,.05f,.055f,1).lerp(entityTint.set(.05f,.82f,.55f,1),restored*.88f);
        else reef.set(.18f, .22f, .24f, 1).lerp(Palette.AQUA, restored * .72f);
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
        MissionConfig.MissionType type=world.mission()==null?MissionConfig.MissionType.BLUE_COAST:world.mission().type;
        backdrop(world.elapsed(), world.restoration(),type);
        ui.beginShapes();
        if (type==MissionConfig.MissionType.NEREID_CORE) drawCoreFacility(world);
        if (type==MissionConfig.MissionType.PLASTIC_VORTEX) drawCurrents(world);
        if (type==MissionConfig.MissionType.SUNKEN_CITY || type==MissionConfig.MissionType.FROZEN_DEPTHS) drawRoute(world);
        for (int i = 0; i < world.corals.capacity(); i++) {
            Entity e = world.corals.at(i);
            if (!e.active) continue;
            if (type==MissionConfig.MissionType.CORAL_GARDENS)
                entityTint.set(.42f,.42f,.43f,1).lerp(Palette.RED,Rules.clamp(e.progress*(float)e.health/Math.max(1,e.maxHealth),0,1));
            else entityTint.set(reef);
            if (e.effectTime>0 || world.reefBreaker()!=null
                && world.reefBreaker().state()==ReefBreaker.State.CORAL_WARNING) entityTint.set(Palette.GOLD);
            coral(e.x, e.y, e.x < WIDTH / 2f ? 1 : -1, entityTint, i);
            ui.bar(e.x - 28, e.y + 48, 56, 3, (float)e.health / Math.max(1, e.maxHealth), entityTint);
        }
        for (int i = 0; i < world.plastics.capacity(); i++) {
            Entity e = world.plastics.at(i);
            if (!e.active) continue;
            if (e.progress > 0) {
                s.setColor(Palette.AQUA); s.rectLine(world.player.x, world.player.y + 10, e.x, e.y, 2);
                float multiplier = e.waste == null ? 1 : e.waste.cleanMultiplier();
                float progress=e.waste!=null && e.waste.kind()==MissionConfig.WasteKind.NET
                    && type==MissionConfig.MissionType.GHOST_NETS ? e.progress
                    : e.progress/(world.spec().loadout().cleanupSeconds()*multiplier);
                ring(e.x, e.y, Math.max(22, e.radius + 5), progress, Palette.AQUA);
            }
            waste(e);
        }
        for (int i=0;i<world.hazards.capacity();i++) {
            Entity e=world.hazards.at(i); if (!e.active) continue;
            if (e.environment==MissionConfig.EnvironmentKind.OIL_FIELD) {
                s.setColor(.015f,.018f,.02f,.88f); s.circle(e.x,e.y,e.radius,28);
                s.setColor(.78f,.25f,.04f,.72f); s.circle(e.x-18,e.y+12,5,10);
            } else {
                s.setColor(.25f,.56f,.08f,.58f); s.circle(e.x,e.y,e.radius,28);
                s.setColor(Palette.GOLD); s.circle(e.x+16,e.y-10,4,10);
            }
            if (e.progress>0) ring(e.x,e.y,e.radius+6,
                e.progress/EnvironmentSystems.interactionSeconds(e.environment,world.spec().difficulty()),Palette.MUTED);
        }
        for (int i=0;i<world.environments.capacity();i++) {
            Entity e=world.environments.at(i); if (e.active && world.environmentVisible(e)) environment(e,world);
        }
        for (int i = 0; i < world.turtles.capacity(); i++) {
            Entity e = world.turtles.at(i);
            if (!e.active || !world.creatureVisible(e)) continue;
            creature(e);
            float multiplier=e.creature==null?1:e.creature.rescueMultiplier();
            if (!e.friendly) ring(e.x, e.y, Math.max(35,e.radius+10), e.progress/(world.spec().loadout().rescueSeconds()*multiplier), e.progress > 0 ? Palette.AQUA : Palette.MUTED);
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
            if (e.active && world.enemyVisible(e)) drone(e);
            else if (e.active) {
                s.setColor(.02f,.025f,.025f,.7f); s.ellipse(e.x-e.radius,e.y-6,e.radius*2,12,16);
                if (e.warned) { ring(e.aimX==0?e.x:e.aimX,e.aimY==0?e.y:e.aimY,16,0,Palette.RED); }
            }
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
            else switch (world.mission().boss.kind()) {
                case SHORELINE_COMPACTOR -> compactor(world);
                case REEF_BREAKER -> reefBreaker(world);
                case GHOST_NET_HARVESTER -> ghostNetHarvester(world);
                case URBAN_SALVAGER -> urbanSalvager(world);
                case OIL_KRAKEN -> oilKraken(world);
                case RESONANCE_ENGINE -> resonanceEngine(world);
                case BOREALIS_DRILL -> borealisDrill(world);
                case THE_HARVESTER -> theHarvester(world);
                case RECYCLER_LEVIATHAN -> recyclerLeviathan(world);
                case LEVIATHAN_CORE -> leviathanCore(world);
            }
        }
        if (world.sonarPulseProgress()>0) ring(world.player.x,world.player.y,45+world.sonarPulseProgress()*420,0,Palette.AQUA);
        if (!world.invulnerable() || (int) (world.elapsed() * 14) % 2 == 0) submarine(world.player.x, world.player.y, 1, world.elapsed());
        for (int i = 0; i < world.particles.capacity(); i++) {
            Entity e = world.particles.at(i);
            if (e.active) {
                s.setColor(e.value == 0 ? Palette.GOLD : Palette.AQUA);
                s.circle(e.x, e.y, 4 * e.timer / PARTICLE_LIFE, 8);
            }
        }
        obscure(world,type);
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
    private void creature(Entity e) {
        MissionConfig.CreatureKind kind=e.creature==null?MissionConfig.CreatureKind.TURTLE:e.creature.kind();
        switch (kind) {
            case TURTLE -> turtle(e.x,e.y,1,e.friendly);
            case SEAHORSE -> seahorse(e.x,e.y,e.friendly);
            case MANTA -> manta(e.x,e.y,e.friendly);
            case REEF_FISH, FISH_SCHOOL -> fishSchool(e.x,e.y,e.friendly,kind==MissionConfig.CreatureKind.FISH_SCHOOL?5:3);
            case SEAL -> seal(e.x,e.y,e.friendly);
            case RESEARCH_DIVER, RESCUE_DIVER -> diver(e.x,e.y,e.friendly,kind==MissionConfig.CreatureKind.RESEARCH_DIVER);
        }
    }
    private void diver(float x,float y,boolean freed,boolean researcher) {
        s.setColor(freed?Palette.AQUA:Palette.TEXT); s.circle(x,y+8,8,14); s.rect(x-7,y-18,14,24);
        s.setColor(researcher?Palette.GOLD:Palette.RED); s.rect(x-13,y-14,7,20);
        s.setColor(Palette.INK); s.rectLine(x-5,y-18,x-12,y-31,3); s.rectLine(x+5,y-18,x+12,y-31,3);
        if (!freed) ring(x,y,30,0,Palette.GOLD);
    }
    private void seahorse(float x,float y,boolean freed) {
        s.setColor(freed?Palette.AQUA:Palette.MUTED);
        s.circle(x,y+9,10,16); s.rectLine(x-2,y+2,x-8,y-18,7); s.circle(x-5,y-23,7,14);
        s.triangle(x+7,y+13,x+20,y+8,x+8,y+5); s.setColor(Palette.INK); s.circle(x+3,y+12,2,8);
        if (!freed) netMark(x,y,22);
    }
    private void manta(float x,float y,boolean freed) {
        s.setColor(freed?Palette.AQUA:Palette.MUTED);
        s.triangle(x-38,y+6,x,y-18,x+38,y+6); s.triangle(x-38,y+6,x-5,y+22,x,y-18);
        s.triangle(x+38,y+6,x+5,y+22,x,y-18); s.rectLine(x,y-12,x,y-38,3);
        if (!freed) netMark(x,y,36);
    }
    private void fishSchool(float x,float y,boolean freed,int count) {
        for (int i=0;i<count;i++) {
            float fx=x+(i%3-1)*17,fy=y+(i/3)*15-(i%2)*8;
            s.setColor(freed?Palette.AQUA:Palette.TEXT); s.ellipse(fx-8,fy-4,16,8,12);
            s.triangle(fx-8,fy,fx-16,fy-6,fx-16,fy+6);
        }
        if (!freed) netMark(x,y,30);
    }
    private void seal(float x,float y,boolean freed) {
        s.setColor(freed?Palette.AQUA:Palette.MUTED); s.ellipse(x-25,y-12,50,24,20); s.circle(x+23,y+2,13,16);
        s.triangle(x-22,y,x-36,y+13,x-30,y-6); s.setColor(Palette.INK); s.circle(x+29,y+6,2,8);
        if (!freed) netMark(x,y,34);
    }
    private void netMark(float x,float y,float size) {
        s.setColor(Palette.GOLD); s.rectLine(x-size,y-size,x+size,y+size,2); s.rectLine(x-size,y+size,x+size,y-size,2);
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
        float width = "CARRIER".equals(type)||"SHIELD_CARRIER".equals(type) ? 94
            : "REPAIR".equals(type)||"FAST_HUNTER_DRONE".equals(type) ? 58 : 80;
        s.rect(x - width/2, y - 10, width, 20);
        s.circle(x - 32, y, 15, 18); s.circle(x + 32, y, 15, 18);
        s.setColor("REPAIR".equals(type)||"SHIELD_CARRIER".equals(type) ? Palette.AQUA
            : "NET_LAUNCHER".equals(type)||"NET_RECYCLER".equals(type) ? Palette.GOLD : Palette.MUTED);
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
        if ("CORAL_CUTTER".equals(type)) { s.setColor(Palette.RED); s.rectLine(x-20,y-22,x-4,y-5,4); s.rectLine(x+20,y-22,x+4,y-5,4); }
        if ("BURROW_DRONE".equals(type) && e.age<1.1f) { s.setColor(Palette.MUTED); ring(x,y,32,0,Palette.MUTED); }
        if ("TURRET".equals(type)) { s.setColor(Palette.RED); s.rectLine(x,y-3,e.aimX,e.aimY,1); }
        if (e.warned) { s.setColor(Palette.GOLD); s.rectLine(x,y-18,e.aimX,e.aimY,1); ring(e.aimX,e.aimY,13,0,Palette.RED); }
        if (e.effectTime > 0 && ("REPAIR".equals(type)||"CORAL_CUTTER".equals(type)||"NET_RECYCLER".equals(type))) {
            s.setColor(Palette.AQUA); s.rectLine(x,y,e.aimX,e.aimY,3);
        }
        if (e.shieldTime>0)
            ring(x,y,e.radius+7,0,Palette.AQUA);
        ui.bar(x - 22, y + 30, 44, 3, (float)e.health / Math.max(1, e.maxHealth), Palette.RED);
    }
    private void drawRoute(GameWorld world) {
        EnvironmentSystems.Route route=world.route();
        boolean frozen=world.mission()!=null&&world.mission().type==MissionConfig.MissionType.FROZEN_DEPTHS;
        if (frozen) s.setColor(.48f,.7f,.82f,.94f); else s.setColor(.18f,.075f,.035f,1);
        s.rect(0,44,route.left(),794); s.rect(route.right(),44,WIDTH-route.right(),794);
        if (frozen) s.setColor(.88f,.95f,1,1); else s.setColor(.31f,.16f,.08f,1);
        s.rect(route.left()-8,44,8,794); s.rect(route.right(),44,8,794);
        if (frozen) s.setColor(.95f,.43f,.08f,.75f); else s.setColor(.28f,.52f,.08f,.75f);
        for (int y=70;y<820;y+=95) { s.circle(route.left()-4,y,5,10); s.circle(route.right()+4,y+37,4,10); }
    }
    private void drawCoreFacility(GameWorld world) {
        float restored=world.restoration();
        s.setColor(.015f,.018f,.021f,.94f);
        s.rect(0,44,38,794); s.rect(WIDTH-38,44,38,794);
        s.setColor(.22f,.24f,.25f,1);
        for (int y=62;y<838;y+=78) {
            s.rect(8,y,30,5); s.rect(WIDTH-38,y+31,30,5);
        }
        Color signal=entityTint.set(Palette.RED).lerp(Palette.AQUA,restored);
        s.setColor(signal);
        for (int y=90;y<820;y+=115) { s.circle(23,y,5,10); s.circle(WIDTH-23,y+48,5,10); }
        if (restored>.35f) {
            s.setColor(.06f,.72f,.42f,restored*.65f);
            for (int y=100;y<790;y+=130) {
                s.rectLine(38,y,62,y+38,3); s.rectLine(WIDTH-38,y+24,WIDTH-64,y+62,3);
            }
        }
    }
    private void drawCurrents(GameWorld world) {
        float direction=world.currentDirection();
        float dx=(float)Math.cos(direction)*24,dy=(float)Math.sin(direction)*24;
        s.setColor(.42f,.62f,.66f,.45f);
        for (int y=100;y<820;y+=120) for (int x=70;x<WIDTH;x+=135) {
            float sway=(float)Math.sin(world.elapsed()*.7f+x*.03f+y*.02f)*18;
            s.rectLine(x+sway,y,x+sway+dx,y+dy,2);
            s.triangle(x+sway+dx,y+dy,x+sway+dx-dx*.35f-dy*.18f,y+dy-dy*.35f+dx*.18f,
                x+sway+dx-dx*.35f+dy*.18f,y+dy-dy*.35f-dx*.18f);
        }
        if (world.escapingVortex()) {
            s.setColor(Palette.AQUA); s.rectLine(70,790,WIDTH-70,790,4);
            for (int x=90;x<WIDTH-70;x+=90) s.triangle(x,815,x-10,795,x+10,795);
        }
    }
    private void environment(Entity e,GameWorld world) {
        float x=e.x,y=e.y;
        switch (e.environment) {
            case CHEMICAL_BARREL -> {
                s.setColor(Palette.INK); s.rect(x-19,y-25,38,50);
                s.setColor(.48f,.19f,.08f,1); s.rect(x-15,y-21,30,42);
                s.setColor(.35f,.72f,.08f,1); s.rect(x-15,y-4,30,8);
            }
            case RUIN -> {
                s.setColor(.22f,.11f,.065f,1); s.rect(x-40,y-34,80,68);
                s.setColor(Palette.INK); s.rect(x-24,y-12,18,46); s.rect(x+10,y-34,17,32);
            }
            case COLLAPSIBLE -> {
                if (e.warned&&!e.friendly) s.setColor(Palette.GOLD); else s.setColor(.30f,.14f,.07f,1);
                s.triangle(x-48,y-30,x+48,y-30,x+25,y+38);
                if (e.warned&&!e.friendly) ring(x,y,e.radius+9,1-e.timer/EnvironmentSystems.collapseWarning(world.spec().difficulty()),Palette.RED);
            }
            case CLEANUP_CAPSULE -> {
                s.setColor(Palette.AQUA); s.circle(x,y,19,16); s.setColor(Palette.TEXT); s.rect(x-3,y-13,6,26); s.rect(x-13,y-3,26,6);
            }
            case VALVE -> {
                s.setColor(Palette.INK); s.circle(x,y,30,16); s.setColor(.68f,.22f,.05f,1); s.circle(x,y,20,12);
                for (int i=0;i<4;i++) { double a=i*Math.PI/2; s.rectLine(x,y,x+(float)Math.cos(a)*28,y+(float)Math.sin(a)*28,4); }
                if (e.progress>0) ring(x,y,37,e.progress/EnvironmentSystems.interactionSeconds(e.environment,world.spec().difficulty()),Palette.GOLD);
            }
            case REEF_OBSTACLE -> {
                s.setColor(.16f,.08f,.26f,1); s.circle(x,y,e.radius,14);
                s.setColor(Palette.AQUA); for (int i=0;i<5;i++) s.circle(x-25+i*12,y-8+(i%2)*18,3,8);
            }
            case SONAR_CELL -> {
                s.setColor(Palette.AQUA); s.circle(x,y,19,16); s.setColor(Palette.INK); s.circle(x,y,10,14);
                ring(x,y,25,0,Palette.AQUA);
            }
            case ICE_FALL -> {
                s.setColor(e.friendly?Palette.TEXT:Palette.GOLD);
                s.triangle(x-32,y+25,x+32,y+25,x,y-35);
                if (!e.friendly) ring(x,y,e.radius+10,1-e.timer/EnvironmentSystems.iceWarning(world.spec().difficulty()),Palette.RED);
            }
            case THERMAL_VENT -> {
                s.setColor(.92f,.31f,.045f,.55f); s.circle(x,y,e.radius,28);
                s.setColor(Palette.GOLD); for (int i=-2;i<=2;i++) s.rectLine(x+i*16,y-35,x+i*10,y+36,3);
            }
            case COLD_ZONE -> {
                s.setColor(.18f,.72f,.92f,.32f); s.circle(x,y,e.radius,28); ring(x,y,e.radius,0,Palette.AQUA);
            }
            case DRILL_POINT -> {
                s.setColor(Palette.INK); s.circle(x,y,35,18); s.setColor(.86f,.34f,.06f,1); s.circle(x,y,21,14);
                for (int i=0;i<6;i++) { double a=i*Math.PI/3; s.rectLine(x,y,x+(float)Math.cos(a)*31,y+(float)Math.sin(a)*31,4); }
                if (e.progress>0) ring(x,y,42,e.progress/EnvironmentSystems.interactionSeconds(e.environment,world.spec().difficulty()),Palette.AQUA);
            }
            case ICE_WALL -> {
                s.setColor(.55f,.79f,.9f,.9f); s.rect(x-48,y-30,96,60);
                s.setColor(Palette.TEXT); s.rectLine(x-37,y-17,x+34,y+18,3); s.rectLine(x-25,y+22,x+30,y-20,2);
            }
            case SAFE_PRESSURE_ZONE -> {
                s.setColor(.035f,.48f,.75f,.28f); s.circle(x,y,e.radius,28);
                ring(x,y,e.radius,0,Palette.AQUA);
                s.setColor(Palette.AQUA); for (int i=0;i<5;i++) s.circle(x-35+i*17,y+(i%2)*16-8,3,8);
            }
            case PRESSURE_ZONE -> {
                if (e.friendly) s.setColor(.35f,.04f,.48f,.42f); else s.setColor(.46f,.22f,.05f,.25f);
                s.circle(x,y,e.radius,28);
                if (e.warned) ring(x,y,e.radius+7,e.friendly?1:1-e.timer/world.mission().pressure.warningSeconds(),
                    e.friendly?Palette.RED:Palette.GOLD);
            }
            case MINE_PATH -> {
                s.setColor(.08f,.75f,.96f,.7f);
                for (int i=-2;i<=2;i++) {
                    s.rectLine(x-42,y+i*13,x+42,y+i*13,2);
                    s.circle(x+i*18,y+i*13,4,8);
                }
            }
            case DRILL_ARM -> {
                s.setColor(e.friendly?Palette.RED:Palette.GOLD);
                s.rect(x-52,y-11,104,22); s.triangle(x-52,y-24,x-52,y+24,x-84,y);
                if (e.warned&&!e.friendly) ring(x,y,e.radius+10,1-e.timer/world.mission().pressure.warningSeconds(),Palette.RED);
            }
            case ENERGY_STATION -> {
                s.setColor(Palette.INK); s.rect(x-29,y-37,58,74);
                s.setColor(.15f,.72f,.94f,1); s.circle(x,y,21,16); ring(x,y,31,0,Palette.AQUA);
                if (e.progress>0) ring(x,y,43,e.progress/EnvironmentSystems.interactionSeconds(e.environment,world.spec().difficulty()),Palette.GOLD);
            }
            case TRASH_CLUSTER -> {
                s.setColor(.42f,.39f,.34f,1); s.circle(x,y,39,12);
                s.setColor(Palette.GOLD);
                for (int i=0;i<5;i++) { double a=e.age*.7+i*Math.PI*2/5; s.rect(x+(float)Math.cos(a)*30-5,y+(float)Math.sin(a)*30-8,10,16); }
                if (e.progress>0) ring(x,y,49,e.progress/EnvironmentSystems.interactionSeconds(e.environment,world.spec().difficulty()),Palette.AQUA);
            }
            case TOXIC_FIELD, OIL_FIELD -> { }
        }
        if (e.health>0 && e.maxHealth>1) ui.bar(x-25,y+e.radius+8,50,3,(float)e.health/e.maxHealth,Palette.RED);
    }
    private void obscure(GameWorld world,MissionConfig.MissionType type) {
        if (type!=MissionConfig.MissionType.SUNKEN_CITY && type!=MissionConfig.MissionType.BLACK_TIDE
            && type!=MissionConfig.MissionType.SILENT_REEF && type!=MissionConfig.MissionType.ABYSS_MINE) return;
        if ((type==MissionConfig.MissionType.SILENT_REEF || type==MissionConfig.MissionType.ABYSS_MINE)
            && world.sonarRevealing()) return;
        float radius=world.visibilityRadius();
        if (radius>=700) return;
        float left=Math.max(0,world.player.x-radius),right=Math.min(WIDTH,world.player.x+radius);
        float bottom=Math.max(44,world.player.y-radius),top=Math.min(838,world.player.y+radius);
        if (type==MissionConfig.MissionType.BLACK_TIDE) s.setColor(.005f,.007f,.008f,.82f);
        else if (type==MissionConfig.MissionType.SILENT_REEF) s.setColor(.025f,.008f,.07f,.74f);
        else if (type==MissionConfig.MissionType.ABYSS_MINE) s.setColor(.004f,.003f,.012f,.84f);
        else s.setColor(.025f,.055f,.065f,.7f);
        s.rect(0,44,WIDTH,Math.max(0,bottom-44)); s.rect(0,top,WIDTH,Math.max(0,838-top));
        s.rect(0,bottom,left,Math.max(0,top-bottom)); s.rect(right,bottom,WIDTH-right,Math.max(0,top-bottom));
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
    private void reefBreaker(GameWorld world) {
        Entity b=world.boss; ReefBreaker controller=world.reefBreaker(); float inset=world.mission().boss.pressInset();
        if (controller.cuttersActive() || controller.state()==ReefBreaker.State.CUTTER_WARNING) {
            s.setColor(controller.telegraphing()?Palette.GOLD:Palette.RED);
            s.triangle(0,100,inset,100,0,770); s.triangle(WIDTH,100,WIDTH-inset,100,WIDTH,770);
        }
        s.setColor(Palette.INK); s.ellipse(b.x-112,b.y-50,224,100,24);
        s.setColor(.45f,.22f,.18f,1); s.ellipse(b.x-96,b.y-38,192,76,22);
        s.setColor(Palette.RED); s.rectLine(b.x-118,b.y-5,b.x+118,b.y-5,12);
        s.setColor(world.bossCoreVulnerable()?Palette.GOLD:Palette.EDGE); s.circle(b.x,b.y,23,20);
        if (controller.telegraphing()) ring(b.x,b.y,78,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-108,b.y+64,216,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void ghostNetHarvester(GameWorld world) {
        Entity b=world.boss; GhostNetHarvester controller=world.harvester();
        if (controller.wallsActive() || controller.state()==GhostNetHarvester.State.WALL_WARNING) {
            float safe=controller.safeLaneX();
            s.setColor(controller.telegraphing()?Palette.GOLD:Palette.MUTED);
            for (int y=100;y<790;y+=24) {
                s.rectLine(0,y,Math.max(0,safe-72),y,2); s.rectLine(Math.min(WIDTH,safe+72),y,WIDTH,y,2);
            }
            s.setColor(Palette.AQUA); s.rectLine(safe-72,100,safe-72,790,2); s.rectLine(safe+72,100,safe+72,790,2);
        }
        s.setColor(Palette.INK); s.rect(b.x-112,b.y-50,224,100);
        s.setColor(.12f,.20f,.18f,1); s.rect(b.x-96,b.y-37,192,74);
        for (int i=-3;i<=3;i++) { s.setColor(Palette.MUTED); s.rectLine(b.x+i*25,b.y-36,b.x+i*25,b.y+36,1); }
        s.setColor(world.bossCoreVulnerable()?Palette.GOLD:Palette.EDGE); s.circle(b.x,b.y,23,20);
        if (controller.telegraphing()) ring(b.x,b.y,80,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-108,b.y+64,216,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void urbanSalvager(GameWorld world) {
        Entity b=world.boss; UrbanSalvager controller=world.urbanSalvager();
        s.setColor(Palette.INK); s.rect(b.x-118,b.y-49,236,98);
        s.setColor(.42f,.18f,.08f,1); s.rect(b.x-101,b.y-35,202,70);
        for (int i=-3;i<=3;i++) { s.setColor(Palette.MUTED); s.rect(b.x+i*27,b.y-42,15,84); }
        s.setColor(world.bossCoreVulnerable()?new Color(.34f,.86f,.12f,1):Palette.EDGE); s.circle(b.x,b.y,24,20);
        if (controller.telegraphing()) ring(b.x,b.y,82,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-112,b.y+65,224,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void oilKraken(GameWorld world) {
        Entity b=world.boss; OilKraken controller=world.oilKraken();
        s.setColor(.01f,.015f,.018f,1); s.circle(b.x,b.y,76,28);
        s.setColor(.65f,.20f,.035f,1);
        for (int i=0;i<6;i++) { double a=i*Math.PI/3; s.rectLine(b.x,b.y,b.x+(float)Math.cos(a)*105,b.y+(float)Math.sin(a)*56,11); }
        s.setColor(world.bossCoreVulnerable()?Palette.GOLD:Palette.EDGE); s.circle(b.x,b.y,25,20);
        if (controller.telegraphing()) ring(b.x,b.y,86,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        if (controller.state()==OilKraken.State.VALVES) {
            if (world.bossLeftPipe.active) ring(world.bossLeftPipe.x,world.bossLeftPipe.y,38,
                world.bossLeftPipe.progress/EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.VALVE,world.spec().difficulty()),Palette.GOLD);
            if (world.bossRightPipe.active) ring(world.bossRightPipe.x,world.bossRightPipe.y,38,
                world.bossRightPipe.progress/EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.VALVE,world.spec().difficulty()),Palette.GOLD);
        }
        ui.bar(b.x-112,b.y+68,224,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void resonanceEngine(GameWorld world) {
        Entity b=world.boss; ResonanceEngine controller=world.resonanceEngine();
        s.setColor(Palette.INK); s.circle(b.x,b.y,76,28);
        s.setColor(.24f,.09f,.42f,1);
        for (int i=0;i<8;i++) { double a=i*Math.PI/4; s.rectLine(b.x,b.y,b.x+(float)Math.cos(a)*105,b.y+(float)Math.sin(a)*58,9); }
        s.setColor(world.bossCoreVulnerable()?Palette.AQUA:Palette.EDGE); s.circle(b.x,b.y,26,22);
        ring(b.x,b.y,54,0,Palette.AQUA);
        if (controller.state()==ResonanceEngine.State.DECOY_FIELD && world.sonarRevealing()) {
            for (int i=0;i<3;i++) {
                float x=90+i*180+(float)Math.sin(world.elapsed()*1.2f+i)*28;
                float y=610-i*65;
                ring(x,y,31,0,Palette.AQUA); s.setColor(.08f,.9f,.85f,.35f); s.circle(x,y,12,14);
            }
        }
        if (world.sonarRevealing()) { drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe); }
        ui.bar(b.x-112,b.y+69,224,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void borealisDrill(GameWorld world) {
        Entity b=world.boss; BorealisDrill controller=world.borealisDrill();
        s.setColor(Palette.INK); s.rect(b.x-122,b.y-50,244,100);
        s.setColor(.58f,.76f,.86f,1); s.rect(b.x-103,b.y-36,206,72);
        s.setColor(.93f,.35f,.055f,1);
        s.triangle(b.x-36,b.y-46,b.x+36,b.y-46,b.x,b.y-112);
        s.setColor(world.bossCoreVulnerable()?Palette.GOLD:Palette.EDGE); s.circle(b.x,b.y,27,22);
        if (controller.telegraphing()) ring(b.x,b.y,86,0,Palette.RED);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-116,b.y+68,232,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void theHarvester(GameWorld world) {
        Entity b=world.boss; TheHarvester controller=world.theHarvester();
        s.setColor(Palette.INK); s.rect(b.x-128,b.y-51,256,102);
        s.setColor(.19f,.17f,.22f,1); s.rect(b.x-108,b.y-37,216,74);
        s.setColor(.08f,.72f,.95f,1);
        for (int i=-2;i<=2;i++) s.rectLine(b.x+i*31,b.y-44,b.x+i*31,b.y+44,5);
        s.setColor(world.bossCoreVulnerable()?Palette.AQUA:Palette.EDGE); s.circle(b.x,b.y,28,22);
        if (controller.telegraphing()) ring(b.x,b.y,88,0,Palette.GOLD);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        if (controller.state()==TheHarvester.State.POWERED_ARMOR) {
            if (world.bossLeftPipe.active) ring(world.bossLeftPipe.x,world.bossLeftPipe.y,40,
                world.bossLeftPipe.progress/EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.ENERGY_STATION,world.spec().difficulty()),Palette.AQUA);
            if (world.bossRightPipe.active) ring(world.bossRightPipe.x,world.bossRightPipe.y,40,
                world.bossRightPipe.progress/EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.ENERGY_STATION,world.spec().difficulty()),Palette.AQUA);
        }
        ui.bar(b.x-120,b.y+70,240,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void recyclerLeviathan(GameWorld world) {
        Entity b=world.boss; RecyclerLeviathan controller=world.recyclerLeviathan();
        s.setColor(Palette.INK); s.ellipse(b.x-132,b.y-54,264,108,28);
        s.setColor(.34f,.36f,.35f,1); s.ellipse(b.x-111,b.y-39,222,78,24);
        for (int i=0;i<8;i++) {
            double a=i*Math.PI/4+world.elapsed()*.16;
            s.setColor(i%2==0?Palette.GOLD:Palette.MUTED);
            s.rect(b.x+(float)Math.cos(a)*79-7,b.y+(float)Math.sin(a)*31-10,14,20);
        }
        s.setColor(world.bossCoreVulnerable()?Palette.AQUA:Palette.EDGE); s.circle(b.x,b.y,29,22);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        if (controller.state()==RecyclerLeviathan.State.WASTE_WEAPON)
            ring(b.x,b.y,64,(float)controller.wasteDelivered()/controller.wasteRequired(),Palette.GOLD);
        ui.bar(b.x-122,b.y+71,244,7,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
    }
    private void leviathanCore(GameWorld world) {
        Entity b=world.boss; LeviathanCore controller=world.leviathanCore();
        s.setColor(.008f,.01f,.012f,1); s.ellipse(b.x-142,b.y-58,284,116,30);
        s.setColor(.13f,.14f,.15f,1); s.ellipse(b.x-120,b.y-42,240,84,26);
        s.setColor(Palette.RED);
        for (int i=0;i<6;i++) {
            double angle=i*Math.PI/3+world.elapsed()*.12;
            s.rectLine(b.x+(float)Math.cos(angle)*52,b.y+(float)Math.sin(angle)*28,
                b.x+(float)Math.cos(angle)*112,b.y+(float)Math.sin(angle)*47,8);
        }
        Color coreColor=world.bossCoreVulnerable()?Palette.AQUA:controller.escaping()?Palette.RED:Palette.EDGE;
        s.setColor(coreColor); s.circle(b.x,b.y,32,24);
        s.setColor(Palette.INK); s.circle(b.x,b.y,15,18);
        if (controller.telegraphing()) {
            ring(b.x,b.y,98,1-controller.warningRemaining()/Math.max(.01f,controller.telegraphSeconds()),Palette.GOLD);
            if (controller.warningAttack()==LeviathanCore.Attack.COLLAPSE)
                ring(world.player.x,world.player.y,56,0,Palette.RED);
        }
        if (controller.state()==LeviathanCore.State.RESTORATION_SYSTEMS)
            ring(b.x,b.y,68,(controller.cleanupProgress()+controller.rescueProgress()+controller.sonarProgress())
                /(float)Math.max(1,controller.cleanupRequired()+controller.rescueRequired()+controller.sonarRequired()),Palette.AQUA);
        drawPipe(world.bossLeftPipe); drawPipe(world.bossRightPipe);
        ui.bar(b.x-128,b.y+75,256,8,(float)b.health/Math.max(1,b.maxHealth),Palette.RED);
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
