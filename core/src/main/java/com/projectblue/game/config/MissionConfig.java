package com.projectblue.game.config;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.projectblue.game.config.GameConfig.*;

/** Immutable authored mission. All parsing and expansion occur before simulation begins. */
public final class MissionConfig {
    public enum MissionType {
        BLUE_COAST, CORAL_GARDENS, GHOST_NETS, SUNKEN_CITY, BLACK_TIDE, SILENT_REEF, FROZEN_DEPTHS,
        ABYSS_MINE, PLASTIC_VORTEX, NEREID_CORE
    }
    public enum Movement { DESCEND, SWEEP, HOLD, HUNTER, BURROW }
    public enum WeaponPattern { SINGLE, TRIPLE, NET, AIMED, NONE }
    public enum EnemyAbility {
        NONE, CORAL_CUTTER, SHIELD_CARRIER, NET_RECYCLER,
        CHEMICAL_BOMBER, RUIN_TURRET, SALVAGE_MECH, AMBUSH_DRONE,
        OIL_SPREADER, IGNITION_DRONE, PRESSURE_TANKER, PIPELINE_GUARD,
        ECHO_HUNTER, SOUND_MINE, SILENT_STALKER, RESONANCE_DRONE,
        ICE_DRILLER, CRYO_DRONE, THERMAL_MINE, HEAT_VENT_GUARD,
        DEEP_MINER, PRESSURE_DRONE, RAIL_TURRET, ABYSS_GUARDIAN,
        VORTEX_DRONE, TRASH_SWARM, MAGNETIC_COLLECTOR, CURRENT_DISRUPTOR
    }
    public enum WasteKind { BOTTLE, BAG, METAL, NET, DIRTY_WATER }
    public enum CreatureKind { TURTLE, SEAHORSE, MANTA, REEF_FISH, SEAL, FISH_SCHOOL, RESEARCH_DIVER, RESCUE_DIVER }
    public enum EnvironmentKind {
        CHEMICAL_BARREL, RUIN, COLLAPSIBLE, TOXIC_FIELD, OIL_FIELD, CLEANUP_CAPSULE, VALVE,
        REEF_OBSTACLE, SONAR_CELL, ICE_FALL, THERMAL_VENT, COLD_ZONE, DRILL_POINT, ICE_WALL,
        SAFE_PRESSURE_ZONE, PRESSURE_ZONE, MINE_PATH, DRILL_ARM, ENERGY_STATION, TRASH_CLUSTER
    }
    public enum SpawnKind { ENEMY, WASTE, TURTLE, CREATURE, CORAL, MECHANIC }
    public enum BossKind {
        SHORELINE_COMPACTOR, REEF_BREAKER, GHOST_NET_HARVESTER, URBAN_SALVAGER, OIL_KRAKEN,
        RESONANCE_ENGINE, BOREALIS_DRILL, THE_HARVESTER, RECYCLER_LEVIATHAN, LEVIATHAN_CORE
    }
    public record Stats(int health, float speed, float radius, float shotInterval, float bulletSpeed,
                        int damage, float lifetime, boolean frontArmor, int repairAmount) {}
    public record Reward(int score, int salvage) {}
    public record Enemy(String id, String displayName, Movement movement, WeaponPattern weapon,
                        EnemyAbility ability, Stats stats, Reward reward) {}
    public record Waste(WasteKind kind, float radius, float cleanMultiplier, float drift, int salvage, boolean plastic) {}
    public record Creature(CreatureKind kind, float radius, float rescueMultiplier, float drift, float timeoutSeconds) {}
    public record Wave(float time, String enemy, int count, float interval, float x, float spacing) {}
    public record Prop(float time, SpawnKind kind, WasteKind waste, CreatureKind creature,
                       EnvironmentKind environment, float x) {}
    public record Boss(BossKind kind, String name, float start, int coreHealth, int pipeHealth, int droneBudget,
                       float arrivalSeconds, float telegraphSeconds, float attackInterval, float pressInset, int salvage,
                       int powerCores, int restorationCleanup, int restorationRescues,
                       int restorationSonarPulses, float escapeSeconds) {}
    public record Sonar(float maxEnergy, float pulseCost, float regenPerSecond, float revealSeconds, float pickupEnergy) {}
    public record Thermal(float maxHeat, float damageThreshold, float hotGainPerSecond,
                          float coldRecoveryPerSecond, float passiveRecoveryPerSecond, float damageInterval) {}
    public record Pressure(float maxPressure, float dangerThreshold, float ambientGainPerSecond,
                           float hazardGainPerSecond, float safeRecoveryPerSecond, float damageInterval,
                           float warningSeconds) {}
    public record Vortex(float currentStrength, float directionSeconds, float debrisAngularSpeed,
                         float comboWindowSeconds, float comboDecaySeconds, int maxCombo,
                         float escapeSeconds, float escapeBoost, int bossWasteRequired) {}
    public final String id, displayName, briefing, introMessage, midpointMessage;
    public final MissionType type;
    public final float durationSeconds, deadlineSeconds, recoverySeconds, cleaningSpeedMultiplier, netSeconds, netSpeedMultiplier;
    public final float midpointStart, midpointDuration, currentStrength;
    public final int hostileBulletLimit, salvageCap;
    public final Boss boss;
    public final Sonar sonar;
    public final Thermal thermal;
    public final Pressure pressure;
    public final Vortex vortex;
    private final Map<String,Enemy> enemies;
    private final EnumMap<WasteKind,Waste> wastes;
    private final EnumMap<CreatureKind,Creature> creatures;
    private final List<Wave> waves;
    private final List<Prop> props;
    public final int wasteCount, plasticCount, turtleCount, creatureCount, coralCount, mechanicCount;
    private static final String FALLBACK_JSON = """
        {"schemaVersion":1,"id":"BLUE_COAST_SAFE","displayName":"Blue Coast",
        "briefing":"Mission data was unavailable. Complete the safe recovery route and disable the Shoreline Compactor.",
        "durationSeconds":240,"deadlineSeconds":300,"recoverySeconds":5,"cleaningSpeedMultiplier":0.72,
        "netSeconds":2,"netSpeedMultiplier":0.55,"hostileBulletLimit":48,"salvageCap":369,
        "boss":{"name":"Shoreline Compactor","start":200,"coreHealth":600,"pipeHealth":60,"droneBudget":2,
        "arrivalSeconds":2,"telegraphSeconds":1.5,"attackInterval":3.5,"pressInset":100,"salvage":30},
        "enemies":[{"id":"SCOUT","displayName":"Scout Drone","movement":"DESCEND","weapon":"SINGLE",
        "stats":{"health":24,"speed":60,"radius":22,"shotInterval":4,"bulletSpeed":120,"damage":6,
        "lifetime":28,"frontArmor":false,"repairAmount":0},"reward":{"score":100,"salvage":5}}],
        "wasteDefinitions":[
        {"kind":"BOTTLE","radius":12,"cleanMultiplier":1,"drift":43,"salvage":1,"plastic":true},
        {"kind":"BAG","radius":16,"cleanMultiplier":1.2,"drift":39,"salvage":1,"plastic":true},
        {"kind":"METAL","radius":17,"cleanMultiplier":1.8,"drift":41,"salvage":3,"plastic":false},
        {"kind":"NET","radius":32,"cleanMultiplier":2.5,"drift":31,"salvage":2,"plastic":false},
        {"kind":"DIRTY_WATER","radius":65,"cleanMultiplier":5,"drift":27,"salvage":2,"plastic":false}],
        "waves":[{"time":10,"enemy":"SCOUT","count":3,"interval":3,"x":135,"spacing":135}],
        "props":[{"time":5,"kind":"WASTE","waste":"BOTTLE","x":180},
        {"time":35,"kind":"TURTLE","x":270},{"time":55,"kind":"CORAL","x":390}]}
        """;
    public static final MissionConfig BLUE_COAST = loadBlueCoast();
    public static final MissionConfig CORAL_GARDENS = loadRequired("/config/coral-gardens.json");
    public static final MissionConfig GHOST_NETS = loadRequired("/config/ghost-nets.json");
    public static final MissionConfig SUNKEN_CITY = loadRequired("/config/sunken-city.json");
    public static final MissionConfig BLACK_TIDE = loadRequired("/config/black-tide.json");
    public static final MissionConfig SILENT_REEF = loadRequired("/config/silent-reef.json");
    public static final MissionConfig FROZEN_DEPTHS = loadRequired("/config/frozen-depths.json");
    public static final MissionConfig ABYSS_MINE = loadRequired("/config/abyss-mine.json");
    public static final MissionConfig PLASTIC_VORTEX = loadRequired("/config/plastic-vortex.json");
    public static final MissionConfig NEREID_CORE = loadRequired("/config/nereid-core.json");

    private MissionConfig(JsonValue root) {
        uniqueKeys(root,0);
        integer(root,"schemaVersion",1,1);
        id = string(root,"id"); displayName = string(root,"displayName"); briefing = string(root,"briefing");
        type = MissionType.valueOf(optionalString(root,"missionType","BLUE_COAST"));
        introMessage = optionalString(root,"introMessage",briefing);
        midpointMessage = optionalString(root,"midpointMessage","MIDPOINT ENCOUNTER");
        durationSeconds = number(root,"durationSeconds",240,360);
        deadlineSeconds = number(root,"deadlineSeconds",durationSeconds,600);
        recoverySeconds = number(root,"recoverySeconds",3,12);
        cleaningSpeedMultiplier = number(root,"cleaningSpeedMultiplier",.4f,.9f);
        netSeconds = number(root,"netSeconds",.5f,4);
        netSpeedMultiplier = number(root,"netSpeedMultiplier",.3f,.8f);
        midpointStart = optionalNumber(root,"midpointStart",durationSeconds*.45f,30,bossStartLimit(root,durationSeconds)-10);
        midpointDuration = optionalNumber(root,"midpointDuration",8,3,20);
        currentStrength = optionalNumber(root,"currentStrength",0,0,90);
        hostileBulletLimit = integer(root,"hostileBulletLimit",16,120);
        salvageCap = integer(root,"salvageCap",50,1000);
        JsonValue b = required(root,"boss");
        boss = new Boss(BossKind.valueOf(optionalString(b,"kind","SHORELINE_COMPACTOR")),string(b,"name"),
            number(b,"start",180,durationSeconds-20),integer(b,"coreHealth",300,3000),
            integer(b,"pipeHealth",30,300),integer(b,"droneBudget",0,12),number(b,"arrivalSeconds",1,5),
            number(b,"telegraphSeconds",.6f,3),number(b,"attackInterval",2,6),number(b,"pressInset",60,140),integer(b,"salvage",0,500),
            optionalInteger(b,"powerCores",0,0,4),optionalInteger(b,"restorationCleanup",0,0,6),
            optionalInteger(b,"restorationRescues",0,0,4),optionalInteger(b,"restorationSonarPulses",0,0,4),
            optionalNumber(b,"escapeSeconds",8,4,24));
        if (type==MissionType.NEREID_CORE) check(boss.kind()==BossKind.LEVIATHAN_CORE && boss.powerCores()>0
            && boss.restorationCleanup()>0 && boss.restorationRescues()>0 && boss.restorationSonarPulses()>0,
            "NEREID Core requires final boss interaction tuning");
        JsonValue sonarNode=root.get("sonar");
        if (type==MissionType.SILENT_REEF || type==MissionType.ABYSS_MINE || type==MissionType.NEREID_CORE)
            check(sonarNode!=null,"Sonar mission requires sonar tuning");
        sonar=sonarNode==null?null:new Sonar(number(sonarNode,"maxEnergy",20,200),
            number(sonarNode,"pulseCost",5,100),number(sonarNode,"regenPerSecond",.1f,20),
            number(sonarNode,"revealSeconds",.5f,8),number(sonarNode,"pickupEnergy",1,100));
        if (sonar!=null) check(sonar.pulseCost()<=sonar.maxEnergy() && sonar.pickupEnergy()<=sonar.maxEnergy(),
            "Sonar energy values exceed capacity");
        JsonValue thermalNode=root.get("thermal");
        if (type==MissionType.FROZEN_DEPTHS) check(thermalNode!=null,"Frozen Depths requires thermal tuning");
        thermal=thermalNode==null?null:new Thermal(number(thermalNode,"maxHeat",20,200),
            number(thermalNode,"damageThreshold",1,200),number(thermalNode,"hotGainPerSecond",1,100),
            number(thermalNode,"coldRecoveryPerSecond",1,100),number(thermalNode,"passiveRecoveryPerSecond",0,30),
            number(thermalNode,"damageInterval",.2f,3));
        if (thermal!=null) check(thermal.damageThreshold()<thermal.maxHeat(),"Thermal threshold must be below capacity");
        JsonValue pressureNode=root.get("pressure");
        if (type==MissionType.ABYSS_MINE) check(pressureNode!=null,"Abyss Mine requires pressure tuning");
        pressure=pressureNode==null?null:new Pressure(number(pressureNode,"maxPressure",20,200),
            number(pressureNode,"dangerThreshold",1,200),number(pressureNode,"ambientGainPerSecond",0,30),
            number(pressureNode,"hazardGainPerSecond",1,100),number(pressureNode,"safeRecoveryPerSecond",1,100),
            number(pressureNode,"damageInterval",.2f,3),number(pressureNode,"warningSeconds",.5f,4));
        if (pressure!=null) check(pressure.dangerThreshold()<pressure.maxPressure(),
            "Pressure threshold must be below capacity");
        JsonValue vortexNode=root.get("vortex");
        if (type==MissionType.PLASTIC_VORTEX) check(vortexNode!=null,"Plastic Vortex requires vortex tuning");
        vortex=vortexNode==null?null:new Vortex(number(vortexNode,"currentStrength",10,180),
            number(vortexNode,"directionSeconds",3,30),number(vortexNode,"debrisAngularSpeed",.1f,3),
            number(vortexNode,"comboWindowSeconds",1,8),number(vortexNode,"comboDecaySeconds",.5f,6),
            integer(vortexNode,"maxCombo",2,20),number(vortexNode,"escapeSeconds",4,20),
            number(vortexNode,"escapeBoost",20,220),integer(vortexNode,"bossWasteRequired",2,12));
        Map<String,Enemy> definitions = new LinkedHashMap<>();
        for (JsonValue e : array(root,"enemies")) {
            String key = string(e,"id"); check(!definitions.containsKey(key),"Duplicate enemy id: " + key);
            JsonValue s = required(e,"stats"), r = required(e,"reward");
            JsonValue armor = required(s,"frontArmor"); check(armor.isBoolean(),"frontArmor must be boolean");
            Stats stats = new Stats(integer(s,"health",1,500),number(s,"speed",0,150),number(s,"radius",12,48),
                number(s,"shotInterval",1,12),number(s,"bulletSpeed",80,250),integer(s,"damage",0,20),
                number(s,"lifetime",6,45),armor.asBoolean(),integer(s,"repairAmount",0,30));
            definitions.put(key,new Enemy(key,string(e,"displayName"),Movement.valueOf(string(e,"movement")),
                WeaponPattern.valueOf(string(e,"weapon")),EnemyAbility.valueOf(optionalString(e,"ability","NONE")),stats,
                new Reward(integer(r,"score",0,500),integer(r,"salvage",0,50))));
        }
        check(!definitions.isEmpty(),"No enemy definitions");
        enemies = Collections.unmodifiableMap(definitions);
        wastes = new EnumMap<>(WasteKind.class);
        for (JsonValue w : array(root,"wasteDefinitions")) {
            WasteKind kind = WasteKind.valueOf(string(w,"kind"));
            check(!wastes.containsKey(kind),"Duplicate waste kind: " + kind);
            JsonValue plastic = required(w,"plastic"); check(plastic.isBoolean(),"plastic must be boolean");
            wastes.put(kind,new Waste(kind,number(w,"radius",8,80),number(w,"cleanMultiplier",.5f,6),
                number(w,"drift",20,60),integer(w,"salvage",0,10),plastic.asBoolean()));
        }
        check(wastes.size() == WasteKind.values().length,"Missing waste definitions");
        creatures = new EnumMap<>(CreatureKind.class);
        JsonValue creatureDefinitions = root.get("creatureDefinitions");
        if (creatureDefinitions == null) {
            creatures.put(CreatureKind.TURTLE,new Creature(CreatureKind.TURTLE,TURTLE_RADIUS,1,TURTLE_SPEED,0));
        } else {
            check(creatureDefinitions.isArray(),"creatureDefinitions must be an array");
            for (JsonValue c : creatureDefinitions) {
                CreatureKind kind=CreatureKind.valueOf(string(c,"kind"));
                check(!creatures.containsKey(kind),"Duplicate creature kind: "+kind);
                creatures.put(kind,new Creature(kind,number(c,"radius",12,48),number(c,"rescueMultiplier",.5f,3),
                    number(c,"drift",10,60),optionalNumber(c,"timeoutSeconds",0,0,35)));
            }
        }
        List<Wave> authoredWaves = new ArrayList<>();
        float previous = -1;
        for (JsonValue w : array(root,"waves")) {
            float time = number(w,"time",0,boss.start());
            String enemy = string(w,"enemy");
            int count = integer(w,"count",1,8);
            float interval = number(w,"interval",.5f,8), x = number(w,"x",55,485), spacing = number(w,"spacing",-300,300);
            check(enemies.containsKey(enemy),"Unknown wave enemy: " + enemy);
            check(time >= previous,"Waves must be time ordered"); previous = time;
            check(time + (Math.ceil(count*2.5)-1)*interval < boss.start(),"Wave can extend into boss encounter");
            check(x + (count-1)*spacing >= 40 && x + (count-1)*spacing <= 500,"Wave formation outside playfield");
            authoredWaves.add(new Wave(time,enemy,count,interval,x,spacing));
        }
        check(!authoredWaves.isEmpty(),"Missing waves");
        waves = Collections.unmodifiableList(authoredWaves);
        List<Prop> authoredProps = new ArrayList<>();
        int wasteTotal = 0, plastics = 0, turtles = 0, creatureTotal = 0, corals = 0, mechanics = 0;
        previous = -1;
        for (JsonValue p : array(root,"props")) {
            float time = number(p,"time",0,boss.start()-25), x = number(p,"x",65,475);
            check(time >= previous,"Props must be time ordered"); previous = time;
            SpawnKind kind = SpawnKind.valueOf(string(p,"kind"));
            check(kind != SpawnKind.ENEMY,"Enemies belong in waves");
            WasteKind waste = kind == SpawnKind.WASTE ? WasteKind.valueOf(string(p,"waste")) : null;
            CreatureKind creature = kind == SpawnKind.TURTLE ? CreatureKind.TURTLE
                : kind == SpawnKind.CREATURE ? CreatureKind.valueOf(string(p,"creature")) : null;
            if (creature != null) check(creatures.containsKey(creature),"Missing creature definition: "+creature);
            EnvironmentKind environment = kind == SpawnKind.MECHANIC ? EnvironmentKind.valueOf(string(p,"environment")) : null;
            authoredProps.add(new Prop(time,kind,waste,creature,environment,x));
            if (kind == SpawnKind.WASTE) { wasteTotal++; if (wastes.get(waste).plastic()) plastics++; }
            if (kind == SpawnKind.TURTLE) turtles++;
            if (creature != null) creatureTotal++;
            if (kind == SpawnKind.CORAL) corals++;
            if (kind == SpawnKind.MECHANIC && cleanupMechanic(environment)) mechanics++;
        }
        boolean coralMission=type==MissionType.BLUE_COAST || type==MissionType.CORAL_GARDENS || type==MissionType.GHOST_NETS;
        check(creatureTotal > 0 && creatureTotal + boss.restorationRescues() <= 8 && wasteTotal <= 100 && corals <= 8
            && (!coralMission || corals > 0),"Invalid environment counts");
        props = Collections.unmodifiableList(authoredProps);
        if (type==MissionType.NEREID_CORE) check(authoredProps.stream().filter(p ->
            p.environment()==EnvironmentKind.ENERGY_STATION).count()>=boss.powerCores(),
            "NEREID Core requires authored power cores");
        wasteCount = wasteTotal; plasticCount = plastics; turtleCount = creatureTotal; creatureCount = creatureTotal;
        coralCount = corals; mechanicCount = mechanics;
    }
    public Enemy enemy(String id) { return enemies.get(id); }
    public Collection<Enemy> enemies() { return enemies.values(); }
    public Waste waste(WasteKind kind) { return wastes.get(kind); }
    public Creature creature(CreatureKind kind) { return creatures.get(kind); }
    public Collection<Creature> creatures() { return Collections.unmodifiableCollection(creatures.values()); }
    public List<Wave> waves() { return waves; }
    public List<Prop> props() { return props; }
    public int cleanupCount() { return wasteCount+mechanicCount+boss.restorationCleanup(); }
    public int rescueCount() { return turtleCount+boss.restorationRescues(); }
    public int enemyCount(float density) {
        int count = boss.droneBudget()+1;
        for (Wave wave : waves) count += Math.max(1,Math.round(wave.count()*density));
        return count;
    }
    public static MissionConfig parse(String text) { return new MissionConfig(new JsonReader().parse(text)); }
    public static MissionConfig readOrFallback(InputStream input, PrintStream log) {
        try {
            if (input == null) throw new IOException("missing resource");
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (out.size()+count > 131072) throw new IOException("file exceeds 128 KiB");
                out.write(buffer,0,count);
            }
            return parse(new String(out.toByteArray(),StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException error) {
            if (log != null) log.println("Project Blue: invalid Blue Coast mission config; using safe fallback: " + error.getMessage());
            return parse(FALLBACK_JSON);
        }
    }
    public static MissionConfig forLevel(int levelId) {
        return switch (levelId) {
            case 1 -> BLUE_COAST;
            case 2 -> CORAL_GARDENS;
            case 3 -> GHOST_NETS;
            case 4 -> SUNKEN_CITY;
            case 5 -> BLACK_TIDE;
            case 6 -> SILENT_REEF;
            case 7 -> FROZEN_DEPTHS;
            case 8 -> ABYSS_MINE;
            case 9 -> PLASTIC_VORTEX;
            case 10 -> NEREID_CORE;
            default -> null;
        };
    }
    private static MissionConfig loadBlueCoast() {
        try (InputStream input = MissionConfig.class.getResourceAsStream("/config/blue-coast.json")) {
            return readOrFallback(input,System.err);
        } catch (IOException error) { return readOrFallback(null,System.err); }
    }
    private static MissionConfig loadRequired(String path) {
        try (InputStream input=MissionConfig.class.getResourceAsStream(path)) {
            if (input==null) throw new IOException("Missing mission resource: "+path);
            return parse(new String(input.readAllBytes(),StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load mission "+path,error);
        }
    }
    private static void uniqueKeys(JsonValue node,int depth) {
        check(node != null && depth < 24,"Invalid or excessively nested mission JSON");
        Set<String> keys = new HashSet<>();
        for (JsonValue child : node) {
            if (node.isObject()) check(keys.add(child.name),"Duplicate mission field: " + child.name);
            uniqueKeys(child,depth+1);
        }
    }
    private static JsonValue required(JsonValue n,String key) { JsonValue v=n.get(key); check(v!=null,"Missing mission field: "+key); return v; }
    private static JsonValue array(JsonValue n,String key) { JsonValue v=required(n,key); check(v.isArray(),key+" must be an array"); return v; }
    private static String string(JsonValue n,String key) { JsonValue v=required(n,key); check(v.isString()&&!v.asString().trim().isEmpty(),"Invalid text: "+key); return v.asString(); }
    private static String optionalString(JsonValue n,String key,String fallback) {
        JsonValue v=n.get(key); if (v==null) return fallback;
        check(v.isString()&&!v.asString().trim().isEmpty(),"Invalid text: "+key); return v.asString();
    }
    private static float optionalNumber(JsonValue n,String key,float fallback,float min,float max) {
        JsonValue v=n.get(key); if (v==null) return fallback;
        check(v.isNumber(),"Invalid number: "+key); float value=v.asFloat();
        check(Float.isFinite(value)&&value>=min&&value<=max,"Out of range: "+key); return value;
    }
    private static float bossStartLimit(JsonValue root,float duration) {
        JsonValue boss=required(root,"boss"),start=required(boss,"start");
        return start.isNumber()?start.asFloat():duration-20;
    }
    private static float number(JsonValue n,String key,float min,float max) {
        JsonValue v=required(n,key); check(v.isNumber(),"Invalid number: "+key);
        float value=v.asFloat(); check(Float.isFinite(value)&&value>=min&&value<=max,"Out of range: "+key); return value;
    }
    private static int integer(JsonValue n,String key,int min,int max) {
        float value=number(n,key,min,max); check(value==(int)value,"Expected integer: "+key); return (int)value;
    }
    private static int optionalInteger(JsonValue n,String key,int fallback,int min,int max) {
        float value=optionalNumber(n,key,fallback,min,max);
        check(value==(int)value,"Expected integer: "+key); return (int)value;
    }
    private static boolean cleanupMechanic(EnvironmentKind kind) {
        return kind!=EnvironmentKind.ICE_FALL && kind!=EnvironmentKind.THERMAL_VENT
            && kind!=EnvironmentKind.COLD_ZONE && kind!=EnvironmentKind.SAFE_PRESSURE_ZONE
            && kind!=EnvironmentKind.PRESSURE_ZONE && kind!=EnvironmentKind.MINE_PATH
            && kind!=EnvironmentKind.DRILL_ARM;
    }
    private static void check(boolean valid,String message) { if (!valid) throw new IllegalArgumentException(message); }
}
