package bd.morph;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class MorphClient implements ClientModInitializer {
    public static final Map<UUID, String> MORPHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Entity> FAKES = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.morphmod.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.consumeClick()) {
                if (mc.screen == null && mc.player != null) mc.setScreen(new MorphScreen());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Sync.TYPE, (payload, ctx) -> {
            UUID u = UUID.fromString(payload.uuid());
            if (payload.id().isEmpty()) MORPHS.remove(u); else MORPHS.put(u, payload.id());
            FAKES.remove(u);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { MORPHS.clear(); FAKES.clear(); });
    }

    /** Returns a client-side dummy mob that mirrors the player, or null if the player is not morphed. */
    public static Entity fakeFor(AbstractClientPlayer p) {
        String id = MORPHS.get(p.getUUID());
        if (id == null) return null;
        EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
        if (t == null) return null;
        Entity f = FAKES.get(p.getUUID());
        if (f == null || f.getType() != t || f.level() != p.level()) {
            f = t.create(p.level(), EntitySpawnReason.LOAD);
            if (f == null) return null;
            FAKES.put(p.getUUID(), f);
        }
        f.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        f.xo = p.xo; f.yo = p.yo; f.zo = p.zo;
        f.xOld = p.xOld; f.yOld = p.yOld; f.zOld = p.zOld;
        f.yRotO = p.yRotO; f.xRotO = p.xRotO;
        f.tickCount = p.tickCount;
        f.setOnGround(p.onGround());
        f.setInvisible(p.isInvisible());
        if (f instanceof LivingEntity lf) {
            lf.yBodyRot = p.yBodyRot; lf.yBodyRotO = p.yBodyRotO;
            lf.yHeadRot = p.yHeadRot; lf.yHeadRotO = p.yHeadRotO;
            lf.hurtTime = p.hurtTime; lf.deathTime = p.deathTime;
            lf.walkAnimation.update(p.walkAnimation.speed(), 1.0F, 1.0F);
        }
        return f;
    }
              }
          
