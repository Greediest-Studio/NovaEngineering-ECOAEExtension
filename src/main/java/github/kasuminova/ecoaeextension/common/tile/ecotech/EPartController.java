package github.kasuminova.ecoaeextension.common.tile.ecotech;

import github.kasuminova.mmce.common.world.MMWorldEventListener;
import github.kasuminova.mmce.common.world.MachineComponentManager;
import github.kasuminova.ecoaeextension.ECOAEExtension;
import github.kasuminova.ecoaeextension.common.block.prop.FacingProp;
import github.kasuminova.ecoaeextension.common.tile.TileCustomController;
import github.kasuminova.ecoaeextension.common.util.EPartMap;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class EPartController<P extends EPart<?>> extends TileCustomController {

    protected final EPartMap<P> parts = new EPartMap<>();
    protected boolean assembled = false;
    protected EnumFacing placementFacingLock = null;

    @Override
    public void doControllerTick() {
        enforcePlacementFacingLockTick();
        if (!this.doStructureCheck() || !this.isStructureFormed()) {
            disassemble();
            return;
        }
        if (!assemble()) {
            return;
        }

        if (onSyncTick()) {
            this.tickExecutor = ModularMachinery.EXECUTE_MANAGER.addTask(this::onAsyncTick, timeRecorder.usedTimeAvg());
        }
    }

    public boolean checkControllerShared() {
        BlockPos pos = getPos();
        World world = getWorld();
        if (world.getTileEntity(pos.up(2)) instanceof EPartController<?> partController) {
            if (partController.getControllerBlock() == getControllerBlock()) {
                return true;
            }
        }
        if (world.getTileEntity(pos.down(2)) instanceof EPartController<?> partController) {
            return partController.getControllerBlock() == getControllerBlock();
        }
        return false;
    }

    protected abstract boolean onSyncTick();

    protected void onAsyncTick() {
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        clearParts();
        this.foundPattern.getTileBlocksArray().forEach((pos, info) -> {
            BlockPos realPos = getPos().add(pos);
            if (!this.getWorld().isBlockLoaded(realPos)) {
                return;
            }
            TileEntity te = this.getWorld().getTileEntity(realPos);
            if (!(te instanceof AbstractEPart<?>)) {
                return;
            }
            try {
                P part = (P) te;
                part.setController(this);
                parts.addPart(part);
                onAddPart(part);
            } catch (ClassCastException e) {
                ECOAEExtension.log.error("Invalid EPart found at {} !", realPos);
                ECOAEExtension.log.error(e);
            }
        });
    }

    protected abstract void onAddPart(P part);

    @Override
    protected boolean canCheckStructure() {
        if (lastStructureCheckTick == -1 || (isStructureFormed() && !assembled)) {
            return true;
        }
        if (ticksExisted % 40 == 0) {
            return true;
        }
        if (isStructureFormed()) {
            BlockPos pos = getPos();
            Vec3i min = foundPattern.getMin();
            Vec3i max = foundPattern.getMax();
            return MMWorldEventListener.INSTANCE.isAreaChanged(getWorld(), pos.add(min), pos.add(max));
        }
        return ticksExisted % Math.min(structureCheckDelay + this.structureCheckCounter * 5, maxStructureCheckDelay) == 0;
    }

    protected boolean assemble() {
        if (assembled) {
            return true;
        }
        if (checkControllerShared()) {
            disassemble();
            return false;
        }
        assembled = true;
        parts.assemble(this);
        return true;
    }

    protected void disassemble() {
        if (!assembled) {
            return;
        }
        assembled = false;
        parts.disassemble();
    }

    protected void clearParts() {
        parts.clear();
    }

    public EPartMap<P> getParts() {
        return parts;
    }

    public boolean isAssembled() {
        return assembled;
    }

    @Override
    protected void checkRotation() {
        IBlockState state = getWorld().getBlockState(getPos());
        if (!getControllerBlock().isInstance(state.getBlock())) {
            ECOAEExtension.log.warn("Invalid EPartController block at {} !", getPos());
            controllerRotation = EnumFacing.NORTH;
            placementFacingLock = EnumFacing.NORTH;
            return;
        }

        EnumFacing stateFacing = state.getValue(BlockController.FACING);
        if (placementFacingLock == null) {
            placementFacingLock = controllerRotation != null ? controllerRotation : stateFacing;
        }
        if (controllerRotation == null) {
            controllerRotation = placementFacingLock;
        }
        enforceLockedFacing(state);
    }

    protected abstract Class<? extends Block> getControllerBlock();

    public void setPlacementFacingLock(final EnumFacing facing) {
        if (facing == null) {
            return;
        }
        this.placementFacingLock = facing;
        this.controllerRotation = facing;
        markForUpdate();
        markDirty();
        ECOAEExtension.log.info("[ECOAE][FacingLock] set lock dim={} pos={} facing={}",
                world != null ? world.provider.getDimension() : -1,
                getPos(),
                facing);
    }

    protected EnumFacing getExpectedFacing(final IBlockState state) {
        if (placementFacingLock != null) {
            return placementFacingLock;
        }
        if (controllerRotation != null) {
            return controllerRotation;
        }
        if (state.getPropertyKeys().contains(BlockController.FACING)) {
            return state.getValue(BlockController.FACING);
        }
        if (state.getPropertyKeys().contains(FacingProp.HORIZONTALS)) {
            return state.getValue(FacingProp.HORIZONTALS);
        }
        return EnumFacing.NORTH;
    }

    protected void enforceLockedFacing(final IBlockState state) {
        if (world == null || world.isRemote || !getControllerBlock().isInstance(state.getBlock())) {
            return;
        }

        EnumFacing expectedFacing = getExpectedFacing(state);
        EnumFacing stateFacing = state.getValue(BlockController.FACING);
        if (stateFacing != expectedFacing) {
            IBlockState fixedState = state.withProperty(BlockController.FACING, expectedFacing);
            world.setBlockState(getPos(), fixedState, 3);
            ECOAEExtension.log.warn("[ECOAE][FacingLock] corrected dim={} pos={} stateFacing={} expected={}",
                    world.provider.getDimension(),
                    getPos(),
                    stateFacing,
                    expectedFacing);
        }
        this.controllerRotation = expectedFacing;
        this.placementFacingLock = expectedFacing;
    }

    protected void enforcePlacementFacingLockTick() {
        if (world == null || world.isRemote || getPos() == null) {
            return;
        }
        IBlockState state = world.getBlockState(getPos());
        if (!getControllerBlock().isInstance(state.getBlock())) {
            return;
        }
        enforceLockedFacing(state);
    }

    @Override
    public void validate() {
        tileEntityInvalid = false;
        loaded = true;
    }

    @Override
    public void invalidate() {
        tileEntityInvalid = true;
        loaded = false;
        foundComponents.forEach(
                (k, v) -> v.forEach((te, component) ->
                        MachineComponentManager.INSTANCE.removeOwner(te, this)
                )
        );
        disassemble();
    }

    @Override
    public void onLoad() {
        loaded = true;
        enforcePlacementFacingLockTick();
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey("placementFacingLock")) {
            int idx = compound.getByte("placementFacingLock");
            if (idx >= 0 && idx < EnumFacing.VALUES.length) {
                placementFacingLock = EnumFacing.VALUES[idx];
                controllerRotation = placementFacingLock;
            }
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (placementFacingLock != null) {
            compound.setByte("placementFacingLock", (byte) placementFacingLock.getIndex());
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        disassemble();
    }

    @Override
    public boolean isWorking() {
        return assembled;
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        if (FMLCommonHandler.instance().getEffectiveSide().isClient() && world != null) {
            final IBlockState state = world.getBlockState(pos);
            final IBlockState actual = state.getActualState(world, pos);
            if (state != actual) {
                world.setBlockState(pos, actual);
            }
        }
    }

}
