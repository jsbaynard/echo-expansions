package monster.giz.mixin;

import monster.giz.init.EEBlocks;
import monster.giz.world.level.block.EchoTransmitterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {

    // Lets redstone wire connect to the transmitter
    @Inject(method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private static void echo_expansions$connectToTransmitter(BlockState blockState, BlockGetter level, BlockPos pos, @Nullable Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (blockState.is(EEBlocks.ECHO_TRANSMITTER)) {
            Direction facing = blockState.getValue(EchoTransmitterBlock.FACING);
            cir.setReturnValue(facing == direction);
        }
    }
}
