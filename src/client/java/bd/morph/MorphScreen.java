package bd.morph;

import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public class MorphScreen extends Screen {
    private static final List<EntityType<?>> TYPES = BuiltInRegistries.ENTITY_TYPE.stream()
        .filter(t -> t != EntityType.PLAYER && MorphMod.isMorphable(t)
            && !BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath().equals("armor_stand")
            && !BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath().equals("mannequin"))
        .sorted(Comparator.comparing(t -> t.getDescription().getString()))
        .toList();
    private static int page = 0;

    public MorphScreen() { super(Component.literal("Choose a mob")); }

    @Override
    protected void init() {
        int w = 110, h = 20, gap = 4, cols = Math.max(1, Math.min(5, (width - 8) / (w + gap)));
        int rows = Math.max(1, (height - 30 - 34) / (h + gap));
        int per = cols * rows, pages = (TYPES.size() + per - 1) / per;
        page = Math.min(page, pages - 1);
        int x0 = (width - (cols * (w + gap) - gap)) / 2, y0 = 24;
        for (int i = 0; i < per && page * per + i < TYPES.size(); i++) {
            EntityType<?> t = TYPES.get(page * per + i);
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
            addRenderableWidget(Button.builder(t.getDescription(), b -> {
                ClientPlayNetworking.send(new Payloads.Select(id));
                onClose();
            }).bounds(x0 + (i % cols) * (w + gap), y0 + (i / cols) * (h + gap), w, h).build());
        }
        int by = height - 26, cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); rebuildWidgets(); })
            .bounds(cx - 130, by, 40, h).build());
        addRenderableWidget(Button.builder(Component.literal("Back to human"), b -> {
            ClientPlayNetworking.send(new Payloads.Select(""));
            onClose();
        }).bounds(cx - 85, by, 100, h).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); })
            .bounds(cx + 20, by, 40, h).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(cx + 65, by, 60, h).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
    }
}
