package com.projectblue.game.config;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable authored mission. All parsing and expansion occur before simulation begins. */
public final class MissionConfig {
    public enum Movement { DESCEND, SWEEP, HOLD }
    public enum WeaponPattern { SINGLE, TRIPLE, NET, AIMED, NONE }
    public enum WasteKind { BOTTLE, BAG, METAL, NET, DIRTY_WATER }
    public enum SpawnKind { ENEMY, WASTE, TURTLE, CORAL }
    public record Stats(int health, float speed, float radius, float shotInterval, float bulletSpeed,
                        int damage, float lifetime, boolean frontArmor, int repairAmount) {}
    public record Reward(int score, int salvage) {}
    public record Enemy(String id, String displayName, Movement movement, WeaponPattern weapon, Stats stats, Reward reward) {}
    public record Waste(WasteKind kind, float radius, float cleanMultiplier, float drift, int salvage, boolean plastic) {}
    public record Wave(float time, String enemy, int count, float interval, float x, float spacing) {}
    public record Prop(float time, SpawnKind kind, WasteKind waste, float x) {}
    public record Boss(String name, float start, int coreHealth, int pipeHealth, int droneBudget,
                       float arrivalSeconds, float telegraphSeconds, float attackInterval, float pressInset, int salvage) {}
    public final String id, displayName, briefing;
    public final float durationSeconds, deadlineSeconds, recoverySeconds, cleaningSpeedMultiplier, netSeconds, netSpeedMultiplier;
    public final int hostileBulletLimit;
    public final Boss boss;
    private final Map<String,Enemy> enemies;
    private final EnumMap<WasteKind,Waste> wastes;
    private final List<Wave> waves;
    private final List<Prop> props;
    public final int wasteCount, plasticCount, turtleCount, coralCount;
    private static final String FALLBACK_JSON = """
        {"schemaVersion":1,"id":"BLUE_COAST_SAFE","displayName":"Blue Coast",
        "briefing":"Mission data was unavailable. Complete the safe recovery route and disable the Shoreline Compactor.",
        "durationSeconds":240,"deadlineSeconds":300,"recoverySeconds":5,"cleaningSpeedMultiplier":0.72,
        "netSeconds":2,"netSpeedMultiplier":0.55,"hostileBulletLimit":48,
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
    public static final MissionConfig BLUE_COAST = load();

    private MissionConfig(JsonValue root) {
        uniqueKeys(root,0);
        integer(root,"schemaVersion",1,1);
        id = string(root,"id"); displayName = string(root,"displayName"); briefing = string(root,"briefing");
        durationSeconds = number(root,"durationSeconds",240,360);
        deadlineSeconds = number(root,"deadlineSeconds",durationSeconds,600);
        recoverySeconds = number(root,"recoverySeconds",3,12);
        cleaningSpeedMultiplier = number(root,"cleaningSpeedMultiplier",.4f,.9f);
        netSeconds = number(root,"netSeconds",.5f,4);
        netSpeedMultiplier = number(root,"netSpeedMultiplier",.3f,.8f);
        hostileBulletLimit = integer(root,"hostileBulletLimit",16,120);
        JsonValue b = required(root,"boss");
        boss = new Boss(string(b,"name"),number(b,"start",180,durationSeconds-20),integer(b,"coreHealth",300,3000),
            integer(b,"pipeHealth",30,300),integer(b,"droneBudget",0,12),number(b,"arrivalSeconds",1,5),
            number(b,"telegraphSeconds",.6f,3),number(b,"attackInterval",2,6),number(b,"pressInset",60,140),integer(b,"salvage",0,500));
        Map<String,Enemy> definitions = new LinkedHashMap<>();
        for (JsonValue e : array(root,"enemies")) {
            String key = string(e,"id"); check(!definitions.containsKey(key),"Duplicate enemy id: " + key);
            JsonValue s = required(e,"stats"), r = required(e,"reward");
            JsonValue armor = required(s,"frontArmor"); check(armor.isBoolean(),"frontArmor must be boolean");
            Stats stats = new Stats(integer(s,"health",1,500),number(s,"speed",0,150),number(s,"radius",12,48),
                number(s,"shotInterval",1,12),number(s,"bulletSpeed",80,250),integer(s,"damage",0,20),
                number(s,"lifetime",6,45),armor.asBoolean(),integer(s,"repairAmount",0,30));
            definitions.put(key,new Enemy(key,string(e,"displayName"),Movement.valueOf(string(e,"movement")),
                WeaponPattern.valueOf(string(e,"weapon")),stats,new Reward(integer(r,"score",0,500),integer(r,"salvage",0,50))));
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
        int wasteTotal = 0, plastics = 0, turtles = 0, corals = 0;
        previous = -1;
        for (JsonValue p : array(root,"props")) {
            float time = number(p,"time",0,boss.start()-25), x = number(p,"x",65,475);
            check(time >= previous,"Props must be time ordered"); previous = time;
            SpawnKind kind = SpawnKind.valueOf(string(p,"kind"));
            check(kind != SpawnKind.ENEMY,"Enemies belong in waves");
            WasteKind waste = kind == SpawnKind.WASTE ? WasteKind.valueOf(string(p,"waste")) : null;
            authoredProps.add(new Prop(time,kind,waste,x));
            if (kind == SpawnKind.WASTE) { wasteTotal++; if (wastes.get(waste).plastic()) plastics++; }
            if (kind == SpawnKind.TURTLE) turtles++;
            if (kind == SpawnKind.CORAL) corals++;
        }
        check(turtles > 0 && turtles <= 6 && wasteTotal <= 100 && corals > 0 && corals <= 8,"Invalid environment counts");
        props = Collections.unmodifiableList(authoredProps);
        wasteCount = wasteTotal; plasticCount = plastics; turtleCount = turtles; coralCount = corals;
    }
    public Enemy enemy(String id) { return enemies.get(id); }
    public Collection<Enemy> enemies() { return enemies.values(); }
    public Waste waste(WasteKind kind) { return wastes.get(kind); }
    public List<Wave> waves() { return waves; }
    public List<Prop> props() { return props; }
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
    private static MissionConfig load() {
        try (InputStream input = MissionConfig.class.getResourceAsStream("/config/blue-coast.json")) {
            return readOrFallback(input,System.err);
        } catch (IOException error) { return readOrFallback(null,System.err); }
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
    private static float number(JsonValue n,String key,float min,float max) {
        JsonValue v=required(n,key); check(v.isNumber(),"Invalid number: "+key);
        float value=v.asFloat(); check(Float.isFinite(value)&&value>=min&&value<=max,"Out of range: "+key); return value;
    }
    private static int integer(JsonValue n,String key,int min,int max) {
        float value=number(n,key,min,max); check(value==(int)value,"Expected integer: "+key); return (int)value;
    }
    private static void check(boolean valid,String message) { if (!valid) throw new IllegalArgumentException(message); }
}
