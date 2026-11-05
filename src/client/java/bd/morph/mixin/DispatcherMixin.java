package bd.morph.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bd.morph.MorphClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderDispatcher.class)
public abstract class DispatcherMixin {
    /** Swap a morphed player's render state for the target mob's render state. */
    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void morph$swap(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        if (!(entity instanceof AbstractClientPlayer p)) return;
        Entity fake = MorphClient.fakeFor(p);
        if (fake == null) return;
        cir.setReturnValue(((EntityRenderDispatcher) (Object) this).extractEntity(fake, partialTick));
    }
}
