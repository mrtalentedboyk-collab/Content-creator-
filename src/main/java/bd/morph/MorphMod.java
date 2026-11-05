package bd.morph;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public class MorphMod implements ModInitializer {
    public static final Map<UUID, String> MORPHS = new ConcurrentHashMap<>();
    private static final Set<UUID> GAVE_FLIGHT = ConcurrentHashMap.newKeySet();

    // ---- powers, keyed by entity id path ----
    static final Set<String> FLY = Set.of("bat", "allay", "bee", "parrot", "phantom", "vex", "ghast", "blaze", "ender_dragon", "happy_ghast");
    static final Set<String> FIRE = Set.of("blaze", "magma_cube", "strider", "ghast", "wither_skeleton", "zombified_piglin", "wither", "happy_ghast");
    static final Set<String> WATER = Set.of("squid", "glow_squid", "cod", "salmon", "pufferfish", "tropical_fish", "dolphin", "guardian", "elder_guardian", "turtle", "axolotl", "drowned", "nautilus");
    static final Set<String> NIGHT = Set.of("bat", "enderman", "cat", "ocelot", "wolf", "spider", "cave_spider", "warden");
    static final Set<String> SLOWFALL = Set.of("chicken", "parrot", "bat", "allay", "bee", "phantom", "vex");
    static final Set<String> JUMP = Set.of("rabbit", "frog", "goat", "horse", "donkey", "mule", "llama", "slime", "magma_cube");
    static final Set<String> SPEED = Set.of("wolf", "ocelot", "cat", "fox", "horse", "rabbit", "dolphin", "strider", "vindicator", "zombie_horse", "skeleton_horse", "camel", "spider");
    static final Set<String> TANK = Set.of("iron_golem", "warden", "ravager", "wither", "ender_dragon", "elder_guardian", "polar_bear", "hoglin", "zoglin");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(Payloads.Select.TYPE, Payloads.Select.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Sync.TYPE, Payloads.Sync.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Select.TYPE, (payload, ctx) -> morph(ctx.player(), payload.id()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            MORPHS.forEach((u, id) -> ServerPlayNetworking.send(handler.getPlayer(), new Payloads.Sync(u.toString(), id))));

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    @SuppressWarnings("unchecked")
    public static boolean isMorphable(EntityType<?> t) {
        return DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) t);
    }

    @SuppressWarnings("unchecked")
    static void morph(ServerPlayer p, String id) {
        EntityType<?> t = null;
        if (!id.isEmpty()) {
            t = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
            if (t == null || t == EntityType.PLAYER || !isMorphable(t)) return;
        }
        double hp = 20, atk = 1, scale = 1;
        if (t != null) {
            AttributeSupplier s = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) t);
            hp = s.getBaseValue(Attributes.MAX_HEALTH);
            if (s.hasAttribute(Attributes.ATTACK_DAMAGE)) atk = Math.max(1, s.getBaseValue(Attributes.ATTACK_DAMAGE));
            scale = Mth.clamp(t.getHeight() / 1.8, 0.3, 3.0);
            MORPHS.put(p.getUUID(), id);
        } else {
            MORPHS.remove(p.getUUID());
            dropFlight(p);
        }
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        p.getAttribute(Attributes.SCALE).setBaseValue(scale);
        p.setHealth(Math.min(p.getHealth(), (float) hp));
        if (t != null) p.setHealth((float) hp);

        Payloads.Sync msg = new Payloads.Sync(p.getUUID().toString(), id);
        for (ServerPlayer o : p.level().getServer().getPlayerList().getPlayers()) ServerPlayNetworking.send(o, msg);
    }

    static void dropFlight(ServerPlayer p) {
        if (GAVE_FLIGHT.remove(p.getUUID()) && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    private static void eff(ServerPlayer p, Holder<MobEffect> e, int amp) {
        p.addEffect(new MobEffectInstance(e, 60, amp, true, false, false));
    }

    private void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String id = MORPHS.get(p.getUUID());
            if (id == null) continue;
            String n = Identifier.parse(id).getPath();
            if (FIRE.contains(n)) eff(p, MobEffects.FIRE_RESISTANCE, 0);
            if (WATER.contains(n)) eff(p, MobEffects.WATER_BREATHING, 0);
            if (n.equals("dolphin")) eff(p, MobEffects.DOLPHINS_GRACE, 0);
            if (NIGHT.contains(n)) eff(p, MobEffects.NIGHT_VISION, 0);
            if (SLOWFALL.contains(n)) eff(p, MobEffects.SLOW_FALLING, 0);
            if (JUMP.contains(n)) eff(p, MobEffects.JUMP_BOOST, 1);
            if (SPEED.contains(n)) eff(p, MobEffects.SPEED, 1);
            if (TANK.contains(n)) eff(p, MobEffects.RESISTANCE, 1);
            if (FLY.contains(n)) {
                if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
                GAVE_FLIGHT.add(p.getUUID());
            } else dropFlight(p);
        }
    }
}
